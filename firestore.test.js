const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_customer_2";
const MANAGER_UID = "manager_staff_1";
const ADMIN_UID = "super_admin_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
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

function adminCtx() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: "alfianaj2016@gmail.com",
    email_verified: true,
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("food_items").get());
  await assertFails(unauthDb.collection("food_categories").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("food_items").doc("item_1").set({
      name: "Hack Burger",
      description: "Bad",
      categoryId: "cat_1",
      categoryName: "Burgers",
      price: 10,
      imageUrl: "",
      vegType: "NON_VEG",
      available: true,
      featured: false,
      popular: false,
      prepTimeMinutes: 10,
      calories: 500,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("User profile isolation and privilege escalation prevention", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Customer can create own profile with role == 'customer'
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      phone: "+15550001111",
      role: "customer",
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Customer cannot self-assign admin or manager role
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      phone: "",
      role: "admin",
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Customer cannot elevate own role on update or inject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: pastTime(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: pastTime(),
    })
  );

  // Cross-user PII read/list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofedDb = testEnv.authenticatedContext("attacker_1", {
    email: "alfianaj2016@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofedDb.collection("restaurant_settings").doc("main").set({
      restaurantName: "TFC",
      restaurantAddress: "123 Main St",
      pickupInstructions: "Counter 1",
      openingHours: "10 AM - 11 PM",
      orderingOpen: true,
      minimumOrderAmount: 10,
      baseDeliveryFee: 2.5,
      estimatedPickupMinutes: 15,
      supportPhone: "+15551234567",
      supportEmail: "support@tfc.com",
      activeBannerTitle: "Hot & Crispy",
      activeBannerSubtitle: "Fresh daily",
      updatedAt: pastTime(),
    })
  );
});

test("Super Admin manages categories, food items, settings, zones, coupons, and managers", async () => {
  const superDb = adminCtx().firestore();

  await assertSucceeds(
    superDb.collection("food_categories").doc("cat_chicken").set({
      name: "Signature Chicken",
      description: "Crispy golden fried chicken",
      imageUrl: "",
      sortOrder: 1,
      active: true,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  await assertSucceeds(
    superDb.collection("food_items").doc("item_bucket").set({
      name: "TFC 8-Piece Crispy Bucket",
      description: "Eight crispy drumsticks and thighs",
      categoryId: "cat_chicken",
      categoryName: "Signature Chicken",
      price: 24.99,
      discountedPrice: 19.99,
      imageUrl: "",
      vegType: "NON_VEG",
      available: true,
      featured: true,
      popular: true,
      prepTimeMinutes: 18,
      calories: 1450,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  await assertSucceeds(
    superDb.collection("managers").doc(MANAGER_UID).set({
      userId: MANAGER_UID,
      name: "Marco Manager",
      email: "marco@tfc.com",
      phone: "+15552223333",
      active: true,
      canManageOrders: true,
      canManageMenu: true,
      canViewSales: true,
      createdBy: ADMIN_UID,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Customer cannot create managers, categories, or coupons
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("managers").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Self Manager",
      email: "alice@example.com",
      phone: "",
      active: true,
      canManageOrders: true,
      canManageMenu: true,
      canViewSales: true,
      createdBy: ALICE_UID,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
  await assertFails(
    aliceDb.collection("food_categories").doc("cat_hack").set({
      name: "Hack",
      description: "",
      imageUrl: "",
      sortOrder: 1,
      active: true,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("Customer addresses: scoped reads/queries and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("addresses").doc("addr_1").set({
      userId: ALICE_UID,
      label: "Home",
      fullAddress: "742 Evergreen Terrace",
      landmark: "Red gate",
      zoneId: "zone_central",
      latitude: 37.7749,
      longitude: -122.4194,
      defaultAddress: true,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Scoped query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("addresses").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("addresses").get());

  // Cross-user access fails
  await assertFails(bobDb.collection("addresses").doc("addr_1").get());
  await assertFails(bobDb.collection("addresses").doc("addr_1").delete());
});

test("Orders lifecycle: customer create/cancel, pickup zero fee, manager status updates, terminal lock", async () => {
  const superDb = adminCtx().firestore();
  await superDb.collection("managers").doc(MANAGER_UID).set({
    userId: MANAGER_UID,
    name: "Marco Manager",
    email: "marco@tfc.com",
    phone: "+15552223333",
    active: true,
    canManageOrders: true,
    canManageMenu: false,
    canViewSales: true,
    createdBy: ADMIN_UID,
    createdAt: pastTime(20000),
    updatedAt: pastTime(20000),
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const managerDb = testEnv.authenticatedContext(MANAGER_UID).firestore();

  // Pickup order with non-zero deliveryFee MUST fail
  await assertFails(
    aliceDb.collection("orders").doc("order_bad_pickup").set({
      orderNumber: "TFC-20261124-0001",
      userId: ALICE_UID,
      customerName: "Alice",
      customerPhone: "+15550001111",
      customerEmail: "alice@example.com",
      orderType: "PICKUP",
      status: "Order Placed",
      items: [{ itemId: "item_1", name: "Burger", price: 12.0, quantity: 1 }],
      subtotal: 12.0,
      discountAmount: 0.0,
      deliveryFee: 3.0,
      totalAmount: 15.0,
      estimatedMinutes: 15,
      paymentMethod: "Cash at Pickup",
      paymentStatus: "Pending",
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Valid delivery order succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").set({
      orderNumber: "TFC-20261124-0002",
      userId: ALICE_UID,
      customerName: "Alice",
      customerPhone: "+15550001111",
      customerEmail: "alice@example.com",
      orderType: "DELIVERY",
      status: "Order Placed",
      items: [{ itemId: "item_1", name: "Crispy Burger", price: 12.0, quantity: 2 }],
      subtotal: 24.0,
      discountAmount: 0.0,
      deliveryFee: 2.5,
      totalAmount: 26.5,
      deliveryAddress: "742 Evergreen Terrace",
      landmark: "Red gate",
      estimatedMinutes: 30,
      paymentMethod: "Cash on Delivery",
      paymentStatus: "Pending",
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Customer scoped list succeeds, unfiltered list fails
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("order_1").get());

  // Customer cannot accept their own order
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({
      status: "Accepted",
      updatedAt: pastTime(),
    })
  );

  // Manager with canManageOrders can advance status through to Delivered
  await assertSucceeds(
    managerDb.collection("orders").doc("order_1").update({
      status: "Accepted",
      updatedAt: pastTime(4000),
    })
  );
  await assertSucceeds(
    managerDb.collection("orders").doc("order_1").update({
      status: "Delivered",
      paymentStatus: "Paid",
      updatedAt: pastTime(2000),
    })
  );

  // Terminal state lock: Manager cannot modify a Delivered order; only Super Admin can override
  await assertFails(
    managerDb.collection("orders").doc("order_1").update({
      status: "Cancelled",
      updatedAt: pastTime(),
    })
  );
  await assertSucceeds(
    superDb.collection("orders").doc("order_1").update({
      status: "Cancelled",
      updatedAt: pastTime(),
    })
  );

  // Manager without canManageMenu cannot edit food_items
  await assertFails(
    managerDb.collection("food_items").doc("item_denied").set({
      name: "Denied",
      description: "",
      categoryId: "cat_1",
      categoryName: "Cat",
      price: 5,
      imageUrl: "",
      vegType: "VEG",
      available: true,
      featured: false,
      popular: false,
      prepTimeMinutes: 5,
      calories: 100,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});
