const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

const DATABASE_URL = process.env.FIREBASE_DATABASE_URL || 'https://gold-silver-live-calc-default-rtdb.firebaseio.com/';

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: DATABASE_URL
});

async function updateRules() {
  const token = await admin.app().options.credential.getAccessToken();
  console.log('Got access token');

  const axios = require('axios');
  const rules = {
    rules: {
      ".read": true,
      ".write": "auth != null"
    }
  };

  const response = await axios.put(
    `${DATABASE_URL}/.settings/rules.json?access_token=${token.access_token}`,
    rules
  );

  console.log('Updated Firebase Realtime Database Rules response:', response.data);
}

updateRules().then(() => {
  console.log('✅ Firebase Rules updated successfully to allow public read on history and rates!');
  process.exit(0);
}).catch(err => {
  console.error('Failed to update rules:', err.response?.data || err.message);
  process.exit(1);
});
