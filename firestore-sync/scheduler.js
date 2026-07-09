const { fork } = require('child_process');
const path = require('path');

function runSync() {
    console.log(`\n===========================================`);
    console.log(`[${new Date().toLocaleString()}] Starting Daily Sync...`);
    console.log(`===========================================`);
    
    // Run Realtime DB Sync
    fork(path.join(__dirname, 'syncRatesRealtime.js'));
    
    // Run Firestore Sync
    fork(path.join(__dirname, 'syncRates.js'));
}

function scheduleNextRun() {
    const now = new Date();
    // Target 9:00 AM IST, which is 3:30 AM UTC
    const next9AM_IST = new Date(now);
    next9AM_IST.setUTCHours(3, 30, 0, 0);
    if (now >= next9AM_IST) {
        next9AM_IST.setUTCDate(next9AM_IST.getUTCDate() + 1);
    }
    const delay = next9AM_IST.getTime() - now.getTime();
    
    console.log(`[${new Date().toLocaleString()}] Next sync scheduled for: ${next9AM_IST.toUTCString()} (which is 9:00 AM IST / in ${Math.round(delay/1000/60)} minutes)`);
    
    setTimeout(() => {
        runSync();
        scheduleNextRun();
    }, delay);
}

console.log("=================================================");
console.log("Firebase Live Rates Daily Scheduler Initialized.");
console.log("This process will keep running and execute sync at 9:00 AM daily.");
console.log("=================================================");

scheduleNextRun();
