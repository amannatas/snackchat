import admin from "firebase-admin";
import express from "express";
import bodyParser from "body-parser";
import fetch from "node-fetch";

// Firebase service account JSON
import serviceAccount from "./serviceAccountKey.json" assert { type: "json" };

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://<YOUR-FIREBASE-PROJECT>.firebaseio.com"
});

const db = admin.database();
const app = express();
app.use(bodyParser.json());

let lastChecked = Date.now();

// Polling route for Render to keep alive
app.get("/", (req, res) => res.send("Server is running"));

// Poll every 5 seconds
setInterval(async () => {
  const snapshot = await db.ref("messages").orderByChild("timestamp").startAt(lastChecked).once("value");
  const messages = snapshot.val();
  if (messages) {
    lastChecked = Date.now();
    Object.values(messages).forEach(async (msg) => {
      const token = msg.toToken; // Store receiver's FCM token in DB
      if (!token) return;

      const payload = {
        notification: {
          title: `New message from ${msg.from}`,
          body: msg.text,
        }
      };

      try {
        await admin.messaging().sendToDevice(token, payload);
        console.log("Notification sent:", msg.text);
      } catch (err) {
        console.error("Error sending notification:", err);
      }
    });
  }
}, 5000);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
