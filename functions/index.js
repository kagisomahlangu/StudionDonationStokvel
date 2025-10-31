const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.validatePayment = functions.https.onRequest((req, res) => {
  const {mPaymentId, pfPaymentId} = req.body;
  if (mPaymentId && pfPaymentId) {
    admin.firestore().collection("payments").add({
      mPaymentId: mPaymentId,
      pfPaymentId: pfPaymentId,
      status: "validated",
      timestamp: new Date(),
    });
    res.json({success: true, message: "Payment validated"});
  } else {
    res.status(400).json({success: false, message: "Missing payment IDs"});
  }
});
