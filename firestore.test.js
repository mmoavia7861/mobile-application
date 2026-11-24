const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("unauthenticated user cannot write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc("user123").set({
      userId: "user123",
      fullName: "Ali Khan",
      phoneNumber: "+923001234567",
      role: "CUSTOMER",
      city: "Islamabad"
    })
  );
});

test("authenticated user can create own profile", async () => {
  const authDb = testEnv.authenticatedContext("user123").firestore();
  await assertSucceeds(
    authDb.collection("users").doc("user123").set({
      userId: "user123",
      fullName: "Ali Khan",
      phoneNumber: "+923001234567",
      role: "CUSTOMER",
      city: "Islamabad"
    })
  );
});

test("authenticated user cannot create profile for another user ID", async () => {
  const authDb = testEnv.authenticatedContext("user123").firestore();
  await assertFails(
    authDb.collection("users").doc("user456").set({
      userId: "user456",
      fullName: "Other User",
      phoneNumber: "+923001234567",
      role: "CUSTOMER",
      city: "Lahore"
    })
  );
});

test("store owner can create store", async () => {
  const authDb = testEnv.authenticatedContext("owner123").firestore();
  await assertSucceeds(
    authDb.collection("stores").doc("store1").set({
      ownerId: "owner123",
      name: "Savour Foods",
      category: "Pakistani Food",
      city: "Islamabad",
      status: "PENDING"
    })
  );
});

test("non-owner cannot create store with someone else ownerId", async () => {
  const authDb = testEnv.authenticatedContext("attacker").firestore();
  await assertFails(
    authDb.collection("stores").doc("store1").set({
      ownerId: "owner123",
      name: "Fake Store",
      category: "Pakistani Food",
      city: "Islamabad",
      status: "PENDING"
    })
  );
});

test("customer can create order", async () => {
  const authDb = testEnv.authenticatedContext("cust1").firestore();
  await assertSucceeds(
    authDb.collection("orders").doc("order1").set({
      customerId: "cust1",
      storeId: "store1",
      city: "Islamabad",
      deliveryAddress: "House 12, Street 4, F-7/2",
      totalAmount: 1250,
      status: "NEW",
      paymentMethod: "COD"
    })
  );
});

test("authenticated merchant can register merchant profile", async () => {
  const authDb = testEnv.authenticatedContext("merchant_user_1").firestore();
  await assertSucceeds(
    authDb.collection("merchants").doc("m1").set({
      ownerUserId: "merchant_user_1",
      businessName: "Savour Foods PVT LTD",
      contactPhone: "+923001234567",
      verificationStatus: "PENDING"
    })
  );
});

test("unauthenticated user cannot create merchant", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("merchants").doc("m1").set({
      ownerUserId: "merchant_user_1",
      businessName: "Savour Foods PVT LTD",
      contactPhone: "+923001234567",
      verificationStatus: "PENDING"
    })
  );
});

test("public read of categories is allowed", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(
    unauthDb.collection("categories").get()
  );
});

test("customer can submit review for own delivered order in Reviews collection", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("orders").doc("order_delivered_1").set({
      customerId: "cust1",
      storeId: "store1",
      storeOwnerId: "owner1",
      city: "Islamabad",
      deliveryAddress: "F-7/2 Islamabad",
      totalAmount: 950,
      status: "DELIVERED",
      paymentMethod: "COD"
    });
  });

  const custDb = testEnv.authenticatedContext("cust1").firestore();
  await assertSucceeds(
    custDb.collection("Reviews").doc("order_delivered_1").set({
      orderId: "order_delivered_1",
      storeId: "store1",
      storeName: "Savour Foods",
      customerId: "cust1",
      customerName: "Ayesha Khan",
      rating: 5,
      comment: "Fresh and hot biryani delivered on time!"
    })
  );
});

test("customer cannot submit review for non-delivered order", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("orders").doc("order_active_1").set({
      customerId: "cust1",
      storeId: "store1",
      storeOwnerId: "owner1",
      city: "Islamabad",
      deliveryAddress: "F-7/2 Islamabad",
      totalAmount: 950,
      status: "PREPARING",
      paymentMethod: "COD"
    });
  });

  const custDb = testEnv.authenticatedContext("cust1").firestore();
  await assertFails(
    custDb.collection("Reviews").doc("order_active_1").set({
      orderId: "order_active_1",
      storeId: "store1",
      storeName: "Savour Foods",
      customerId: "cust1",
      customerName: "Ayesha Khan",
      rating: 5,
      comment: "Cannot review before delivery"
    })
  );
});

test("another user cannot submit review for someone else delivered order", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("orders").doc("order_delivered_2").set({
      customerId: "cust1",
      storeId: "store1",
      storeOwnerId: "owner1",
      city: "Islamabad",
      deliveryAddress: "F-7/2 Islamabad",
      totalAmount: 950,
      status: "DELIVERED",
      paymentMethod: "COD"
    });
  });

  const otherDb = testEnv.authenticatedContext("attacker").firestore();
  await assertFails(
    otherDb.collection("Reviews").doc("order_delivered_2").set({
      orderId: "order_delivered_2",
      storeId: "store1",
      storeName: "Savour Foods",
      customerId: "attacker",
      customerName: "Attacker",
      rating: 1,
      comment: "Fake negative review"
    })
  );
});


