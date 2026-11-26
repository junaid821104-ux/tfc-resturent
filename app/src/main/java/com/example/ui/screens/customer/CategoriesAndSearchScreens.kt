package com.example.ui.screens.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.FoodAddon
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.FoodVariant
import com.example.data.model.UiState
import com.example.data.model.VegType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.FoodItemCard
import com.example.ui.components.FoodPhoto
import com.example.ui.components.LoadingView
import com.example.ui.components.VegTypeBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.viewmodel.CartSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryMenuScreen(
    initialCategoryId: String,
    categoriesState: UiState<List<FoodCategory>>,
    foodItemsState: UiState<List<FoodItem>>,
    cartSummary: CartSummary,
    onBack: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit
) {
    BackHandler { onBack() }

    val categories = ((categoriesState as? UiState.Success)?.data ?: emptyList()).filter { it.active }
    var selectedCategoryId by remember(initialCategoryId) {
        mutableStateOf(initialCategoryId.ifBlank { "ALL" })
    }
    var vegFilter by remember { mutableStateOf<VegType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TFC Categories") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("categories_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("categories_screen")
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == "ALL",
                        onClick = { selectedCategoryId = "ALL" },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TfcCrimson,
                            selectedLabelColor = TfcPureWhite
                        )
                    )
                }
                items(categories, key = { it.effectiveId }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.effectiveId,
                        onClick = { selectedCategoryId = cat.effectiveId },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TfcCrimson,
                            selectedLabelColor = TfcPureWhite
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = vegFilter == VegType.VEG,
                    onClick = { vegFilter = if (vegFilter == VegType.VEG) null else VegType.VEG },
                    label = { Text("Veg Only") }
                )
                FilterChip(
                    selected = vegFilter == VegType.NON_VEG,
                    onClick = { vegFilter = if (vegFilter == VegType.NON_VEG) null else VegType.NON_VEG },
                    label = { Text("Non-Veg") }
                )
            }

            when (foodItemsState) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorStateView(foodItemsState.message)
                is UiState.Success -> {
                    val selectedCatName = categories.firstOrNull { it.effectiveId == selectedCategoryId }?.name
                    val filtered = foodItemsState.data.filter { item ->
                        val catMatch = selectedCategoryId == "ALL" ||
                            item.categoryId == selectedCategoryId ||
                            (selectedCatName != null && item.effectiveCategory.equals(selectedCatName, ignoreCase = true))
                        val vegMatch = vegFilter == null || VegType.fromRaw(item.foodType) == vegFilter
                        catMatch && vegMatch
                    }
                    if (filtered.isEmpty()) {
                        EmptyStateView(
                            title = "No items in this category",
                            subtitle = "Try another category or clear your Veg/Non-Veg filter."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.effectiveId }) { item ->
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
}

@Composable
fun SearchScreen(
    categoriesState: UiState<List<FoodCategory>>,
    foodItemsState: UiState<List<FoodItem>>,
    cartSummary: CartSummary,
    onOpenFoodDetail: (String) -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var onlyDeals by remember { mutableStateOf(false) }
    var vegFilter by remember { mutableStateOf<VegType?>(null) }

    val categories = ((categoriesState as? UiState.Success)?.data ?: emptyList()).filter { it.active }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Search Menu", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search crispy chicken, burgers, dips...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TfcCrimson) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("food_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = onlyDeals,
                        onClick = { onlyDeals = !onlyDeals },
                        label = { Text("Offers") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TfcCrimson,
                            selectedLabelColor = TfcPureWhite
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = vegFilter == VegType.VEG,
                        onClick = { vegFilter = if (vegFilter == VegType.VEG) null else VegType.VEG },
                        label = { Text("Veg") }
                    )
                }
                item {
                    FilterChip(
                        selected = vegFilter == VegType.NON_VEG,
                        onClick = { vegFilter = if (vegFilter == VegType.NON_VEG) null else VegType.NON_VEG },
                        label = { Text("Non-Veg") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text("All Categories") }
                    )
                }
                items(categories, key = { it.effectiveId }) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.effectiveId,
                        onClick = { selectedCategory = cat.effectiveId },
                        label = { Text(cat.name) }
                    )
                }
            }
        }

        when (foodItemsState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorStateView(foodItemsState.message)
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val selectedCatName = categories.firstOrNull { it.effectiveId == selectedCategory }?.name
                val matches = foodItemsState.data.filter { item ->
                    val textOk = q.isEmpty() ||
                        item.name.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.effectiveCategory.lowercase().contains(q)
                    val catOk = selectedCategory == "ALL" ||
                        item.categoryId == selectedCategory ||
                        (selectedCatName != null && item.effectiveCategory.equals(selectedCatName, ignoreCase = true))
                    val dealOk = !onlyDeals || item.hasDiscount
                    val vegOk = vegFilter == null || VegType.fromRaw(item.foodType) == vegFilter
                    textOk && catOk && dealOk && vegOk
                }
                if (matches.isEmpty()) {
                    EmptyStateView(
                        title = "No matching dishes",
                        subtitle = "Try searching with a different keyword or clearing filters."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(matches, key = { it.effectiveId }) { item ->
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodDetailScreen(
    itemId: String,
    foodItemsState: UiState<List<FoodItem>>,
    onBack: () -> Unit,
    onAddToCart: (FoodItem, Int, FoodVariant?, List<FoodAddon>) -> Unit
) {
    BackHandler { onBack() }

    val item = (foodItemsState as? UiState.Success)?.data?.firstOrNull { it.effectiveId == itemId }
    var quantity by remember { mutableIntStateOf(1) }
    var selectedVariant by remember(item?.effectiveId, item?.variants) {
        mutableStateOf(item?.variants?.firstOrNull())
    }
    var selectedAddons by remember(item?.effectiveId) {
        mutableStateOf<List<FoodAddon>>(emptyList())
    }

    val unitPrice = if (item == null) 0.0 else {
        (selectedVariant?.price ?: item.effectivePrice) + selectedAddons.sumOf { it.price }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "Item Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (item != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity -= 1 },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                                }
                                Text(
                                    text = quantity.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (quantity < 25) quantity += 1 },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onAddToCart(item, quantity, selectedVariant, selectedAddons)
                                onBack()
                            },
                            enabled = item.isAvailable,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("detail_add_to_cart_button")
                        ) {
                            Text(
                                text = if (item.isAvailable) {
                                    "Add • ${formatCurrency(unitPrice * quantity)}"
                                } else {
                                    "Currently Out of Stock"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (item == null) {
            Box(Modifier.padding(innerPadding)) {
                EmptyStateView(
                    title = "Item not found",
                    subtitle = "This item may have been removed from the menu.",
                    actionLabel = "Go Back",
                    onAction = onBack
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                ) {
                    FoodPhoto(
                        imageUrl = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VegTypeBadge(item.foodType)
                            Text(
                                text = VegType.fromRaw(item.foodType).label,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Surface(color = TfcSoftRed, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = item.effectiveCategory,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TfcCrimson,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = formatCurrency(unitPrice),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = TfcCrimson,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            if (item.hasDiscount && selectedVariant == null) {
                                Text(
                                    text = formatCurrency(item.price),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = TfcCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("${item.prepTimeMinutes} mins prep", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (item.calories > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = TfcCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("${item.calories} kcal", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (item.variants.isNotEmpty()) {
                            Text("Choose Portion / Variant", style = MaterialTheme.typography.titleMedium)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item.variants.forEach { variant ->
                                    val isSelected = selectedVariant?.name == variant.name
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedVariant = variant },
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isSelected) TfcCrimson else MaterialTheme.colorScheme.outline
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) TfcSoftRed else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedVariant = variant },
                                                    colors = RadioButtonDefaults.colors(selectedColor = TfcCrimson)
                                                )
                                                Text(variant.name, style = MaterialTheme.typography.bodyLarge)
                                            }
                                            Text(
                                                text = formatCurrency(variant.price),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    color = TfcCrimson,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (item.addons.isNotEmpty()) {
                            Text("Extra Ingredients & Add-ons", style = MaterialTheme.typography.titleMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item.addons.forEach { addon ->
                                    val isChecked = selectedAddons.any { it.name == addon.name }
                                    Card(
                                        modifier = Modifier.clickable {
                                            selectedAddons = if (isChecked) {
                                                selectedAddons.filterNot { it.name == addon.name }
                                            } else {
                                                selectedAddons + addon
                                            }
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isChecked) TfcCrimson else MaterialTheme.colorScheme.outline
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChecked) TfcSoftRed else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedAddons = if (checked) {
                                                        selectedAddons + addon
                                                    } else {
                                                        selectedAddons.filterNot { it.name == addon.name }
                                                    }
                                                }
                                            )
                                            Text(
                                                text = "${addon.name} (+${formatCurrency(addon.price)})",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = item.description.ifBlank { "Freshly crafted by TFC Kitchen chefs." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
