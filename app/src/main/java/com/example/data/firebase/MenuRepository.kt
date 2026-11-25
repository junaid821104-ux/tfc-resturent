package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.DiscountType
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.RestaurantSettings
import com.example.data.model.VegType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MenuRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

    fun observeCategories(): Flow<List<FoodCategory>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("food_categories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FoodCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "food_categories")
                    throw error
                }
        )
    }

    fun observeFoodItems(): Flow<List<FoodItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("food_items")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FoodItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "food_items")
                    throw error
                }
        )
    }

    fun observeCoupons(): Flow<List<Coupon>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("coupons")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.code }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "coupons")
                    throw error
                }
        )
    }

    fun observeDeliveryZones(): Flow<List<DeliveryZone>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("delivery_zones")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DeliveryZone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.fee }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "delivery_zones")
                    throw error
                }
        )
    }

    fun observeRestaurantSettings(): Flow<RestaurantSettings> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("restaurant_settings").document("main")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(RestaurantSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: RestaurantSettings()
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.GET, "restaurant_settings/main")
                    throw error
                }
        )
    }

    suspend fun saveCategory(category: FoodCategory): Result<Unit> {
        val col = db.collection("food_categories")
        val docRef = if (category.id.isBlank()) col.document() else col.document(category.id)
        return try {
            requireUserId()
            require(category.name.trim().length in 1..60) { "Category name must be 1–60 characters" }
            if (category.id.isBlank()) {
                val payload = mapOf(
                    "name" to category.name.trim(),
                    "description" to category.description.trim().take(200),
                    "imageUrl" to category.imageUrl.trim().take(500),
                    "sortOrder" to category.sortOrder.coerceIn(0L, 9999L),
                    "active" to category.active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to category.name.trim(),
                        "description" to category.description.trim().take(200),
                        "imageUrl" to category.imageUrl.trim().take(500),
                        "sortOrder" to category.sortOrder.coerceIn(0L, 9999L),
                        "active" to category.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val path = "food_categories/$categoryId"
        return try {
            requireUserId()
            require(categoryId.isNotBlank())
            db.collection("food_categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveFoodItem(item: FoodItem): Result<Unit> {
        val col = db.collection("food_items")
        val docRef = if (item.id.isBlank()) col.document() else col.document(item.id)
        return try {
            requireUserId()
            require(item.name.trim().length in 1..100) { "Food name must be 1–100 characters" }
            require(item.price > 0.0 && item.price <= 100000.0) { "Price must be greater than 0" }
            val validDiscount = item.discountedPrice?.takeIf { it > 0.0 && it < item.price }
            if (item.id.isBlank()) {
                val payload = mutableMapOf<String, Any>(
                    "name" to item.name.trim(),
                    "description" to item.description.trim().take(500),
                    "categoryId" to item.categoryId,
                    "categoryName" to item.categoryName.trim().take(60).ifBlank { "General" },
                    "price" to item.price,
                    "imageUrl" to item.imageUrl.trim().take(500),
                    "vegType" to VegType.fromRaw(item.vegType).raw,
                    "available" to item.available,
                    "featured" to item.featured,
                    "popular" to item.popular,
                    "prepTimeMinutes" to item.prepTimeMinutes.coerceIn(1L, 240L),
                    "calories" to item.calories.coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (validDiscount != null) payload["discountedPrice"] = validDiscount
                docRef.set(payload).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "name" to item.name.trim(),
                    "description" to item.description.trim().take(500),
                    "categoryId" to item.categoryId,
                    "categoryName" to item.categoryName.trim().take(60).ifBlank { "General" },
                    "price" to item.price,
                    "imageUrl" to item.imageUrl.trim().take(500),
                    "vegType" to VegType.fromRaw(item.vegType).raw,
                    "available" to item.available,
                    "featured" to item.featured,
                    "popular" to item.popular,
                    "prepTimeMinutes" to item.prepTimeMinutes.coerceIn(1L, 240L),
                    "calories" to item.calories.coerceIn(0L, 10000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                payload["discountedPrice"] = validDiscount ?: FieldValue.delete()
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFoodAvailability(itemId: String, available: Boolean): Result<Unit> {
        val path = "food_items/$itemId"
        return try {
            requireUserId()
            require(itemId.isNotBlank())
            db.collection("food_items").document(itemId).update(
                mapOf(
                    "available" to available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFoodItem(itemId: String): Result<Unit> {
        val path = "food_items/$itemId"
        return try {
            requireUserId()
            require(itemId.isNotBlank())
            db.collection("food_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCoupon(coupon: Coupon): Result<Unit> {
        val col = db.collection("coupons")
        val docRef = if (coupon.id.isBlank()) col.document() else col.document(coupon.id)
        return try {
            requireUserId()
            val cleanCode = coupon.code.trim().uppercase()
            require(cleanCode.length in 2..30) { "Coupon code must be 2–30 characters" }
            require(coupon.discountValue > 0.0 && coupon.discountValue <= 10000.0) { "Discount value must be > 0" }
            if (coupon.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "code" to cleanCode,
                        "title" to coupon.title.trim().take(80),
                        "description" to coupon.description.trim().take(250),
                        "discountType" to DiscountType.fromRaw(coupon.discountType).raw,
                        "discountValue" to coupon.discountValue,
                        "minOrderAmount" to coupon.minOrderAmount.coerceAtLeast(0.0),
                        "maxDiscount" to coupon.maxDiscount.coerceAtLeast(0.0),
                        "active" to coupon.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "code" to cleanCode,
                        "title" to coupon.title.trim().take(80),
                        "description" to coupon.description.trim().take(250),
                        "discountType" to DiscountType.fromRaw(coupon.discountType).raw,
                        "discountValue" to coupon.discountValue,
                        "minOrderAmount" to coupon.minOrderAmount.coerceAtLeast(0.0),
                        "maxDiscount" to coupon.maxDiscount.coerceAtLeast(0.0),
                        "active" to coupon.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCoupon(couponId: String): Result<Unit> {
        val path = "coupons/$couponId"
        return try {
            requireUserId()
            db.collection("coupons").document(couponId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveDeliveryZone(zone: DeliveryZone): Result<Unit> {
        val col = db.collection("delivery_zones")
        val docRef = if (zone.id.isBlank()) col.document() else col.document(zone.id)
        return try {
            requireUserId()
            require(zone.name.trim().length in 1..80) { "Zone name must be 1–80 characters" }
            if (zone.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "name" to zone.name.trim(),
                        "fee" to zone.fee.coerceIn(0.0, 10000.0),
                        "estimatedMinutes" to zone.estimatedMinutes.coerceIn(1L, 180L),
                        "minOrderAmount" to zone.minOrderAmount.coerceIn(0.0, 100000.0),
                        "active" to zone.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to zone.name.trim(),
                        "fee" to zone.fee.coerceIn(0.0, 10000.0),
                        "estimatedMinutes" to zone.estimatedMinutes.coerceIn(1L, 180L),
                        "minOrderAmount" to zone.minOrderAmount.coerceIn(0.0, 100000.0),
                        "active" to zone.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDeliveryZone(zoneId: String): Result<Unit> {
        val path = "delivery_zones/$zoneId"
        return try {
            requireUserId()
            db.collection("delivery_zones").document(zoneId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveRestaurantSettings(settings: RestaurantSettings): Result<Unit> {
        val docRef = db.collection("restaurant_settings").document("main")
        return try {
            requireUserId()
            docRef.set(
                mapOf(
                    "restaurantName" to settings.restaurantName.trim().take(80).ifBlank { "TFC" },
                    "restaurantAddress" to settings.restaurantAddress.trim().take(300),
                    "pickupInstructions" to settings.pickupInstructions.trim().take(300),
                    "openingHours" to settings.openingHours.trim().take(120).ifBlank { "10:00 AM - 11:00 PM" },
                    "orderingOpen" to settings.orderingOpen,
                    "minimumOrderAmount" to settings.minimumOrderAmount.coerceIn(0.0, 100000.0),
                    "baseDeliveryFee" to settings.baseDeliveryFee.coerceIn(0.0, 10000.0),
                    "estimatedPickupMinutes" to settings.estimatedPickupMinutes.coerceIn(1L, 180L),
                    "supportPhone" to settings.supportPhone.trim().take(30),
                    "supportEmail" to settings.supportEmail.trim().take(120),
                    "activeBannerTitle" to settings.activeBannerTitle.trim().take(100),
                    "activeBannerSubtitle" to settings.activeBannerSubtitle.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Admin-triggered setup helper to populate initial TFC Restaurant categories,
     * signature menu items, delivery zones, coupons, and restaurant settings in Firestore.
     */
    suspend fun initializeStarterCatalog(): Result<Unit> {
        return try {
            requireUserId()
            val batch = db.batch()
            val settingsRef = db.collection("restaurant_settings").document("main")
            batch.set(
                settingsRef,
                mapOf(
                    "restaurantName" to "TFC",
                    "restaurantAddress" to "450 Culinary Boulevard, Downtown",
                    "pickupInstructions" to "Present your Order ID at the TFC Express Pickup Counter inside the main lobby.",
                    "openingHours" to "10:00 AM - 11:00 PM",
                    "orderingOpen" to true,
                    "minimumOrderAmount" to 10.0,
                    "baseDeliveryFee" to 2.99,
                    "estimatedPickupMinutes" to 15L,
                    "supportPhone" to "+1 (800) 555-0199",
                    "supportEmail" to "support@tfcrestaurant.com",
                    "activeBannerTitle" to "Crispy Perfection, Delivered Hot",
                    "activeBannerSubtitle" to "Signature fried chicken, gourmet burgers & hand-spun sides.",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val cats = listOf(
                Triple("cat_buckets", "Crispy Buckets", 1L),
                Triple("cat_burgers", "Signature Burgers", 2L),
                Triple("cat_sides", "Sides & Wraps", 3L),
                Triple("cat_drinks", "Drinks & Shakes", 4L)
            )
            cats.forEach { (id, name, order) ->
                batch.set(
                    db.collection("food_categories").document(id),
                    mapOf(
                        "name" to name,
                        "description" to "Freshly prepared $name at TFC Restaurant",
                        "imageUrl" to "",
                        "sortOrder" to order,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val items: List<Map<String, Any>> = listOf(
                mapOf(
                    "docId" to "item_tfc_bucket_8",
                    "name" to "TFC 8-Piece Signature Bucket",
                    "description" to "Eight hand-breaded golden crispy chicken pieces with two house dips.",
                    "categoryId" to "cat_buckets",
                    "categoryName" to "Crispy Buckets",
                    "price" to 24.99,
                    "discountedPrice" to 19.99,
                    "imageUrl" to "local://img_crispy_bucket",
                    "vegType" to "NON_VEG",
                    "featured" to true,
                    "popular" to true,
                    "prepTimeMinutes" to 18L,
                    "calories" to 1420L
                ),
                mapOf(
                    "docId" to "item_spicy_brioche_burger",
                    "name" to "Crimson Crunch Brioche Burger",
                    "description" to "Double-dredged crispy chicken fillet, aged cheddar, spicy chili glaze on toasted brioche.",
                    "categoryId" to "cat_burgers",
                    "categoryName" to "Signature Burgers",
                    "price" to 12.49,
                    "discountedPrice" to 9.99,
                    "imageUrl" to "local://img_burger_deluxe",
                    "vegType" to "NON_VEG",
                    "featured" to true,
                    "popular" to true,
                    "prepTimeMinutes" to 14L,
                    "calories" to 740L
                ),
                mapOf(
                    "docId" to "item_hot_wings_6",
                    "name" to "Nashville Fire Wings (6 pcs)",
                    "description" to "Glazed crispy wings tossed in smoky cayenne butter with cool ranch dip.",
                    "categoryId" to "cat_buckets",
                    "categoryName" to "Crispy Buckets",
                    "price" to 10.99,
                    "imageUrl" to "local://img_crispy_bucket",
                    "vegType" to "NON_VEG",
                    "featured" to false,
                    "popular" to true,
                    "prepTimeMinutes" to 12L,
                    "calories" to 620L
                ),
                mapOf(
                    "docId" to "item_paneer_crunch_burger",
                    "name" to "Crispy Herb Cottage Burger",
                    "description" to "Golden spiced cottage cheese patty, crisp lettuce, and smoky garlic aioli.",
                    "categoryId" to "cat_burgers",
                    "categoryName" to "Signature Burgers",
                    "price" to 9.49,
                    "imageUrl" to "local://img_burger_deluxe",
                    "vegType" to "VEG",
                    "featured" to true,
                    "popular" to false,
                    "prepTimeMinutes" to 12L,
                    "calories" to 590L
                ),
                mapOf(
                    "docId" to "item_truffle_fries",
                    "name" to "Loaded Black-Pepper Fries",
                    "description" to "Skin-on golden fries tossed in cracked black pepper, sea salt, and cheddar drizzle.",
                    "categoryId" to "cat_sides",
                    "categoryName" to "Sides & Wraps",
                    "price" to 5.49,
                    "imageUrl" to "local://img_hero_banner",
                    "vegType" to "VEG",
                    "featured" to false,
                    "popular" to true,
                    "prepTimeMinutes" to 8L,
                    "calories" to 430L
                ),
                mapOf(
                    "docId" to "item_crimson_berry_cooler",
                    "name" to "TFC Crimson Berry Fizz",
                    "description" to "Chilled sparkling hibiscus and cranberry cooler over crushed ice.",
                    "categoryId" to "cat_drinks",
                    "categoryName" to "Drinks & Shakes",
                    "price" to 3.99,
                    "imageUrl" to "local://img_hero_banner",
                    "vegType" to "VEG",
                    "featured" to false,
                    "popular" to false,
                    "prepTimeMinutes" to 5L,
                    "calories" to 140L
                )
            )
            items.forEach { raw ->
                val docId = raw["docId"] as String
                val payload = mutableMapOf<String, Any>()
                raw.forEach { (k, v) -> if (k != "docId") payload[k] = v }
                payload["available"] = true
                payload["createdAt"] = FieldValue.serverTimestamp()
                payload["updatedAt"] = FieldValue.serverTimestamp()
                batch.set(db.collection("food_items").document(docId), payload)
            }

            batch.set(
                db.collection("delivery_zones").document("zone_downtown"),
                mapOf(
                    "name" to "Downtown Core (0–5 km)",
                    "fee" to 1.99,
                    "estimatedMinutes" to 25L,
                    "minOrderAmount" to 10.0,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("delivery_zones").document("zone_suburbs"),
                mapOf(
                    "name" to "Metro Extended (5–12 km)",
                    "fee" to 3.99,
                    "estimatedMinutes" to 40L,
                    "minOrderAmount" to 15.0,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                db.collection("coupons").document("coupon_tfc20"),
                mapOf(
                    "code" to "TFC20",
                    "title" to "20% Off Signature Buckets & Burgers",
                    "description" to "Save 20% up to $8.00 on orders over $15.00.",
                    "discountType" to "PERCENT",
                    "discountValue" to 20.0,
                    "minOrderAmount" to 15.0,
                    "maxDiscount" to 8.0,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "starter_catalog_batch")
            Result.failure(e)
        }
    }

    private fun logScopedFlowError(
        error: Throwable,
        uid: String,
        operationType: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
