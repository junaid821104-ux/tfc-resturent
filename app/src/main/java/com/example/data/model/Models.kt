package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class LoginRole(val raw: String, val label: String) {
    CUSTOMER("customer", "Customer"),
    MANAGER("manager", "Manager"),
    OWNER("owner", "Restaurant Owner");

    companion object {
        fun fromRaw(raw: String?): LoginRole =
            entries.firstOrNull { it.raw == raw } ?: CUSTOMER
    }
}

object IndianPhoneValidator {
    const val COUNTRY_CODE = "+91"

    fun digitsOnly(input: String): String = input.filter { it.isDigit() }

    fun extractTenDigits(input: String): String {
        val digits = digitsOnly(input)
        return when {
            digits.length == 10 -> digits
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 11 && digits.startsWith("0") -> digits.substring(1)
            else -> digits.takeLast(10)
        }
    }

    fun isValidTenDigitIndianMobile(tenDigits: String): Boolean =
        tenDigits.length == 10 &&
            tenDigits[0] in '6'..'9' &&
            tenDigits.all { it.isDigit() }

    fun toE164(input: String): String? {
        val digits = extractTenDigits(input)
        return if (isValidTenDigitIndianMobile(digits)) "$COUNTRY_CODE$digits" else null
    }
}

object EmailDocIdHelper {
    private val NON_ALNUM = Regex("[^a-z0-9]")

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        val atIdx = trimmed.indexOf('@')
        val dotIdx = trimmed.lastIndexOf('.')
        return atIdx > 0 && dotIdx > atIdx + 1 && dotIdx < trimmed.length - 1 && !trimmed.contains(' ')
    }

    fun fromEmail(email: String): String =
        email.trim().lowercase().replace(NON_ALNUM, "_")
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
    CASH_ON_DELIVERY("Cash on Delivery", "Pay with cash or UPI on doorstep delivery"),
    CASH_AT_PICKUP("Cash at Pickup", "Pay at the TFC restaurant pickup counter"),
    ONLINE_PAYMENT("Online Payment", "UPI / Card gateway (integration-ready)");

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
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImage: String = "",
    val role: String = LoginRole.CUSTOMER.raw,
    @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    // Legacy compatibility fields for existing Firestore documents
    val userId: String = "",
    val displayName: String = "",
    val phone: String = "",
    val photoUrl: String? = null
) {
    val effectiveUid: String
        get() = uid.ifBlank { userId.ifBlank { id } }

    val effectiveName: String
        get() = name.ifBlank { displayName }

    val effectivePhone: String
        get() = phoneNumber.ifBlank { phone }

    val effectiveProfileImage: String
        get() = profileImage.ifBlank { photoUrl.orEmpty() }

    val isProfileCompleted: Boolean
        get() = effectiveName.isNotBlank() &&
            IndianPhoneValidator.toE164(effectivePhone) != null
}

@Keep
@IgnoreExtraProperties
data class ManagerAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val active: Boolean = true,
    val canViewOrders: Boolean = true,
    val canManageOrders: Boolean = true,
    val canViewMenu: Boolean = true,
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
    val phone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FoodCategory(
    @DocumentId val docId: String = "",
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveId: String get() = docId.ifBlank { id }
}

@Keep
@IgnoreExtraProperties
data class FoodVariant(
    val name: String = "",
    val price: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name.trim(),
        "price" to price
    )
}

@Keep
@IgnoreExtraProperties
data class FoodAddon(
    val name: String = "",
    val price: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name.trim(),
        "price" to price
    )
}

@Keep
@IgnoreExtraProperties
data class FoodItem(
    @DocumentId val docId: String = "",
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val discountPrice: Double? = null,
    val imageUrl: String = "",
    val category: String = "",
    val categoryId: String = "",
    val foodType: String = VegType.NON_VEG.raw,
    @get:PropertyName("isAvailable") @set:PropertyName("isAvailable") var isAvailable: Boolean = true,
    @get:PropertyName("isPopular") @set:PropertyName("isPopular") var isPopular: Boolean = false,
    val featured: Boolean = false,
    val prepTimeMinutes: Long = 15L,
    val calories: Long = 0L,
    val variants: List<FoodVariant> = emptyList(),
    val addons: List<FoodAddon> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveId: String
        get() = docId.ifBlank { id }

    val effectiveCategory: String
        get() = category.ifBlank { "General" }

    val effectivePrice: Double
        get() = if (discountPrice != null && discountPrice > 0.0 && discountPrice < price) {
            discountPrice
        } else {
            price
        }

    val hasDiscount: Boolean
        get() = discountPrice != null && discountPrice > 0.0 && discountPrice < price
}

@Keep
@IgnoreExtraProperties
data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Long = 1L,
    val imageUrl: String = "",
    val vegType: String = VegType.NON_VEG.raw,
    val variantName: String = "",
    val addonsSummary: String = ""
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("itemId", itemId)
        put("name", name)
        put("price", price)
        put("quantity", quantity)
        put("imageUrl", imageUrl)
        put("vegType", vegType)
        if (variantName.isNotBlank()) put("variantName", variantName)
        if (addonsSummary.isNotBlank()) put("addonsSummary", addonsSummary)
    }
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
    val restaurantAddress: String = "14th Main, Indiranagar, Bengaluru 560038",
    val pickupInstructions: String = "Present your Order ID at the TFC Express Pickup Counter inside the main lobby.",
    val openingHours: String = "10:00 AM - 11:30 PM",
    val orderingOpen: Boolean = true,
    val minimumOrderAmount: Double = 0.0,
    val baseDeliveryFee: Double = 40.0,
    val estimatedPickupMinutes: Long = 15L,
    val supportPhone: String = "+919876543210",
    val supportEmail: String = "support@tfcrestaurant.in",
    val ownerPhone: String = "",
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
    val quantity: Int,
    val selectedVariant: FoodVariant? = null,
    val selectedAddons: List<FoodAddon> = emptyList()
) {
    val cartKey: String
        get() {
            val vKey = selectedVariant?.name?.trim().orEmpty()
            val aKey = selectedAddons.map { it.name.trim() }.sorted().joinToString(",")
            return "${foodItem.effectiveId}|$vKey|$aKey"
        }

    val unitPrice: Double
        get() = (selectedVariant?.price ?: foodItem.effectivePrice) + selectedAddons.sumOf { it.price }

    val lineTotal: Double
        get() = unitPrice * quantity

    val customizationSummary: String
        get() = buildList {
            if (selectedVariant != null && selectedVariant.name.isNotBlank()) {
                add(selectedVariant.name)
            }
            if (selectedAddons.isNotEmpty()) {
                add("+ ${selectedAddons.joinToString(", ") { it.name }}")
            }
        }.joinToString(" • ")
}
