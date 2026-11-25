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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.DiscountType
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.ManagerAccount
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.RestaurantSettings
import com.example.data.model.SupportTicket
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingView
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isSuperAdmin: Boolean,
    ordersState: UiState<List<Order>>,
    categoriesState: UiState<List<FoodCategory>>,
    foodItemsState: UiState<List<FoodItem>>,
    couponsState: UiState<List<Coupon>>,
    zonesState: UiState<List<DeliveryZone>>,
    settings: RestaurantSettings,
    managersState: UiState<List<ManagerAccount>>,
    usersState: UiState<List<UserProfile>>,
    ticketsState: UiState<List<SupportTicket>>,
    onBack: () -> Unit,
    onInitializeStarterCatalog: () -> Unit,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit,
    onSaveCategory: (FoodCategory, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveFoodItem: (FoodItem, () -> Unit) -> Unit,
    onDeleteFoodItem: (String) -> Unit,
    onSaveCoupon: (Coupon, () -> Unit) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onSaveDeliveryZone: (DeliveryZone, () -> Unit) -> Unit,
    onDeleteDeliveryZone: (String) -> Unit,
    onSaveSettings: (RestaurantSettings) -> Unit,
    onSaveManager: (ManagerAccount, Boolean, () -> Unit) -> Unit,
    onDeleteManager: (String) -> Unit,
    onResolveTicket: (String, String) -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Analytics", "Catalog", "Settings", "Team")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TFC Super Admin") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isSuperAdmin) {
            BoxWithConstraints(Modifier.padding(innerPadding)) {
                EmptyStateView(
                    title = "Super Admin Access Only",
                    subtitle = "This portal is restricted to verified TFC Super Admin accounts."
                )
            }
            return@Scaffold
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("admin_dashboard_screen")
        ) {
            val isWide = maxWidth >= 680.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            label = { Text("Analytics") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Fastfood, contentDescription = null) },
                            label = { Text("Catalog") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Settings") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.People, contentDescription = null) },
                            label = { Text("Team") }
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
                        0 -> AdminAnalyticsAndOrdersTab(
                            ordersState = ordersState,
                            foodItemsCount = (foodItemsState as? UiState.Success)?.data?.size ?: 0,
                            onInitializeStarterCatalog = onInitializeStarterCatalog,
                            onUpdateOrderStatus = onUpdateOrderStatus
                        )
                        1 -> AdminCatalogTab(
                            categoriesState = categoriesState,
                            foodItemsState = foodItemsState,
                            couponsState = couponsState,
                            onSaveCategory = onSaveCategory,
                            onDeleteCategory = onDeleteCategory,
                            onSaveFoodItem = onSaveFoodItem,
                            onDeleteFoodItem = onDeleteFoodItem,
                            onSaveCoupon = onSaveCoupon,
                            onDeleteCoupon = onDeleteCoupon
                        )
                        2 -> AdminSettingsAndZonesTab(
                            settings = settings,
                            zonesState = zonesState,
                            onSaveSettings = onSaveSettings,
                            onSaveZone = onSaveDeliveryZone,
                            onDeleteZone = onDeleteDeliveryZone
                        )
                        3 -> AdminTeamAndCustomersTab(
                            managersState = managersState,
                            usersState = usersState,
                            ticketsState = ticketsState,
                            onSaveManager = onSaveManager,
                            onDeleteManager = onDeleteManager,
                            onResolveTicket = onResolveTicket
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAnalyticsAndOrdersTab(
    ordersState: UiState<List<Order>>,
    foodItemsCount: Int,
    onInitializeStarterCatalog: () -> Unit,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (foodItemsCount == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TfcSoftRed),
                    border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Database Menu Setup", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your Firestore menu is currently empty. Seed starter TFC categories, signature dishes, delivery zones, and coupon codes in one tap, or add them manually in Catalog.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onInitializeStarterCatalog,
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("init_starter_catalog_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Initialize TFC Starter Menu & Settings")
                        }
                    }
                }
            }
        }

        when (ordersState) {
            is UiState.Loading -> item { LoadingView() }
            is UiState.Error -> item { ErrorStateView(ordersState.message) }
            is UiState.Success -> {
                val orders = ordersState.data
                val nonCancelled = orders.filter { OrderStatus.fromRaw(it.status) != OrderStatus.CANCELLED }
                val totalRevenue = nonCancelled.sumOf { it.totalAmount }
                val avgOrder = if (nonCancelled.isNotEmpty()) totalRevenue / nonCancelled.size else 0.0
                val deliveryCount = orders.count { OrderType.fromRaw(it.orderType) == OrderType.DELIVERY }
                val pickupCount = orders.count { OrderType.fromRaw(it.orderType) == OrderType.PICKUP }

                item {
                    Text("Sales & Order Analytics", style = MaterialTheme.typography.titleLarge)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Total Revenue", formatCurrency(totalRevenue), Modifier.weight(1f), highlight = true)
                        StatCard("Total Orders", orders.size.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Avg Order Value", formatCurrency(avgOrder), Modifier.weight(1f))
                        StatCard("Delivery / Pickup", "$deliveryCount / $pickupCount", Modifier.weight(1f))
                    }
                }
                item {
                    Text("All Orders (${orders.size})", style = MaterialTheme.typography.titleMedium)
                }
                if (orders.isEmpty()) {
                    item {
                        EmptyStateView("No Orders Yet", "Customer orders will stream here in real time.")
                    }
                } else {
                    items(orders, key = { it.id }) { order ->
                        var showStatusDialog by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(order.orderNumber, style = MaterialTheme.typography.titleSmall)
                                    OrderStatusBadge(order.status)
                                }
                                Text(
                                    "${order.customerName} (${order.customerEmail.ifBlank { order.customerPhone }})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        formatCurrency(order.totalAmount),
                                        style = MaterialTheme.typography.titleSmall.copy(color = TfcCrimson, fontWeight = FontWeight.Bold)
                                    )
                                    OutlinedButton(onClick = { showStatusDialog = true }) {
                                        Text("Override Status")
                                    }
                                }
                            }
                        }

                        if (showStatusDialog) {
                            AlertDialog(
                                onDismissRequest = { showStatusDialog = false },
                                title = { Text("Update ${order.orderNumber}") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OrderStatus.entries.forEach { st ->
                                            OutlinedButton(
                                                onClick = {
                                                    onUpdateOrderStatus(order.id, st)
                                                    showStatusDialog = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text(st.raw) }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showStatusDialog = false }) { Text("Close") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCatalogTab(
    categoriesState: UiState<List<FoodCategory>>,
    foodItemsState: UiState<List<FoodItem>>,
    couponsState: UiState<List<Coupon>>,
    onSaveCategory: (FoodCategory, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveFoodItem: (FoodItem, () -> Unit) -> Unit,
    onDeleteFoodItem: (String) -> Unit,
    onSaveCoupon: (Coupon, () -> Unit) -> Unit,
    onDeleteCoupon: (String) -> Unit
) {
    var section by remember { mutableIntStateOf(0) } // 0=Items, 1=Categories, 2=Coupons
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }
    var showFoodDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<FoodCategory?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var showCouponDialog by remember { mutableStateOf(false) }

    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val items = (foodItemsState as? UiState.Success)?.data ?: emptyList()
    val coupons = (couponsState as? UiState.Success)?.data ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("Items (${items.size})") })
            FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("Categories (${categories.size})") })
            FilterChip(selected = section == 2, onClick = { section = 2 }, label = { Text("Coupons (${coupons.size})") })
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (section) {
                0 -> {
                    item {
                        Button(
                            onClick = { editingFood = null; showFoodDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Food Product")
                        }
                    }
                    items(items, key = { it.id }) { item ->
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${item.categoryName} • ${formatCurrency(item.effectivePrice)} • ${if (item.available) "Active" else "Out of Stock"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editingFood = item; showFoodDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onDeleteFoodItem(item.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TfcCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    item {
                        Button(
                            onClick = { editingCategory = null; showCategoryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Food Category")
                        }
                    }
                    items(categories, key = { it.id }) { cat ->
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                    Text("Order: ${cat.sortOrder} • ${if (cat.active) "Active" else "Hidden"}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = { editingCategory = cat; showCategoryDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TfcCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    item {
                        Button(
                            onClick = { editingCoupon = null; showCouponDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Coupon / Offer")
                        }
                    }
                    items(coupons, key = { it.id }) { coupon ->
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${coupon.code} — ${coupon.title}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${coupon.discountType}: ${coupon.discountValue} • Min ${formatCurrency(coupon.minOrderAmount)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editingCoupon = coupon; showCouponDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onDeleteCoupon(coupon.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TfcCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFoodDialog) {
        FoodItemEditorDialog(
            initial = editingFood,
            categories = categories,
            onDismiss = { showFoodDialog = false },
            onSave = { onSaveFoodItem(it) { showFoodDialog = false } }
        )
    }

    if (showCategoryDialog) {
        var name by remember { mutableStateOf(editingCategory?.name ?: "") }
        var desc by remember { mutableStateOf(editingCategory?.description ?: "") }
        var sortText by remember { mutableStateOf((editingCategory?.sortOrder ?: 1L).toString()) }
        var active by remember { mutableStateOf(editingCategory?.active ?: true) }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(if (editingCategory == null) "Add Category" else "Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    OutlinedTextField(
                        value = sortText,
                        onValueChange = { sortText = it },
                        label = { Text("Sort Order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCategory(
                            FoodCategory(
                                id = editingCategory?.id ?: "",
                                name = name.trim(),
                                description = desc.trim(),
                                sortOrder = sortText.toLongOrNull() ?: 1L,
                                active = active
                            )
                        ) { showCategoryDialog = false }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCouponDialog) {
        var code by remember { mutableStateOf(editingCoupon?.code ?: "") }
        var title by remember { mutableStateOf(editingCoupon?.title ?: "") }
        var desc by remember { mutableStateOf(editingCoupon?.description ?: "") }
        var type by remember { mutableStateOf(DiscountType.fromRaw(editingCoupon?.discountType)) }
        var valueText by remember { mutableStateOf((editingCoupon?.discountValue ?: 15.0).toString()) }
        var minOrderText by remember { mutableStateOf((editingCoupon?.minOrderAmount ?: 10.0).toString()) }
        var maxDiscText by remember { mutableStateOf((editingCoupon?.maxDiscount ?: 10.0).toString()) }
        var active by remember { mutableStateOf(editingCoupon?.active ?: true) }
        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text(if (editingCoupon == null) "Add Coupon" else "Edit Coupon") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Code (e.g. TFC20)") })
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Offer Title") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == DiscountType.PERCENT, onClick = { type = DiscountType.PERCENT }, label = { Text("Percent %") })
                        FilterChip(selected = type == DiscountType.FLAT, onClick = { type = DiscountType.FLAT }, label = { Text("Flat $") })
                    }
                    OutlinedTextField(value = valueText, onValueChange = { valueText = it }, label = { Text("Discount Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = minOrderText, onValueChange = { minOrderText = it }, label = { Text("Min Order ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = maxDiscText, onValueChange = { maxDiscText = it }, label = { Text("Max Discount ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCoupon(
                            Coupon(
                                id = editingCoupon?.id ?: "",
                                code = code.trim(),
                                title = title.trim(),
                                description = desc.trim(),
                                discountType = type.raw,
                                discountValue = valueText.toDoubleOrNull() ?: 10.0,
                                minOrderAmount = minOrderText.toDoubleOrNull() ?: 0.0,
                                maxDiscount = maxDiscText.toDoubleOrNull() ?: 0.0,
                                active = active
                            )
                        ) { showCouponDialog = false }
                    },
                    enabled = code.trim().length >= 2 && title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCouponDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminSettingsAndZonesTab(
    settings: RestaurantSettings,
    zonesState: UiState<List<DeliveryZone>>,
    onSaveSettings: (RestaurantSettings) -> Unit,
    onSaveZone: (DeliveryZone, () -> Unit) -> Unit,
    onDeleteZone: (String) -> Unit
) {
    var address by remember(settings) { mutableStateOf(settings.restaurantAddress) }
    var pickupNotes by remember(settings) { mutableStateOf(settings.pickupInstructions) }
    var hours by remember(settings) { mutableStateOf(settings.openingHours) }
    var orderingOpen by remember(settings) { mutableStateOf(settings.orderingOpen) }
    var minOrder by remember(settings) { mutableStateOf(settings.minimumOrderAmount.toString()) }
    var baseFee by remember(settings) { mutableStateOf(settings.baseDeliveryFee.toString()) }
    var pickupMins by remember(settings) { mutableStateOf(settings.estimatedPickupMinutes.toString()) }
    var supportPhone by remember(settings) { mutableStateOf(settings.supportPhone) }
    var supportEmail by remember(settings) { mutableStateOf(settings.supportEmail) }
    var bannerTitle by remember(settings) { mutableStateOf(settings.activeBannerTitle) }
    var bannerSub by remember(settings) { mutableStateOf(settings.activeBannerSubtitle) }

    var showZoneDialog by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<DeliveryZone?>(null) }
    val zones = (zonesState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Restaurant Controls & Settings", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TfcSoftRed),
                border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Restaurant Ordering", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (orderingOpen) "Customers can place new orders" else "Ordering paused for customers",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = orderingOpen, onCheckedChange = { orderingOpen = it })
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Restaurant Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pickupNotes, onValueChange = { pickupNotes = it }, label = { Text("Pickup Instructions") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Opening Hours") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = minOrder, onValueChange = { minOrder = it }, label = { Text("Min Order ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = baseFee, onValueChange = { baseFee = it }, label = { Text("Base Delivery ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = pickupMins, onValueChange = { pickupMins = it }, label = { Text("Pickup ETA (mins)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = supportPhone, onValueChange = { supportPhone = it }, label = { Text("Support Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Home Banner Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerSub, onValueChange = { bannerSub = it }, label = { Text("Home Banner Subtitle") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        onSaveSettings(
                            settings.copy(
                                restaurantAddress = address,
                                pickupInstructions = pickupNotes,
                                openingHours = hours,
                                orderingOpen = orderingOpen,
                                minimumOrderAmount = minOrder.toDoubleOrNull() ?: settings.minimumOrderAmount,
                                baseDeliveryFee = baseFee.toDoubleOrNull() ?: settings.baseDeliveryFee,
                                estimatedPickupMinutes = pickupMins.toLongOrNull() ?: settings.estimatedPickupMinutes,
                                supportPhone = supportPhone,
                                supportEmail = supportEmail,
                                activeBannerTitle = bannerTitle,
                                activeBannerSubtitle = bannerSub
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) { Text("Save Restaurant Settings") }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Delivery Zones (${zones.size})", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { editingZone = null; showZoneDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcJetBlack)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Zone")
                }
            }
        }

        items(zones, key = { it.id }) { zone ->
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(zone.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Fee ${formatCurrency(zone.fee)} • ${zone.estimatedMinutes} mins • Min ${formatCurrency(zone.minOrderAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row {
                        IconButton(onClick = { editingZone = zone; showZoneDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit zone")
                        }
                        IconButton(onClick = { onDeleteZone(zone.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete zone", tint = TfcCrimson)
                        }
                    }
                }
            }
        }
    }

    if (showZoneDialog) {
        var name by remember { mutableStateOf(editingZone?.name ?: "") }
        var fee by remember { mutableStateOf((editingZone?.fee ?: 2.99).toString()) }
        var eta by remember { mutableStateOf((editingZone?.estimatedMinutes ?: 30L).toString()) }
        var minOrd by remember { mutableStateOf((editingZone?.minOrderAmount ?: 10.0).toString()) }
        var active by remember { mutableStateOf(editingZone?.active ?: true) }
        AlertDialog(
            onDismissRequest = { showZoneDialog = false },
            title = { Text(if (editingZone == null) "Add Delivery Zone" else "Edit Delivery Zone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Zone Name") })
                    OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Delivery Fee ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = eta, onValueChange = { eta = it }, label = { Text("Estimated Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = minOrd, onValueChange = { minOrd = it }, label = { Text("Min Order ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveZone(
                            DeliveryZone(
                                id = editingZone?.id ?: "",
                                name = name.trim(),
                                fee = fee.toDoubleOrNull() ?: 0.0,
                                estimatedMinutes = eta.toLongOrNull() ?: 30L,
                                minOrderAmount = minOrd.toDoubleOrNull() ?: 0.0,
                                active = active
                            )
                        ) { showZoneDialog = false }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showZoneDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminTeamAndCustomersTab(
    managersState: UiState<List<ManagerAccount>>,
    usersState: UiState<List<UserProfile>>,
    ticketsState: UiState<List<SupportTicket>>,
    onSaveManager: (ManagerAccount, Boolean, () -> Unit) -> Unit,
    onDeleteManager: (String) -> Unit,
    onResolveTicket: (String, String) -> Unit
) {
    var showManagerDialog by remember { mutableStateOf(false) }
    var editingManager by remember { mutableStateOf<ManagerAccount?>(null) }

    val managers = (managersState as? UiState.Success)?.data ?: emptyList()
    val customers = (usersState as? UiState.Success)?.data ?: emptyList()
    val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Managers (${managers.size})", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { editingManager = null; showManagerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Manager")
                }
            }
        }
        if (managers.isEmpty()) {
            item {
                Text(
                    "No managers added yet. Ask a staff member to copy their UID from Profile and add them here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(managers, key = { it.id }) { mgr ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mgr.name, style = MaterialTheme.typography.titleSmall)
                            Text("${mgr.email.ifBlank { mgr.phone }} • ${if (mgr.active) "Active" else "Suspended"}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Perms: ${listOfNotNull(if (mgr.canManageOrders) "Orders" else null, if (mgr.canManageMenu) "Menu" else null, if (mgr.canViewSales) "Sales" else null).joinToString(", ").ifBlank { "None" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TfcCrimson
                            )
                        }
                        Row {
                            IconButton(onClick = { editingManager = mgr; showManagerDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit manager")
                            }
                            IconButton(onClick = { onDeleteManager(mgr.userId) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove manager", tint = TfcCrimson)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Registered Customers (${customers.size})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }
        items(customers, key = { it.id }) { user ->
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(user.email.ifBlank { user.phone }, style = MaterialTheme.typography.bodySmall)
                    Text("UID: ${user.userId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (tickets.isNotEmpty()) {
            item {
                Text("Customer Support Tickets (${tickets.size})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            }
            items(tickets, key = { it.id }) { ticket ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ticket.subject, style = MaterialTheme.typography.titleSmall)
                            Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                            Text("${ticket.userEmail} • ${ticket.status}", style = MaterialTheme.typography.labelSmall, color = TfcCrimson)
                        }
                        if (ticket.status == "Open") {
                            OutlinedButton(onClick = { onResolveTicket(ticket.id, "Resolved") }) {
                                Text("Resolve")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManagerDialog) {
        val isEdit = editingManager != null
        var uid by remember { mutableStateOf(editingManager?.userId ?: "") }
        var name by remember { mutableStateOf(editingManager?.name ?: "") }
        var email by remember { mutableStateOf(editingManager?.email ?: "") }
        var phone by remember { mutableStateOf(editingManager?.phone ?: "") }
        var active by remember { mutableStateOf(editingManager?.active ?: true) }
        var canOrders by remember { mutableStateOf(editingManager?.canManageOrders ?: true) }
        var canMenu by remember { mutableStateOf(editingManager?.canManageMenu ?: true) }
        var canSales by remember { mutableStateOf(editingManager?.canViewSales ?: true) }

        AlertDialog(
            onDismissRequest = { showManagerDialog = false },
            title = { Text(if (isEdit) "Edit Manager" else "Add Manager") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uid,
                        onValueChange = { uid = it },
                        enabled = !isEdit,
                        label = { Text("Firebase User ID (UID)") }
                    )
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Manager Name") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Account Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Manage Orders")
                        Switch(checked = canOrders, onCheckedChange = { canOrders = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Manage Menu Items")
                        Switch(checked = canMenu, onCheckedChange = { canMenu = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("View Daily Sales")
                        Switch(checked = canSales, onCheckedChange = { canSales = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveManager(
                            ManagerAccount(
                                userId = uid.trim(),
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                active = active,
                                canManageOrders = canOrders,
                                canManageMenu = canMenu,
                                canViewSales = canSales
                            ),
                            isEdit
                        ) { showManagerDialog = false }
                    },
                    enabled = uid.isNotBlank() && name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save Manager") }
            },
            dismissButton = { TextButton(onClick = { showManagerDialog = false }) { Text("Cancel") } }
        )
    }
}
