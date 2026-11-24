const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const STATUS_MESSAGES = {
  ACCEPTED: (store) => ({
    title: `Order Accepted — ${store}`,
    body: `${store} confirmed your order and added it to the kitchen queue.`,
  }),
  PREPARING: (store) => ({
    title: `Chef Is Cooking — ${store}`,
    body: `Your meal from ${store} is now being freshly prepared.`,
  }),
  READY: (store) => ({
    title: `Ready for Pickup — ${store}`,
    body: `Your order is packed hot and waiting for rider dispatch.`,
  }),
  ON_THE_WAY: (store) => ({
    title: "Rider On the Way!",
    body: `Your food from ${store} is out for delivery to your doorstep.`,
  }),
  DELIVERED: (store) => ({
    title: "Order Delivered — Enjoy!",
    body: `Your order from ${store} has been delivered. Har Zaiqa, Har Ghar!`,
  }),
  REJECTED: (store) => ({
    title: `Order Declined — ${store}`,
    body: `Unfortunately ${store} could not fulfill this order.`,
  }),
  CANCELLED: (store) => ({
    title: "Order Cancelled",
    body: `Your order from ${store} has been cancelled.`,
  }),
};

/**
 * Triggers whenever an order document's `status` field changes in Firestore
 * and sends an FCM push notification to the customer's registered device token.
 */
exports.onOrderStatusUpdatedSendFcm = onDocumentUpdated(
  {
    document: "orders/{orderId}",
  },
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after || before.status === after.status) {
      return null;
    }

    const orderId = event.params.orderId;
    const customerId = after.customerId;
    const storeName = after.storeName || "MealVerse Restaurant";
    const newStatus = after.status;

    if (!customerId) return null;

    const db = getFirestore();
    const userDoc = await db.collection("users").doc(customerId).get();
    const userData = userDoc.data();
    if (!userData || userData.notificationsEnabled === false || !userData.fcmToken) {
      return null;
    }

    const formatter = STATUS_MESSAGES[newStatus] || ((s) => ({
      title: `Order Update — ${s}`,
      body: `Your order status is now ${newStatus.replace(/_/g, " ").toLowerCase()}.`,
    }));
    const copy = formatter(storeName);

    const response = await getMessaging().send({
      token: userData.fcmToken,
      notification: {
        title: copy.title,
        body: copy.body,
      },
      data: {
        orderId,
        storeName,
        status: newStatus,
        title: copy.title,
        body: copy.body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "mealverse_order_updates",
        },
      },
    });

    await db.collection("notifications").add({
      orderId,
      customerId,
      storeName,
      status: newStatus,
      title: copy.title,
      body: copy.body,
      fcmToken: userData.fcmToken,
      deliveryChannel: "FCM",
      dispatchStatus: "SENT",
      fcmMessageId: response,
      createdAt: FieldValue.serverTimestamp(),
    });

    return response;
  }
);
