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
const MANAGER_EMAIL = "rahul.manager@gmail.com";
const MANAGER_EMAIL_DOC = "rahul_manager_gmail_com";
const ADMIN_UID = "super_admin_1";
const OWNER_EMAIL = "junaid545ansrai@gmail.com";

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

function ownerCtx() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  });
}

function managerCtx(verified = true) {
  return testEnv.authenticatedContext(MANAGER_UID, {
    email: MANAGER_EMAIL,
    email_verified: verified,
  });
}

function customerCtx(uid, email) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("food_items").get());
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("food_items").doc("item_1").set({
      id: "item_1",
      name: "Hack Burger",
      description: "Bad",
      price: 199,
      imageUrl: "",
      category: "Burgers",
      categoryId: "cat_1",
      foodType: "NON_VEG",
      isAvailable: true,
      isPopular: false,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("Customer Google Login: profile completion, +91 validation, isolation, and anti-escalation", async () => {
  const aliceDb = customerCtx(ALICE_UID, "alice@gmail.com").firestore();
  const bobDb = customerCtx(BOB_UID, "bob@gmail.com").firestore();

  // Customer creates profile on Google Sign-In with empty phoneNumber before Profile Completion
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice Sharma",
      email: "alice@gmail.com",
      phoneNumber: "",
      profileImage: "https://lh3.googleusercontent.com/a/alice",
      role: "customer",
      isActive: true,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Customer completes Profile Completion manually with 10-digit Indian +91 number (no OTP)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Alice Sharma",
      phoneNumber: "+919876543210",
      updatedAt: pastTime(4000),
    })
  );

  // Invalid phoneNumber format rejected (+1 or invalid digit)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phoneNumber: "+14155551234",
      updatedAt: pastTime(2000),
    })
  );

  // Customer cannot change read-only Google email
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      email: OWNER_EMAIL,
      updatedAt: pastTime(2000),
    })
  );

  // Customer cannot self-assign owner or manager role on create or update
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Bob Hacker",
      email: "bob@gmail.com",
      phoneNumber: "+919111222333",
      profileImage: "",
      role: "owner",
      isActive: true,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "manager",
      updatedAt: pastTime(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isActive: false,
      updatedAt: pastTime(),
    })
  );

  // Cross-user PII read/list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Email spoofing test: unverified owner or manager email is rejected", async () => {
  const spoofedOwnerDb = testEnv.authenticatedContext("attacker_owner", {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofedOwnerDb.collection("restaurant_settings").doc("main").set({
      restaurantName: "TFC",
      restaurantAddress: "MG Road, Bengaluru",
      pickupInstructions: "Counter 1",
      openingHours: "10 AM - 11 PM",
      orderingOpen: true,
      minimumOrderAmount: 199,
      baseDeliveryFee: 40,
      estimatedPickupMinutes: 15,
      supportPhone: "+919876543210",
      supportEmail: "support@tfc.com",
      activeBannerTitle: "Hot & Crispy",
      activeBannerSubtitle: "Fresh daily",
      updatedAt: pastTime(),
    })
  );

  // Pre-register manager email by legitimate owner
  const superDb = ownerCtx().firestore();
  await assertSucceeds(
    superDb.collection("managers").doc(MANAGER_EMAIL_DOC).set({
      userId: MANAGER_EMAIL_DOC,
      name: "Rahul Manager",
      email: MANAGER_EMAIL,
      phone: "+919876543210",
      active: true,
      canViewOrders: true,
      canManageOrders: true,
      canViewMenu: true,
      canManageMenu: true,
      canViewSales: true,
      createdBy: ADMIN_UID,
      createdAt: pastTime(15000),
      updatedAt: pastTime(15000),
    })
  );

  // Unverified email claim matching manager email is rejected
  const unverifiedMgrDb = managerCtx(false).firestore();
  await assertFails(unverifiedMgrDb.collection("managers").doc(MANAGER_EMAIL_DOC).get());
});

