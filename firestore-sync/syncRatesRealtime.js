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
const GOLD_API_KEY = process.env.GOLD_API_KEY || 'goldapi-136ec4ee91fff624d86a276a8699d6c1-io'; 
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

async function syncRates() {
  console.log('Starting gold and silver rate synchronization for Realtime Database (Optimized)...');

  console.log('\nFetching global prices in USD...');
  const goldDataUSD = await fetchMetalPrice('XAU', 'USD');
  const silverDataUSD = await fetchMetalPrice('XAG', 'USD');

  if (!goldDataUSD || !silverDataUSD) {
    console.error('CRITICAL ERROR: Failed to fetch base USD rates from GoldAPI. Aborting sync.');
    process.exit(1);
  }

  const exchangeRates = await fetchExchangeRates();
  const timestamp = Date.now();

  for (const currency of CURRENCIES) {
    console.log(`\nProcessing rates for ${currency}...`);
    const rate = exchangeRates[currency];
    if (!rate) {
      console.warn(`No exchange rate found for ${currency}, skipping.`);
      continue;
    }

    // Convert prices from USD per gram to target currency
    let goldPrice24k = goldDataUSD.price_gram_24k * rate;
    let goldPrice22k = goldDataUSD.price_gram_22k * rate;
    let goldPrice18k = goldDataUSD.price_gram_18k * rate;
    let goldPrice14k = goldDataUSD.price_gram_14k * rate;
    
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

    const documentData = {
      currency: currency,
      timestamp: timestamp,
      goldPrice24k: Number(goldPrice24k),
      goldPrice22k: Number(goldPrice22k),
      goldPrice18k: Number(goldPrice18k),
      goldPrice14k: Number(goldPrice14k),
      silverPrice: Number(silverPrice)
    };

    try {
      // Save directly to Realtime Database ref: /rates/CURRENCY
      await db.ref('rates/' + currency).set(documentData);
      console.log(`Successfully updated Realtime DB ref 'rates/${currency}':`, documentData);
    } catch (dbError) {
      console.error(`Failed to save ${currency} to Realtime DB:`, dbError.message);
    }
  }

  console.log('\nSynchronization complete.');
  process.exit(0);
}

// Execute sync
syncRates();
