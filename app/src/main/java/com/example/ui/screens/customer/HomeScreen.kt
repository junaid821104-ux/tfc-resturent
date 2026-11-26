package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Coupon
import com.example.data.model.DiscountType
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.OrderType
import com.example.data.model.RestaurantSettings
import com.example.data.model.SavedAddress
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.FoodItemCard
import com.example.ui.components.FoodPhoto
import com.example.ui.components.LoadingView
import com.example.ui.components.TfcLogoBadge
import com.example.ui.components.VegTypeBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.theme.TfcSuccessGreen
import com.example.ui.viewmodel.CartSummary
import com.example.ui.viewmodel.CheckoutDraft

@Composable
fun HomeScreen(
    settings: RestaurantSettings,
    categoriesState: UiState<List<FoodCategory>>,
    foodItemsState: UiState<List<FoodItem>>,
    couponsState: UiState<List<Coupon>>,
    addressesState: UiState<List<SavedAddress>>,
    checkoutDraft: CheckoutDraft,
    cartSummary: CartSummary,
    canOpenAdminOrManager: Boolean,
    onOpenSearch: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenFoodDetail: (String) -> Unit,
    onOpenAddressCheckout: () -> Unit,
    onOpenStaffSetup: () -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onApplyCoupon: (String) -> Unit
) {
    val savedAddresses = (addressesState as? UiState.Success)?.data ?: emptyList()
    val currentAddress = savedAddresses.firstOrNull { it.id == checkoutDraft.selectedAddressId }
        ?: savedAddresses.firstOrNull { it.defaultAddress }
        ?: savedAddresses.firstOrNull()
    val locationLabel = when {
        checkoutDraft.orderType == OrderType.PICKUP -> "Pickup: ${settings.restaurantAddress}"
        checkoutDraft.useManualAddress && checkoutDraft.manualAddress.isNotBlank() -> checkoutDraft.manualAddress
        currentAddress != null -> "${currentAddress.label}: ${currentAddress.fullAddress}"
        else -> "Set delivery address"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Logo & Customer Location
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TfcLogoBadge()
                    Surface(
                        color = if (settings.orderingOpen) Color(0xFFDCFCE7) else TfcSoftRed,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (settings.orderingOpen) "OPEN NOW" else "CLOSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (settings.orderingOpen) TfcSuccessGreen else TfcCrimson,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Location Pill
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAddressCheckout)
                        .testTag("home_location_pill"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (checkoutDraft.orderType == OrderType.PICKUP) {
                                    Icons.Default.Storefront
                                } else {
                                    Icons.Default.LocationOn
                                },
                                contentDescription = "Delivery location",
                                tint = TfcCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (checkoutDraft.orderType == OrderType.PICKUP) "Self Pickup" else "Delivering to",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = locationLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Change location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Search bar trigger
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(onClick = onOpenSearch)
                        .testTag("home_search_bar"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search menu",
                            tint = TfcCrimson
                        )
                        Text(
                            text = "Search TFC buckets, burgers, sides...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = TfcJetBlack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "TFC Signature Platter",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        TfcJetBlack.copy(alpha = 0.92f),
                                        TfcJetBlack.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = TfcCrimson,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "TFC KITCHEN • ${settings.openingHours}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TfcPureWhite,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = settings.activeBannerTitle.ifBlank { "Crispy Perfection, Delivered Hot" },
                                style = MaterialTheme.typography.titleLarge.copy(color = TfcPureWhite),
                                maxLines = 2
                            )
                            Text(
                                text = settings.activeBannerSubtitle.ifBlank { "Single-kitchen quality straight to your door." },
                                style = MaterialTheme.typography.bodySmall.copy(color = TfcPureWhite.copy(alpha = 0.85f)),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Offers / Coupons
        val activeCoupons = ((couponsState as? UiState.Success)?.data ?: emptyList()).filter { it.active }
        if (activeCoupons.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Offers & Deals",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeCoupons, key = { it.id }) { coupon ->
                            Card(
                                modifier = Modifier
                                    .width(260.dp)
                                    .clickable { onApplyCoupon(coupon.code) }
                                    .testTag("offer_card_${coupon.code}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = TfcJetBlack)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(TfcCrimson),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.LocalOffer,
                                            contentDescription = null,
                                            tint = TfcPureWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        val badge = if (DiscountType.fromRaw(coupon.discountType) == DiscountType.PERCENT) {
                                            "${coupon.discountValue.toInt()}% OFF"
                                        } else {
                                            "${formatCurrency(coupon.discountValue)} OFF"
                                        }
                                        Text(
                                            text = "$badge • Code ${coupon.code}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = TfcCrimson,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = coupon.title,
                                            style = MaterialTheme.typography.titleSmall.copy(color = TfcPureWhite),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tap to apply • Min ${formatCurrency(coupon.minOrderAmount)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TfcPureWhite.copy(alpha = 0.72f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Food Categories
        val activeCategories = ((categoriesState as? UiState.Success)?.data ?: emptyList()).filter { it.active }
        if (activeCategories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Food Categories", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onOpenCategory(activeCategories.first().effectiveId) }) {
                            Text("See All", color = TfcCrimson)
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeCategories, key = { it.effectiveId }) { cat ->
                            FilterChip(
                                selected = false,
                                onClick = { onOpenCategory(cat.effectiveId) },
                                label = {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TfcCrimson
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("category_chip_${cat.effectiveId}")
                            )
                        }
                    }
                }
            }
        }

        when (foodItemsState) {
            is UiState.Loading -> {
                item { LoadingView("Loading fresh menu from TFC Kitchen...") }
            }
            is UiState.Error -> {
                item { ErrorStateView(foodItemsState.message) }
            }
            is UiState.Success -> {
                val allItems = foodItemsState.data
                if (allItems.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No Dishes Published Yet",
                            subtitle = "Our kitchen menu updates live from TFC Kitchen. Ask the Restaurant Owner to add dishes in the Owner Dashboard.",
                            actionLabel = if (canOpenAdminOrManager) "Open Owner / Staff Dashboard" else null,
                            onAction = if (canOpenAdminOrManager) onOpenStaffSetup else null
                        )
                    }
                } else {
                    val featured = allItems.filter { it.featured }
                    if (featured.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Featured Foods",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(featured, key = { "feat_${it.effectiveId}" }) { item ->
                                        FeaturedFoodCard(
                                            item = item,
                                            cartQuantity = cartSummary.quantityForItem(item.effectiveId),
                                            onClick = { onOpenFoodDetail(item.effectiveId) },
                                            onAdd = { onAddToCart(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val popular = allItems.filter { it.isPopular }
                    if (popular.isNotEmpty()) {
                        item {
                            Text(
                                text = "Popular at TFC",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        items(popular, key = { "pop_${it.effectiveId}" }) { item ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                FoodItemCard(
                                    item = item,
                                    cartQuantity = cartSummary.quantityForItem(item.effectiveId),
                                    onCardClick = { onOpenFoodDetail(item.effectiveId) },
                                    onAddClick = { onAddToCart(item) },
                                    onIncrement = { onIncrement(item.effectiveId) },
                                    onDecrement = { onDecrement(item.effectiveId) }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Full Kitchen Menu (${allItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    items(allItems, key = { "all_${it.effectiveId}" }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            FoodItemCard(
                                item = item,
                                cartQuantity = cartSummary.quantityForItem(item.effectiveId),
                                onCardClick = { onOpenFoodDetail(item.effectiveId) },
                                onAddClick = { onAddToCart(item) },
                                onIncrement = { onIncrement(item.effectiveId) },
                                onDecrement = { onDecrement(item.effectiveId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedFoodCard(
    item: FoodItem,
    cartQuantity: Int,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onClick)
            .testTag("featured_card_${item.effectiveId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                FoodPhoto(
                    imageUrl = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = TfcJetBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TfcPureWhite,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VegTypeBadge(item.foodType)
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formatCurrency(item.effectivePrice),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TfcCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (item.hasDiscount) {
                            Text(
                                text = formatCurrency(item.price),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Button(
                        onClick = onAdd,
                        enabled = item.isAvailable,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        if (item.isAvailable) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(if (cartQuantity > 0) "($cartQuantity)" else "Add")
                        } else {
                            Text("Sold Out", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
