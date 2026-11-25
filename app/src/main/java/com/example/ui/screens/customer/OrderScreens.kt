package com.example.ui.screens.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingView
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.OrderTimelineStepper
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.theme.TfcSuccessGreen
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrderConfirmationScreen(
    orderNumber: String,
    estimatedMinutes: Long,
    totalAmount: Double,
    onTrackOrder: () -> Unit,
    onBackHome: () -> Unit
) {
    BackHandler { onBackHome() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("order_confirmation_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(TfcSuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Order Confirmed",
                        tint = TfcPureWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "Order Placed!",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "TFC Kitchen has received your order and will confirm it shortly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = TfcSoftRed,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("ORDER ID", style = MaterialTheme.typography.labelSmall, color = TfcCrimson)
                        Text(
                            text = orderNumber,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TfcJetBlack
                            )
                        )
                        Text(
                            text = "Total ${formatCurrency(totalAmount)} • Est. ~$estimatedMinutes mins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onTrackOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("track_order_button")
                ) {
                    Text("Track Order Live")
                }

                OutlinedButton(
                    onClick = onBackHome,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("back_to_home_button")
                ) {
                    Text("Back to TFC Menu")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderFlow: Flow<UiState<Order?>>,
    onBack: () -> Unit,
    onCancelOrder: (String) -> Unit
) {
    BackHandler { onBack() }
    val state by orderFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Order Tracking") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tracking_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("order_tracking_screen")
        ) {
            when (val current = state) {
                is UiState.Loading -> LoadingView("Connecting to live order updates...")
                is UiState.Error -> ErrorStateView(current.message)
                is UiState.Success -> {
                    val order = current.data
                    if (order == null) {
                        EmptyStateView(
                            title = "Order not found",
                            subtitle = "This order record could not be loaded.",
                            actionLabel = "Go Back",
                            onAction = onBack
                        )
                    } else {
                        val isPickup = OrderType.fromRaw(order.orderType) == OrderType.PICKUP
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 640.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = TfcJetBlack),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = order.orderNumber,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    color = TfcPureWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            OrderStatusBadge(order.status)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = TfcCrimson,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (isPickup) {
                                                    "Self Pickup • Est. ${order.estimatedMinutes} mins"
                                                } else {
                                                    "Home Delivery • Est. ${order.estimatedMinutes} mins"
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(color = TfcPureWhite.copy(alpha = 0.8f))
                                            )
                                        }
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("Real-Time Progress", style = MaterialTheme.typography.titleMedium)
                                        OrderTimelineStepper(
                                            currentStatusRaw = order.status,
                                            isPickup = isPickup
                                        )
                                        if (!order.rejectionReason.isNullOrBlank()) {
                                            Text(
                                                text = "Kitchen Note: ${order.rejectionReason}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TfcCrimson
                                            )
                                        }
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPickup) Icons.Default.Storefront else Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = TfcCrimson
                                            )
                                            Text(
                                                text = if (isPickup) "Pickup Location" else "Delivery Address",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }
                                        Text(
                                            text = order.deliveryAddress ?: "TFC Restaurant",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (!order.landmark.isNullOrBlank()) {
                                            Text(
                                                text = "Landmark: ${order.landmark}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "${order.paymentMethod} (${order.paymentStatus})",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Items Ordered", style = MaterialTheme.typography.titleMedium)
                                        order.items.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${item.quantity}x ${item.name}", style = MaterialTheme.typography.bodyMedium)
                                                Text(formatCurrency(item.price * item.quantity), style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        if (!order.cookingInstructions.isNullOrBlank()) {
                                            Text(
                                                text = "Instructions: ${order.cookingInstructions}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        HorizontalDivider()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total Paid / Due", style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = formatCurrency(order.totalAmount),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = TfcCrimson,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                if (OrderStatus.fromRaw(order.status) == OrderStatus.ORDER_PLACED) {
                                    OutlinedButton(
                                        onClick = { onCancelOrder(order.id) },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .minimumInteractiveComponentSize()
                                            .testTag("cancel_order_button")
                                    ) {
                                        Text("Cancel Order", color = TfcCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(
    ordersState: UiState<List<Order>>,
    onOpenTracking: (String) -> Unit,
    onReorder: (Order) -> Unit,
    onBrowseMenu: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("my_orders_screen")
    ) {
        Text(
            text = "My Orders",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        when (ordersState) {
            is UiState.Loading -> LoadingView("Loading your TFC orders...")
            is UiState.Error -> ErrorStateView(ordersState.message)
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    EmptyStateView(
                        title = "No Orders Yet",
                        subtitle = "Your live and past TFC Restaurant orders will appear here.",
                        actionLabel = "Explore Menu",
                        onAction = onBrowseMenu
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders, key = { it.id }) { order ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTracking(order.id) }
                                    .testTag("order_card_${order.id}"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(18.dp))
                                            Text(order.orderNumber, style = MaterialTheme.typography.titleSmall)
                                        }
                                        OrderStatusBadge(order.status)
                                    }

                                    Text(
                                        text = order.items.joinToString(", ") { "${it.quantity}x ${it.name}" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = formatCurrency(order.totalAmount),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = TfcCrimson,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            val ts = order.createdAt?.toDate()
                                            if (ts != null) {
                                                Text(
                                                    text = dateFormatter.format(ts),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { onReorder(order) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.size(4.dp))
                                                Text("Reorder")
                                            }
                                            Button(
                                                onClick = { onOpenTracking(order.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Text("Track")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
