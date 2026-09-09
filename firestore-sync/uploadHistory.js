const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

let serviceAccount;
try {
  serviceAccount = require('./serviceAccountKey.json');
} catch (e) {
  console.error("CRITICAL ERROR: 'serviceAccountKey.json' not found.");
  console.error("Please place your Firebase service account private key file in this directory and name it 'serviceAccountKey.json'.");
  process.exit(1);
}

const DATABASE_URL = process.env.FIREBASE_DATABASE_URL || 'https://gold-silver-live-calc-default-rtdb.firebaseio.com/';

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: DATABASE_URL
});

const db = admin.database();

async function uploadHistory() {
  const jsonPath = path.join(__dirname, 'firebase_history_import.json');
  if (!fs.existsSync(jsonPath)) {
    console.log('Generating historical JSON first...');
    require('./generateHistoryJson');
  }

  const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

  console.log('Uploading historical rates to Firebase Realtime Database...');
  
  if (data.history) {
    await db.ref('history').set(data.history);
    console.log('✅ Successfully uploaded /history node to Firebase!');
  }
  
  if (data.rates) {
    await db.ref('rates').set(data.rates);
    console.log('✅ Successfully updated /rates node in Firebase!');
  }

  console.log('Historical data upload complete.');
  process.exit(0);
}

uploadHistory().catch(err => {
  console.error('Upload failed:', err);
  process.exit(1);
});
