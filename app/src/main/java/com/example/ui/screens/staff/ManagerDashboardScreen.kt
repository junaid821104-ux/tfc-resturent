package com.example.ui.screens.staff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.ManagerAccount
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.UiState
import com.example.data.model.VegType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.FoodImageInputField
import com.example.ui.components.FoodPhoto
import com.example.ui.components.LoadingView
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSuccessGreen
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    managerRecord: ManagerAccount?,
    isSuperAdmin: Boolean,
    ordersState: UiState<List<Order>>,
    categoriesState: UiState<List<FoodCategory>>,
    foodItemsState: UiState<List<FoodItem>>,
    onBack: () -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String?, String?) -> Unit,
    onToggleFoodStock: (String, Boolean) -> Unit,
    onSaveFoodItem: (FoodItem, () -> Unit) -> Unit
) {
    BackHandler { onBack() }

    val hasAccess = isSuperAdmin || (managerRecord?.active == true)
    val canManageOrders = isSuperAdmin || (managerRecord?.canManageOrders == true)
    val canManageMenu = isSuperAdmin || (managerRecord?.canManageMenu == true)
    val canViewSales = isSuperAdmin || (managerRecord?.canViewSales == true)

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }
    var showFoodDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Orders", "Menu Stock", "Daily Sales")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TFC Manager Dashboard") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("manager_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (hasAccess && selectedTab == 1 && canManageMenu) {
                FloatingActionButton(
                    onClick = {
                        editingFood = null
                        showFoodDialog = true
                    },
                    containerColor = TfcCrimson,
                    contentColor = TfcPureWhite
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add food item")
                }
            }
        }
    ) { innerPadding ->
        if (!hasAccess) {
            BoxWithConstraints(Modifier.padding(innerPadding)) {
                EmptyStateView(
                    title = "Manager Access Required",
                    subtitle = "Your account does not have active Manager permissions. Ask the TFC Super Admin to assign access."
                )
            }
            return@Scaffold
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("manager_dashboard_screen")
        ) {
            val isWide = maxWidth >= 680.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                            label = { Text("Orders") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Fastfood, contentDescription = null) },
                            label = { Text("Menu") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            label = { Text("Sales") }
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!isWide) {
                        PrimaryTabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> ManagerOrdersTab(
                            canManageOrders = canManageOrders,
                            ordersState = ordersState,
                            onUpdateOrderStatus = onUpdateOrderStatus
                        )
                        1 -> ManagerMenuTab(
                            canManageMenu = canManageMenu,
                            foodItemsState = foodItemsState,
                            onToggleFoodStock = onToggleFoodStock,
                            onEditFood = {
                                editingFood = it
                                showFoodDialog = true
                            }
                        )
                        2 -> ManagerDailySalesTab(
                            canViewSales = canViewSales,
                            ordersState = ordersState
                        )
                    }
                }
            }
        }
    }

    if (showFoodDialog) {
        val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
        FoodItemEditorDialog(
            initial = editingFood,
            categories = categories,
            onDismiss = { showFoodDialog = false },
            onSave = { item ->
                onSaveFoodItem(item) { showFoodDialog = false }
            }
        )
    }
}

