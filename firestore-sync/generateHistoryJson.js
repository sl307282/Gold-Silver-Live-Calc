const fs = require('fs');
const path = require('path');

// Currencies and their latest base prices per gram
const BASE_PRICES = {
  INR: { gold: 15375.15, silver: 233.50, unit: "gram" },
  USD: { gold: 133.74, silver: 3.00, unit: "gram" },
  EUR: { gold: 123.00, silver: 2.76, unit: "gram" },
  AED: { gold: 491.00, silver: 11.00, unit: "gram" },
  GBP: { gold: 105.00, silver: 2.35, unit: "gram" }
};

// Generate 365 days of historical data walking backward from current date
function generateHistoricalData() {
  const result = { history: {}, rates: {} };
  const numDays = 365;
  const now = new Date();

  for (const [currency, base] of Object.entries(BASE_PRICES)) {
    result.history[currency] = {};

    let currentGold = base.gold;
    let currentSilver = base.silver;

    // Use a deterministic seed so trends are smooth and realistic
    const dailyRecords = [];

    for (let i = 0; i < numDays; i++) {
      const d = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
      const dayOfWeek = d.getDay(); // 0 = Sun, 6 = Sat

      // Skip weekends (market closed)
      if (dayOfWeek === 0 || dayOfWeek === 6) continue;

      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const dateStr = `${year}-${month}-${day}`;

      d.setHours(18, 0, 0, 0); // 6:00 PM closing
      const timestamp = d.getTime();

      dailyRecords.push({
        date: dateStr,
        timestamp: timestamp,
        gold: Number(currentGold.toFixed(2)),
        silver: Number(currentSilver.toFixed(2))
      });

      // Realistic daily percentage change walk (-0.4% to +0.4%)
      const goldChange = (Math.sin(i / 14) * 0.002) + ((Math.cos(i / 5) * 0.003));
      const silverChange = (Math.sin(i / 10) * 0.003) + ((Math.cos(i / 7) * 0.004));

      currentGold = currentGold / (1 + goldChange);
      currentSilver = currentSilver / (1 + silverChange);
    }

    // Sort chronologically and add to result
    dailyRecords.reverse().forEach(record => {
      const gold24k = record.gold;
      const silver = record.silver;

      result.history[currency][record.date] = {
        date: record.date,
        currency: currency,
        unit: base.unit,
        timestamp: record.timestamp,
        apiFetchedTimestamp: record.timestamp,
        firebaseSavedTimestamp: record.timestamp,
        goldPrice24k: gold24k,
        goldPrice22k: Number((gold24k * 0.9167).toFixed(2)),
        goldPrice18k: Number((gold24k * 0.75).toFixed(2)),
        goldPrice14k: Number((gold24k * 0.5833).toFixed(2)),
        silverPrice: silver
      };
    });

    // Also populate latest snapshot
    const latest = dailyRecords[dailyRecords.length - 1];
    if (latest) {
      result.rates[currency] = result.history[currency][latest.date];
    }
  }

  return result;
}

const data = generateHistoricalData();
const outputPath = path.join(__dirname, 'firebase_history_import.json');
fs.writeFileSync(outputPath, JSON.stringify(data, null, 2), 'utf8');

console.log('✅ Generated 1-year historical dataset at:');
console.log(outputPath);
console.log('\nYou can:');
console.log('1. Import this JSON directly into Firebase Console -> Realtime Database -> (⋮) -> Import JSON');
console.log('2. Or run: node uploadHistory.js (if serviceAccountKey.json is present in this directory)');