test("Owner Google Login: manages categories, dynamic food items, settings, and email-indexed managers", async () => {
  const superDb = ownerCtx().firestore();

  // Owner creates own users/{uid} profile with role == 'owner'
  await assertSucceeds(
    superDb.collection("users").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      name: "TFC Owner",
      email: OWNER_EMAIL,
      phoneNumber: "+919999888877",
      profileImage: "",
      role: "owner",
      isActive: true,
      createdAt: pastTime(15000),
      updatedAt: pastTime(15000),
    })
  );

  await assertSucceeds(
    superDb.collection("restaurant_settings").doc("main").set({
      restaurantName: "TFC Restaurant",
      restaurantAddress: "14th Main, Indiranagar, Bengaluru",
      pickupInstructions: "Show order ID at Counter 1",
      openingHours: "10:00 AM - 11:30 PM",
      orderingOpen: true,
      minimumOrderAmount: 149,
      baseDeliveryFee: 40,
      estimatedPickupMinutes: 15,
      supportPhone: "+919876543210",
      supportEmail: "support@tfc.in",
      activeBannerTitle: "TFC Hot & Crispy",
      activeBannerSubtitle: "Fresh Indian spices & crunch",
      updatedAt: pastTime(15000),
    })
  );

  await assertSucceeds(
    superDb.collection("categories").doc("cat_chicken").set({
      id: "cat_chicken",
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
      id: "item_bucket",
      name: "TFC 8-Piece Crispy Bucket",
      description: "Eight crispy drumsticks and thighs",
      price: 599,
      discountPrice: 499,
      imageUrl: "",
      category: "Signature Chicken",
      categoryId: "cat_chicken",
      foodType: "NON_VEG",
      isAvailable: true,
      isPopular: true,
      featured: true,
      prepTimeMinutes: 18,
      calories: 1450,
      variants: [{ name: "Regular (4 Pc)", price: 329 }, { name: "Jumbo (8 Pc)", price: 599 }],
      addons: [{ name: "Peri-Peri Dip", price: 35 }, { name: "Extra Garlic Mayo", price: 30 }],
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  await assertSucceeds(
    superDb.collection("managers").doc(MANAGER_EMAIL_DOC).set({
      userId: MANAGER_EMAIL_DOC,
      name: "Rahul Manager",
      email: MANAGER_EMAIL,
      phone: "+919876543210",
      active: true,
      canViewOrders: true,
      canManageOrders: true,
      canViewMenu: true,
      canManageMenu: false,
      canViewSales: true,
      createdBy: ADMIN_UID,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Customer cannot create managers, categories, or food items
  const aliceDb = customerCtx(ALICE_UID, "alice@gmail.com").firestore();
  await assertFails(
    aliceDb.collection("managers").doc("alice_gmail_com").set({
      userId: "alice_gmail_com",
      name: "Self Manager",
      email: "alice@gmail.com",
      phone: "+919000011111",
      active: true,
      canViewOrders: true,
      canManageOrders: true,
      canViewMenu: true,
      canManageMenu: true,
      canViewSales: true,
      createdBy: ALICE_UID,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
  await assertFails(
    aliceDb.collection("categories").doc("cat_hack").set({
      id: "cat_hack",
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

test("Manager Google Login RBAC: order status, availability toggle only, cannot create owner or items", async () => {
  const superDb = ownerCtx().firestore();
  await superDb.collection("managers").doc(MANAGER_EMAIL_DOC).set({
    userId: MANAGER_EMAIL_DOC,
    name: "Rahul Manager",
    email: MANAGER_EMAIL,
    phone: "+919876543210",
    active: true,
    canViewOrders: true,
    canManageOrders: true,
    canViewMenu: true,
    canManageMenu: true,
    canViewSales: true,
    createdBy: ADMIN_UID,
    createdAt: pastTime(20000),
    updatedAt: pastTime(20000),
  });

  await superDb.collection("food_items").doc("item_zinger").set({
    id: "item_zinger",
    name: "TFC Spicy Zinger",
    description: "Crunchy chicken fillet burger",
    price: 199,
    imageUrl: "",
    category: "Burgers",
    categoryId: "cat_burgers",
    foodType: "NON_VEG",
    isAvailable: true,
    isPopular: true,
    createdAt: pastTime(20000),
    updatedAt: pastTime(20000),
  });

  const managerDb = managerCtx(true).firestore();

  // Manager can read own manager record via emailDocId
  await assertSucceeds(managerDb.collection("managers").doc(MANAGER_EMAIL_DOC).get());

  // Manager can create own users/{uid} profile with role == 'manager'
  await assertSucceeds(
    managerDb.collection("users").doc(MANAGER_UID).set({
      uid: MANAGER_UID,
      name: "Rahul Manager",
      email: MANAGER_EMAIL,
      phoneNumber: "+919876543210",
      profileImage: "",
      role: "manager",
      isActive: true,
      createdAt: pastTime(10000),
      updatedAt: pastTime(10000),
    })
  );

  // Manager CANNOT escalate own users/{uid} role to 'owner'
  await assertFails(
    managerDb.collection("users").doc(MANAGER_UID).update({
      role: "owner",
      updatedAt: pastTime(),
    })
  );

  // Manager with canManageMenu can toggle isAvailable only
  await assertSucceeds(
    managerDb.collection("food_items").doc("item_zinger").update({
      isAvailable: false,
      updatedAt: pastTime(2000),
    })
  );

  // Manager CANNOT change food price or create/delete food items or create admins/managers
  await assertFails(
    managerDb.collection("food_items").doc("item_zinger").update({
      price: 1,
      updatedAt: pastTime(),
    })
  );
  await assertFails(
    managerDb.collection("food_items").doc("item_new").set({
      id: "item_new",
      name: "Unauthorized Item",
      description: "",
      price: 99,
      imageUrl: "",
      category: "Burgers",
      foodType: "VEG",
      isAvailable: true,
      isPopular: false,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
  await assertFails(
    managerDb.collection("admins").doc(MANAGER_EMAIL_DOC).set({
      userId: MANAGER_EMAIL_DOC,
      name: "Rogue Owner",
      email: MANAGER_EMAIL,
      phone: "+919876543210",
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("Customer addresses: scoped reads/queries and cross-user isolation", async () => {
  const aliceDb = customerCtx(ALICE_UID, "alice@gmail.com").firestore();
  const bobDb = customerCtx(BOB_UID, "bob@gmail.com").firestore();

  await assertSucceeds(
    aliceDb.collection("addresses").doc("addr_1").set({
      userId: ALICE_UID,
      label: "Home",
      fullAddress: "12th Cross, Indiranagar, Bengaluru",
      landmark: "Near Metro Station",
      zoneId: "zone_central",
      latitude: 12.9716,
      longitude: 77.5946,
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
  const superDb = ownerCtx().firestore();
  await superDb.collection("managers").doc(MANAGER_EMAIL_DOC).set({
    userId: MANAGER_EMAIL_DOC,
    name: "Rahul Manager",
    email: MANAGER_EMAIL,
    phone: "+919876543210",
    active: true,
    canViewOrders: true,
    canManageOrders: true,
    canViewMenu: true,
    canManageMenu: false,
    canViewSales: true,
    createdBy: ADMIN_UID,
    createdAt: pastTime(20000),
    updatedAt: pastTime(20000),
  });

  const aliceDb = customerCtx(ALICE_UID, "alice@gmail.com").firestore();
  const bobDb = customerCtx(BOB_UID, "bob@gmail.com").firestore();
  const managerDb = managerCtx(true).firestore();

  // Pickup order with non-zero deliveryFee MUST fail
  await assertFails(
    aliceDb.collection("orders").doc("order_bad_pickup").set({
      orderNumber: "TFC-20261124-0001",
      userId: ALICE_UID,
      customerName: "Alice",
      customerPhone: "+919000011111",
      customerEmail: "alice@gmail.com",
      orderType: "PICKUP",
      status: "Order Placed",
      items: [{ itemId: "item_1", name: "Burger", price: 199, quantity: 1 }],
      subtotal: 199,
      discountAmount: 0,
      deliveryFee: 40,
      totalAmount: 239,
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
      customerPhone: "+919000011111",
      customerEmail: "alice@gmail.com",
      orderType: "DELIVERY",
      status: "Order Placed",
      items: [{ itemId: "item_1", name: "Crispy Burger", price: 199, quantity: 2 }],
      subtotal: 398,
      discountAmount: 0,
      deliveryFee: 40,
      totalAmount: 438,
      deliveryAddress: "12th Cross, Indiranagar",
      landmark: "Near Metro",
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

  // Terminal state lock: Manager cannot modify a Delivered order; only Owner can override
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

  // Manager without canManageMenu cannot toggle food_items
  await assertFails(
    managerDb.collection("food_items").doc("item_denied").set({
      id: "item_denied",
      name: "Denied",
      description: "",
      price: 99,
      imageUrl: "",
      category: "Cat",
      foodType: "VEG",
      isAvailable: true,
      isPopular: false,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});
