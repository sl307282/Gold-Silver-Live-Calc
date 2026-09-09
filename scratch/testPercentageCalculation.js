// Test Percentage Fluctuation Calculation Logic

function calculatePercentageChange(currentRate, previousRate) {
  if (previousRate === null || previousRate === undefined || previousRate <= 0) {
    return null;
  }
  return ((currentRate - previousRate) / previousRate) * 100.0;
}

function formatPercentageDisplay(changePercent) {
  if (changePercent === null || isNaN(changePercent) || !isFinite(changePercent)) {
    return "—";
  }

  const formattedPercent = Math.abs(changePercent).toFixed(2);
  if (formattedPercent === "0.00") {
    return "0.00%";
  } else if (changePercent > 0) {
    return `▲ +${formattedPercent}%`;
  } else {
    return `▼ ${formattedPercent}%`;
  }
}

// User Test Case Data
const goldRates = [
  { date: "2026-09-06", rate: 15655.87 },
  { date: "2026-09-08", rate: 15560.82 },
  { date: "2026-09-09", rate: 15630.72 }
];

const silverRates = [
  { date: "2026-09-06", rate: 254.88 },
  { date: "2026-09-08", rate: 255.45 },
  { date: "2026-09-09", rate: 258.57 }
];

console.log("=== GOLD PERCENTAGE FLUCTUATION TESTS ===");
for (let i = 0; i < goldRates.length; i++) {
  const current = goldRates[i];
  const previous = i > 0 ? goldRates[i - 1] : null;
  const pct = calculatePercentageChange(current.rate, previous ? previous.rate : null);
  const display = formatPercentageDisplay(pct);
  console.log(`${current.date} = ₹${current.rate.toFixed(2)} | Prev: ${previous ? "₹" + previous.rate.toFixed(2) : "None"} → Display: ${display} (raw: ${pct !== null ? pct.toFixed(6) + "%" : "null"})`);
}

console.log("\n=== SILVER PERCENTAGE FLUCTUATION TESTS ===");
for (let i = 0; i < silverRates.length; i++) {
  const current = silverRates[i];
  const previous = i > 0 ? silverRates[i - 1] : null;
  const pct = calculatePercentageChange(current.rate, previous ? previous.rate : null);
  const display = formatPercentageDisplay(pct);
  console.log(`${current.date} = ₹${current.rate.toFixed(2)} | Prev: ${previous ? "₹" + previous.rate.toFixed(2) : "None"} → Display: ${display} (raw: ${pct !== null ? pct.toFixed(6) + "%" : "null"})`);
}
