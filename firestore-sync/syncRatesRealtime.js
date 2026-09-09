const admin = require('firebase-admin');
const axios = require('axios');

// Initialize Firebase Admin SDK
// Put your Firebase service account private key file in this directory and name it 'serviceAccountKey.json'
let serviceAccount;
try {
  serviceAccount = require('./serviceAccountKey.json');
} catch (e) {
  console.error("CRITICAL ERROR: 'serviceAccountKey.json' not found.");
  console.error("Please place your Firebase service account private key file in this directory and name it 'serviceAccountKey.json'.");
  process.exit(1);
}

// CONFIGURATIONS
// Replace this with your Realtime Database URL shown in the Firebase Console (e.g. https://your-project-default-rtdb.firebaseio.com/)
const DATABASE_URL = process.env.FIREBASE_DATABASE_URL || 'https://gold-silver-live-calc-default-rtdb.firebaseio.com/'; 
const GOLD_API_KEY = process.env.GOLD_API_KEY || 'goldapi-f0531209bb348f3fc593c8bc404a6dc6-io'; 
const CURRENCIES = ['USD', 'INR', 'EUR', 'AED', 'GBP'];

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: DATABASE_URL
});

const db = admin.database();

async function fetchMetalPrice(metal, currency) {
  try {
    const url = `https://www.goldapi.io/api/${metal}/${currency}`;
    const response = await axios.get(url, {
      headers: {
        'x-access-token': GOLD_API_KEY,
        'Content-Type': 'application/json'
      }
    });
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch ${metal} for ${currency}:`, error.response?.data || error.message);
    return null;
  }
}

const FALLBACK_EXCHANGE_RATES = {
  USD: 1.0,
  INR: 83.5,
  EUR: 0.92,
  AED: 3.67,
  GBP: 0.78
};

async function fetchExchangeRates() {
  try {
    const response = await axios.get('https://open.er-api.com/v6/latest/USD');
    if (response.data && response.data.rates) {
      return response.data.rates;
    }
  } catch (error) {
    console.error('Failed to fetch exchange rates, using fallbacks:', error.message);
  }
  return FALLBACK_EXCHANGE_RATES;
}

function getFormattedDate(date) {
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

async function syncRates() {
  console.log('Starting gold and silver rate synchronization for Realtime Database (Optimized)...');

  console.log('\nFetching global prices in USD...');
  const goldDataUSD = await fetchMetalPrice('XAU', 'USD');
  const silverDataUSD = await fetchMetalPrice('XAG', 'USD');

  if (!goldDataUSD || !silverDataUSD) {
    console.error('CRITICAL ERROR: Failed to fetch base USD rates from GoldAPI. Aborting sync.');
    process.exit(1);
  }

  // Validate API rates are strictly positive
  if (!goldDataUSD.price_gram_24k || goldDataUSD.price_gram_24k <= 0 ||
      !silverDataUSD.price_gram_24k || silverDataUSD.price_gram_24k <= 0) {
    console.error('CRITICAL ERROR: Invalid/zero rate received from GoldAPI. Aborting sync.');
    process.exit(1);
  }

  const exchangeRates = await fetchExchangeRates();
  const now = new Date();
  const timestamp = now.getTime();
  const dateStr = getFormattedDate(now);

  for (const currency of CURRENCIES) {
    console.log(`\nProcessing rates for ${currency} on date ${dateStr}...`);
    const rate = exchangeRates[currency];
    if (!rate || rate <= 0) {
      console.warn(`No valid exchange rate found for ${currency}, skipping.`);
      continue;
    }

    // Convert prices from USD per gram to target currency
    let goldPrice24k = goldDataUSD.price_gram_24k * rate;
    let goldPrice22k = (goldDataUSD.price_gram_22k || (goldDataUSD.price_gram_24k * 0.9167)) * rate;
    let goldPrice18k = (goldDataUSD.price_gram_18k || (goldDataUSD.price_gram_24k * 0.75)) * rate;
    let goldPrice14k = (goldDataUSD.price_gram_14k || (goldDataUSD.price_gram_24k * 0.5833)) * rate;
    
    let silverPriceUSD = silverDataUSD.price_gram_24k || (silverDataUSD.price / 31.1035);
    let silverPrice = silverPriceUSD * rate;

    // Apply Indian Market import duty, taxes and local premium adjustments for INR
    if (currency === 'INR') {
      const goldAdjustmentFactor = 1.163; // 15% custom duty + local premium + taxes
      const silverAdjustmentFactor = 1.267; // custom duty + local premium + taxes
      
      goldPrice24k *= goldAdjustmentFactor;
      goldPrice22k *= goldAdjustmentFactor;
      goldPrice18k *= goldAdjustmentFactor;
      goldPrice14k *= goldAdjustmentFactor;
      silverPrice *= silverAdjustmentFactor;
    }

    // Ensure valid non-zero values
    if (isNaN(goldPrice24k) || goldPrice24k <= 0 || isNaN(silverPrice) || silverPrice <= 0) {
      console.warn(`Invalid computed rate for ${currency}, skipping.`);
      continue;
    }

    const rateRecord = {
      date: dateStr,
      currency: currency,
      unit: 'gram',
      timestamp: timestamp,
      apiFetchedTimestamp: goldDataUSD.timestamp ? Number(goldDataUSD.timestamp) * 1000 : timestamp,
      firebaseSavedTimestamp: timestamp,
      goldPrice24k: Number(goldPrice24k.toFixed(2)),
      goldPrice22k: Number(goldPrice22k.toFixed(2)),
      goldPrice18k: Number(goldPrice18k.toFixed(2)),
      goldPrice14k: Number(goldPrice14k.toFixed(2)),
      silverPrice: Number(silverPrice.toFixed(2))
    };

    try {
      // Look up immediately preceding saved trading day from history to compute daily fluctuation
      let prevTradingDayGoldPrice = null;
      let prevTradingDaySilverPrice = null;

      const historySnapshot = await db.ref(`history/${currency}`).once('value');
      if (historySnapshot.exists()) {
        const historyData = historySnapshot.val();
        const pastDates = Object.keys(historyData).filter(d => d < dateStr).sort();
        if (pastDates.length > 0) {
          const lastDate = pastDates[pastDates.length - 1];
          const lastRecord = historyData[lastDate];
          if (lastRecord && lastRecord.goldPrice24k > 0 && lastRecord.silverPrice > 0) {
            prevTradingDayGoldPrice = lastRecord.goldPrice24k;
            prevTradingDaySilverPrice = lastRecord.silverPrice;
          }
        }
      }

      if (prevTradingDayGoldPrice !== null) rateRecord.prevTradingDayGoldPrice = prevTradingDayGoldPrice;
      if (prevTradingDaySilverPrice !== null) rateRecord.prevTradingDaySilverPrice = prevTradingDaySilverPrice;

      // 1. Update Latest Live Snapshot: /rates/CURRENCY
      await db.ref('rates/' + currency).set(rateRecord);
      console.log(`Successfully updated Realtime DB ref 'rates/${currency}':`, rateRecord);

      // 2. Update/Save Daily Historical Record: /history/CURRENCY/YYYY-MM-DD
      // (Overwrites existing record on same date, guaranteeing one record per day)
      await db.ref(`history/${currency}/${dateStr}`).set(rateRecord);
      console.log(`Successfully updated Realtime DB history ref 'history/${currency}/${dateStr}'`);

      // 3. Rolling 1-Year Window: Purge records older than 365 days
      const oneYearAgo = new Date(now.getTime() - 365 * 24 * 60 * 60 * 1000);
      const cutoffDateStr = getFormattedDate(oneYearAgo);

      if (historySnapshot.exists()) {
        const historyData = historySnapshot.val();
        let purgedCount = 0;
        for (const recordDate of Object.keys(historyData)) {
          if (recordDate < cutoffDateStr) {
            await db.ref(`history/${currency}/${recordDate}`).remove();
            purgedCount++;
          }
        }
        if (purgedCount > 0) {
          console.log(`[Rolling 1-Year] Purged ${purgedCount} expired records (< ${cutoffDateStr}) for ${currency}`);
        }
      }
    } catch (dbError) {
      console.error(`Failed to save ${currency} to Realtime DB:`, dbError.message);
    }
  }

  console.log('\nSynchronization complete.');
  process.exit(0);
}

// Execute sync
syncRates();