@Composable
private fun ManagerOrdersTab(
    canManageOrders: Boolean,
    ordersState: UiState<List<Order>>,
    onUpdateOrderStatus: (String, OrderStatus, String?, String?) -> Unit
) {
    if (!canManageOrders) {
        EmptyStateView("Permission Disabled", "You do not have permission to manage orders.")
        return
    }
    var showCompleted by remember { mutableStateOf(false) }
    var rejectingOrderId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showCompleted,
                onClick = { showCompleted = false },
                label = { Text("Incoming & Active") }
            )
            FilterChip(
                selected = showCompleted,
                onClick = { showCompleted = true },
                label = { Text("Completed & Cancelled") }
            )
        }

        when (ordersState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorStateView(ordersState.message)
            is UiState.Success -> {
                val filtered = ordersState.data.filter { order ->
                    val terminal = OrderStatus.fromRaw(order.status).isTerminal
                    if (showCompleted) terminal else !terminal
                }
                if (filtered.isEmpty()) {
                    EmptyStateView(
                        title = if (showCompleted) "No Completed Orders" else "No Incoming Orders",
                        subtitle = "Orders placed by TFC customers appear here in real time."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            ManagerOrderCard(
                                order = order,
                                onAccept = {
                                    onUpdateOrderStatus(order.id, OrderStatus.ACCEPTED, null, null)
                                },
                                onReject = { rejectingOrderId = order.id },
                                onAdvance = { next ->
                                    val payStatus = if (next == OrderStatus.DELIVERED) "Paid" else null
                                    onUpdateOrderStatus(order.id, next, null, payStatus)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (rejectingOrderId != null) {
        var reason by remember { mutableStateOf("Kitchen at capacity") }
        AlertDialog(
            onDismissRequest = { rejectingOrderId = null },
            title = { Text("Reject Order") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rejection Reason") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = rejectingOrderId ?: return@Button
                        onUpdateOrderStatus(id, OrderStatus.CANCELLED, reason, null)
                        rejectingOrderId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Reject Order") }
            },
            dismissButton = {
                TextButton(onClick = { rejectingOrderId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ManagerOrderCard(
    order: Order,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onAdvance: (OrderStatus) -> Unit
) {
    val status = OrderStatus.fromRaw(order.status)
    val isPickup = OrderType.fromRaw(order.orderType) == OrderType.PICKUP
    val nextStatus = when (status) {
        OrderStatus.ACCEPTED -> OrderStatus.PREPARING
        OrderStatus.PREPARING -> OrderStatus.READY
        OrderStatus.READY -> if (isPickup) OrderStatus.DELIVERED else OrderStatus.OUT_FOR_DELIVERY
        OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
        else -> null
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${order.customerName} • ${if (isPickup) "PICKUP" else "DELIVERY"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatusBadge(order.status)
            }

            Text(
                text = order.items.joinToString("\n") { "• ${it.quantity}x ${it.name}" },
                style = MaterialTheme.typography.bodySmall
            )
            if (!order.cookingInstructions.isNullOrBlank()) {
                Text(
                    text = "Note: ${order.cookingInstructions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TfcCrimson
                )
            }
            Text(
                text = "Total: ${formatCurrency(order.totalAmount)} (${order.paymentMethod})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            if (status == OrderStatus.ORDER_PLACED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject", color = TfcCrimson)
                    }
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = TfcSuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                    }
                }
            } else if (nextStatus != null) {
                Button(
                    onClick = { onAdvance(nextStatus) },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Mark as ${nextStatus.raw}")
                }
            }
        }
    }
}

@Composable
private fun ManagerMenuTab(
    canManageMenu: Boolean,
    foodItemsState: UiState<List<FoodItem>>,
    onToggleFoodStock: (String, Boolean) -> Unit,
    onEditFood: (FoodItem) -> Unit
) {
    if (!canManageMenu) {
        EmptyStateView("Permission Disabled", "You do not have permission to manage food items.")
        return
    }
    when (foodItemsState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorStateView(foodItemsState.message)
        is UiState.Success -> {
            if (foodItemsState.data.isEmpty()) {
                EmptyStateView("No Menu Items", "Tap + to add your first dish to TFC Restaurant.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(foodItemsState.data, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FoodPhoto(
                                    imageUrl = item.imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${formatCurrency(item.effectivePrice)} • ${if (item.available) "In Stock" else "Out of Stock"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.available) TfcSuccessGreen else TfcCrimson
                                    )
                                }
                                Switch(
                                    checked = item.available,
                                    onCheckedChange = { onToggleFoodStock(item.id, it) }
                                )
                                IconButton(
                                    onClick = { onEditFood(item) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit item")
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
private fun ManagerDailySalesTab(
    canViewSales: Boolean,
    ordersState: UiState<List<Order>>
) {
    if (!canViewSales) {
        EmptyStateView("Permission Disabled", "You do not have permission to view sales.")
        return
    }
    when (ordersState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorStateView(ordersState.message)
        is UiState.Success -> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.time
            val todaysOrders = ordersState.data.filter { order ->
                val d = order.createdAt?.toDate()
                d != null && !d.before(startOfToday)
            }
            val validToday = todaysOrders.filter { OrderStatus.fromRaw(it.status) != OrderStatus.CANCELLED }
            val revenueToday = validToday.sumOf { it.totalAmount }
            val deliveredToday = validToday.count { OrderStatus.fromRaw(it.status) == OrderStatus.DELIVERED }
            val activeNow = validToday.count { !OrderStatus.fromRaw(it.status).isTerminal }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Today's Kitchen Summary", style = MaterialTheme.typography.headlineMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Today's Sales", formatCurrency(revenueToday), Modifier.weight(1f), highlight = true)
                    StatCard("Orders Today", validToday.size.toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Active Kitchen", activeNow.toString(), Modifier.weight(1f))
                    StatCard("Delivered", deliveredToday.toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) TfcJetBlack else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlight) TfcPureWhite.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = if (highlight) TfcCrimson else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

@Composable
fun FoodItemEditorDialog(
    initial: FoodItem?,
    categories: List<FoodCategory>,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var priceText by remember { mutableStateOf(initial?.price?.takeIf { it > 0 }?.toString() ?: "") }
    var discountText by remember { mutableStateOf(initial?.discountedPrice?.toString() ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "local://img_crispy_bucket") }
    var selectedCatId by remember {
        mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: "cat_general")
    }
    var vegType by remember { mutableStateOf(VegType.fromRaw(initial?.vegType)) }
    var available by remember { mutableStateOf(initial?.available ?: true) }
    var featured by remember { mutableStateOf(initial?.featured ?: false) }
    var popular by remember { mutableStateOf(initial?.popular ?: false) }
    var prepText by remember { mutableStateOf((initial?.prepTimeMinutes ?: 15L).toString()) }
    var caloriesText by remember { mutableStateOf((initial?.calories ?: 450L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Food Item" else "Edit Food Item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (categories.isNotEmpty()) {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.take(4).forEach { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = vegType == VegType.NON_VEG,
                        onClick = { vegType = VegType.NON_VEG },
                        label = { Text("Non-Veg") }
                    )
                    FilterChip(
                        selected = vegType == VegType.VEG,
                        onClick = { vegType = VegType.VEG },
                        label = { Text("Veg") }
                    )
                }

                FoodImageInputField(imageUrl = imageUrl, onImageUrlChange = { imageUrl = it })

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prepText,
                        onValueChange = { prepText = it },
                        label = { Text("Prep Mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it },
                        label = { Text("Calories") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available (In Stock)")
                    Switch(checked = available, onCheckedChange = { available = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured on Home")
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Popular Badge")
                    Switch(checked = popular, onCheckedChange = { popular = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: return@Button
                    val discount = discountText.toDoubleOrNull()
                    val catName = categories.firstOrNull { it.id == selectedCatId }?.name ?: "General"
                    onSave(
                        FoodItem(
                            id = initial?.id ?: "",
                            name = name.trim(),
                            description = description.trim(),
                            categoryId = selectedCatId,
                            categoryName = catName,
                            price = price,
                            discountedPrice = discount,
                            imageUrl = imageUrl.trim(),
                            vegType = vegType.raw,
                            available = available,
                            featured = featured,
                            popular = popular,
                            prepTimeMinutes = prepText.toLongOrNull() ?: 15L,
                            calories = caloriesText.toLongOrNull() ?: 0L
                        )
                    )
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
            ) { Text("Save Item") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
