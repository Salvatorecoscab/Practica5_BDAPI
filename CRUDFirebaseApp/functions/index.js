// functions/index.js
const functions = require("@google-cloud/functions-framework");
const admin = require("firebase-admin");
const {logger} = require("firebase-functions");

try {
  if (!admin.apps.length) {
    admin.initializeApp();
  }
} catch (e) {
  logger.error("Firebase Admin SDK initialization error", e);
}

functions.http("sendHttpPushNotification", async (req, res) => {
  // Configurar CORS
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers",
      "Content-Type, Authorization");

  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }

  if (req.method !== "POST") {
    logger.warn(`Method Not Allowed: ${req.method}`);
    res.status(405).send({success: false,
      error: "Method Not Allowed"});
    return;
  }

  const authorizationHeader = req.headers.authorization;
  if (!authorizationHeader || !authorizationHeader.startsWith("Bearer ")) {
    logger.error("Unauthorized: Missing or invalid Authorization header.");
    res.status(403).send({success: false,
      error: "Unauthorized: Missing or invalid Authorization header"});
    return;
  }

  const idToken = authorizationHeader.split("Bearer ")[1];
  let decodedToken;
  try {
    decodedToken = await admin.auth().verifyIdToken(idToken);
  } catch (error) {
    logger.error("Unauthorized: Error verifying Firebase ID token:", error);
    res.status(403).send({success: false,
      error: "Unauthorized: Invalid ID token"});
    return;
  }

  const callingUserId = decodedToken.uid;
  logger.info(`Authenticated call from user: ${callingUserId}`);

  try {
    const userAdminSnapshot = await admin.database()
        .ref(`/users/${callingUserId}/isAdmin`).once("value");
    if (!userAdminSnapshot.exists() || userAdminSnapshot.val() !== true) {
      logger.error(`Forbidden: User ${callingUserId} is not an admin.`);
      res.status(403).send({success: false,
        error: "Forbidden: User is not an administrator."});
      return;
    }
    logger.info(`User ${callingUserId} is an administrator. Proceeding.`);
  } catch (error) {
    logger.error("Internal Server Error: Error checking admin status:",
        error);
    res.status(500).send({success: false,
      error: "Internal Server Error: Could not verify admin status."});
    return;
  }

  const {to: targetToken, notification} = req.body;
  const notificationTitle = notification ? notification.title : null;
  const notificationBody = notification ? notification.body : null;

  if (!targetToken || !notificationTitle || !notificationBody) {
    logger.error("Bad Request: Missing parameters.", {body: req.body});
    res.status(400).send({success: false,
      error: "Bad Request"});
    return;
  }

  // Línea corregida:
  logger.info(`Admin ${callingUserId}`);

  const message = {
    notification: {
      title: notificationTitle,
      body: notificationBody,
    },
    data: {
      click_action: "FLUTTER_NOTIFICATION_CLICK",
      senderId: callingUserId,
    },
    token: targetToken,
  };

  try {
    const fcmResponse = await admin.messaging().send(message);
    logger.info("Successfully sent message to FCM:", fcmResponse);
    res.status(200).send({success: true,
      message: "Notification sent successfully.", messageId: fcmResponse});
  } catch (error) {
    logger.error("Error sending FCM message:", error.code, error.message);
    if (error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered") {
      res.status(400).send({success: false,
        error: `Invalid FCM token: ${error.message}`,
        errorCode: error.code});
    } else {
      res.status(500).send({success: false,
        error: `Failed to send notification: ${error.message}`,
        errorCode: error.code});
    }
  }
});
