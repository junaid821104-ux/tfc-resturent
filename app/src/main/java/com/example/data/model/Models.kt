package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class VegType(val raw: String, val label: String) {
    NON_VEG("NON_VEG", "Non-Veg"),
    VEG("VEG", "Veg");

    companion object {
        fun fromRaw(raw: String?): VegType =
            entries.firstOrNull { it.raw == raw } ?: NON_VEG
    }
}

enum class OrderType(val raw: String, val label: String) {
    DELIVERY("DELIVERY", "Home Delivery"),
    PICKUP("PICKUP", "Self Pickup");

    companion object {
        fun fromRaw(raw: String?): OrderType =
            entries.firstOrNull { it.raw == raw } ?: DELIVERY
    }
}

enum class OrderStatus(val raw: String) {
    ORDER_PLACED("Order Placed"),
    ACCEPTED("Accepted"),
    PREPARING("Preparing"),
    READY("Ready"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.raw == raw } ?: ORDER_PLACED
    }
}

enum class PaymentMethod(val raw: String, val subtitle: String) {
    CASH_ON_DELIVERY("Cash on Delivery", "Pay with cash when your order arrives"),
    CASH_AT_PICKUP("Cash at Pickup", "Pay at the TFC restaurant pickup counter"),
    ONLINE_PAYMENT("Online Payment", "Card / UPI gateway (integration-ready)");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.raw == raw } ?: CASH_ON_DELIVERY
    }
}

enum class DiscountType(val raw: String) {
    PERCENT("PERCENT"),
    FLAT("FLAT");

    companion object {
        fun fromRaw(raw: String?): DiscountType =
            entries.firstOrNull { it.raw == raw } ?: FLAT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val role: String = "customer",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ManagerAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val active: Boolean = true,
    val canManageOrders: Boolean = true,
    val canManageMenu: Boolean = true,
    val canViewSales: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FoodCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FoodItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val discountedPrice: Double? = null,
    val imageUrl: String = "",
    val vegType: String = VegType.NON_VEG.raw,
    val available: Boolean = true,
    val featured: Boolean = false,
    val popular: Boolean = false,
    val prepTimeMinutes: Long = 15L,
    val calories: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectivePrice: Double
        get() = if (discountedPrice != null && discountedPrice > 0.0 && discountedPrice < price) {
            discountedPrice
        } else {
            price
        }

    val hasDiscount: Boolean
        get() = discountedPrice != null && discountedPrice > 0.0 && discountedPrice < price
}

@Keep
@IgnoreExtraProperties
data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Long = 1L,
    val imageUrl: String = "",
    val vegType: String = VegType.NON_VEG.raw
) {
    fun toMap(): Map<String, Any> = mapOf(
        "itemId" to itemId,
        "name" to name,
        "price" to price,
        "quantity" to quantity,
        "imageUrl" to imageUrl,
        "vegType" to vegType
    )
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val orderType: String = OrderType.DELIVERY.raw,
    val status: String = OrderStatus.ORDER_PLACED.raw,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val couponCode: String? = null,
    val cookingInstructions: String? = null,
    val deliveryAddress: String? = null,
    val landmark: String? = null,
    val deliveryZoneId: String? = null,
    val deliveryZoneName: String? = null,
    val deliveryLat: Double? = null,
    val deliveryLng: Double? = null,
    val estimatedMinutes: Long = 30L,
    val paymentMethod: String = PaymentMethod.CASH_ON_DELIVERY.raw,
    val paymentStatus: String = "Pending",
    val rejectionReason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val fullAddress: String = "",
    val landmark: String = "",
    val zoneId: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val title: String = "",
    val description: String = "",
    val discountType: String = DiscountType.PERCENT.raw,
    val discountValue: Double = 0.0,
    val minOrderAmount: Double = 0.0,
    val maxDiscount: Double = 0.0,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RestaurantSettings(
    @DocumentId val id: String = "main",
    val restaurantName: String = "TFC",
    val restaurantAddress: String = "450 Culinary Boulevard, Downtown",
    val pickupInstructions: String = "Present your Order ID at the TFC Express Pickup Counter inside the main lobby.",
    val openingHours: String = "10:00 AM - 11:00 PM",
    val orderingOpen: Boolean = true,
    val minimumOrderAmount: Double = 10.0,
    val baseDeliveryFee: Double = 2.99,
    val estimatedPickupMinutes: Long = 15L,
    val supportPhone: String = "+1 (800) 555-0199",
    val supportEmail: String = "support@tfcrestaurant.com",
    val activeBannerTitle: String = "Crispy Perfection, Delivered Hot",
    val activeBannerSubtitle: String = "Signature fried chicken, gourmet burgers & hand-spun sides.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryZone(
    @DocumentId val id: String = "",
    val name: String = "",
    val fee: Double = 0.0,
    val estimatedMinutes: Long = 30L,
    val minOrderAmount: Double = 0.0,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val orderNumber: String? = null,
    val status: String = "Open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartLineItem(
    val foodItem: FoodItem,
    val quantity: Int
) {
    val lineTotal: Double get() = foodItem.effectivePrice * quantity
}
