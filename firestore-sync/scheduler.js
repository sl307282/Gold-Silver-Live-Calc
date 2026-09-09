const { fork } = require('child_process');
const path = require('path');

function runSync() {
    console.log(`\n===========================================`);
    console.log(`[${new Date().toLocaleString()}] Starting Daily Gold & Silver Rate Sync...`);
    console.log(`===========================================`);
    
    // Run Realtime DB Sync (strictly 1 run per day)
    const child = fork(path.join(__dirname, 'syncRatesRealtime.js'));
    child.on('exit', (code) => {
        console.log(`Sync process finished with code ${code}`);
    });
}

function getNextScheduledTime() {
    const now = new Date();
    // Schedule strictly once per day at 9:00 AM IST (3:30 AM UTC)
    const next9AM_IST = new Date(now);
    next9AM_IST.setUTCHours(3, 30, 0, 0);

    if (now.getTime() >= next9AM_IST.getTime()) {
        next9AM_IST.setUTCDate(next9AM_IST.getUTCDate() + 1);
    }

    return next9AM_IST;
}

function scheduleNextRun() {
    const now = new Date();
    const nextRun = getNextScheduledTime();
    const delay = nextRun.getTime() - now.getTime();
    
    console.log(`[${new Date().toLocaleString()}] Next sync scheduled for: ${nextRun.toUTCString()} (9:00 AM IST / in ${Math.round(delay/1000/60)} minutes)`);
    
    setTimeout(() => {
        runSync();
        scheduleNextRun();
    }, delay);
}

console.log("=================================================");
console.log("Firebase Gold & Silver Rate Scheduler Initialized.");
console.log("Strictly scheduled ONLY ONE TIME PER DAY at 9:00 AM IST.");
console.log("=================================================");

scheduleNextRun();
