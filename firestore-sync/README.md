# Firebase Live Rates Synchronization Utility

This utility fetches gold and silver rates from GoldAPI.io and writes them to either **Cloud Firestore** or **Firebase Realtime Database** in the exact schema required by the **Gold Silver Live Calc** Android application.

---

## Option A: Setup for Firebase Realtime Database (Based on your Console Screenshot)

### 1. Update Database Rules
In your Firebase Console, click on the **Rules** tab under **Realtime Database** and change them to:
```json
{
  "rules": {
    "rates": {
      ".read": true,
      ".write": false // Only this backend script/Admin SDK can write
    }
  }
}
```
Click **Publish**.

### 2. Configure Database URL in Android App
In your Android app, go to Settings and type your full Database URL (e.g. `https://gold-silver-live-calc-default-rtdb.firebaseio.com/` - check the URL displayed on the **Data** tab in your Firebase Console) into the input field.

### 3. Run the Realtime DB Sync Script
Run the script specifically configured for Realtime Database:
```bash
# Install dependencies if you haven't already
npm install

# Run the sync script
GOLD_API_KEY="YOUR_GOLDAPI_IO_KEY" node syncRatesRealtime.js
```

---

## Option B: Setup for Cloud Firestore

### 1. Update Firestore Rules
Under **Firestore Database** > **Rules** in the Firebase Console:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /rates/{currency} {
      allow read: if true;
      allow write: if false;
    }
  }
}
```
Click **Publish**.

### 2. Configure Project ID in Android App
In your Android app, go to Settings and type your Firestore Project ID (`gold-silver-live-calc`) into the input field.

### 3. Run the Firestore Sync Script
```bash
# Install dependencies
npm install

# Run the sync script
GOLD_API_KEY="YOUR_GOLDAPI_IO_KEY" node syncRates.js
```

---

## Daily Scheduler (Optional)

If you want the rates to update automatically every day at **exactly 9:00 AM**, you can run the scheduler script:

```bash
# Run the scheduler in the background
GOLD_API_KEY="YOUR_GOLDAPI_IO_KEY" node scheduler.js
```

This script will run continuously, automatically triggering both the Realtime Database and Firestore sync scripts at 9:00 AM every morning.

