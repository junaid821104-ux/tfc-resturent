package com.example.ui.screens.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.FoodAddon
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.FoodVariant
import com.example.data.model.VegType
import com.example.ui.components.FoodImageInputField
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed

private data class DraftOptionRow(
    val id: Int,
    var name: String,
    var priceText: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerFoodEditorContent(
    initial: FoodItem?,
    categories: List<FoodCategory>,
    submitButtonLabel: String = "Save Menu Item",
    resetAfterSave: Boolean = false,
    onSave: (FoodItem, onSaved: () -> Unit) -> Unit
) {
    var nextRowId by remember(initial?.effectiveId) { mutableStateOf(100) }
    var name by remember(initial?.effectiveId) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial?.effectiveId) { mutableStateOf(initial?.description ?: "") }
    var priceText by remember(initial?.effectiveId) {
        mutableStateOf(initial?.price?.takeIf { it > 0.0 }?.let { trimPrice(it) } ?: "")
    }
    var discountText by remember(initial?.effectiveId) {
        mutableStateOf(initial?.discountPrice?.takeIf { it > 0.0 }?.let { trimPrice(it) } ?: "")
    }
    var imageUrl by remember(initial?.effectiveId) {
        mutableStateOf(initial?.imageUrl?.ifBlank { "local://img_crispy_bucket" } ?: "local://img_crispy_bucket")
    }
    var selectedCatId by remember(initial?.effectiveId, categories) {
        mutableStateOf(
            initial?.categoryId?.takeIf { it.isNotBlank() }
                ?: categories.firstOrNull()?.effectiveId
                ?: "cat_general"
        )
    }
    var customCategoryName by remember(initial?.effectiveId) {
        mutableStateOf(initial?.category ?: "")
    }
    var foodType by remember(initial?.effectiveId) {
        mutableStateOf(VegType.fromRaw(initial?.foodType))
    }
    var isAvailable by remember(initial?.effectiveId) { mutableStateOf(initial?.isAvailable ?: true) }
    var isPopular by remember(initial?.effectiveId) { mutableStateOf(initial?.isPopular ?: false) }
    var featured by remember(initial?.effectiveId) { mutableStateOf(initial?.featured ?: false) }
    var prepText by remember(initial?.effectiveId) {
        mutableStateOf((initial?.prepTimeMinutes ?: 15L).toString())
    }
    var caloriesText by remember(initial?.effectiveId) {
        mutableStateOf((initial?.calories ?: 450L).toString())
    }
    var isSaving by remember { mutableStateOf(false) }

    val variants = remember(initial?.effectiveId) {
        mutableStateListOf<DraftOptionRow>().apply {
            initial?.variants?.forEachIndexed { idx, v ->
                add(DraftOptionRow(idx, v.name, trimPrice(v.price)))
            }
        }
    }
    val addons = remember(initial?.effectiveId) {
        mutableStateListOf<DraftOptionRow>().apply {
            initial?.addons?.forEachIndexed { idx, a ->
                add(DraftOptionRow(idx, a.name, trimPrice(a.price)))
            }
        }
    }

    val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
    val canSubmit = !isSaving && name.isNotBlank() && parsedPrice > 0.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FoodImageInputField(
            imageUrl = imageUrl,
            onImageUrlChange = { imageUrl = it }
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food Name *") },
            placeholder = { Text("e.g., Crispy Peri-Peri Bucket") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("food_name_input")
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Food Description") },
            placeholder = { Text("Describe ingredients, crispiness, spices, and portion size...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("food_description_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price (₹) *") },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("food_price_input")
            )
            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = it },
                label = { Text("Discount Price (₹)") },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("food_discount_price_input")
            )
        }

        // Category Selection
        Text("Food Category", style = MaterialTheme.typography.titleSmall)
        if (categories.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val selected = selectedCatId == cat.effectiveId
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCatId = cat.effectiveId
                            customCategoryName = cat.name
                        },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TfcCrimson,
                            selectedLabelColor = TfcPureWhite
                        )
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = customCategoryName,
                onValueChange = { customCategoryName = it },
                label = { Text("Category Name") },
                placeholder = { Text("Buckets, Burgers, Sides...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Veg / Non-Veg
        Text("Food Type", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = foodType == VegType.NON_VEG,
                onClick = { foodType = VegType.NON_VEG },
                label = { Text("Non-Veg") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TfcCrimson,
                    selectedLabelColor = TfcPureWhite
                )
            )
            FilterChip(
                selected = foodType == VegType.VEG,
                onClick = { foodType = VegType.VEG },
                label = { Text("Veg") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TfcJetBlack,
                    selectedLabelColor = TfcPureWhite
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = prepText,
                onValueChange = { prepText = it },
                label = { Text("Prep Time (mins)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = caloriesText,
                onValueChange = { caloriesText = it },
                label = { Text("Calories (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available (In Stock)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark Item as Popular", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPopular, onCheckedChange = { isPopular = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured on Home Banner", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
            }
        }

        HorizontalDivider()

        // Food Variants
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Food Variants (Optional)", style = MaterialTheme.typography.titleSmall)
                Text(
                    "e.g., Half Portion, Full Bucket, Regular / Large",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { variants.add(DraftOptionRow(nextRowId++, "", "")) },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Variant")
            }
        }
        variants.forEachIndexed { idx, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { variants[idx] = row.copy(name = it) },
                    label = { Text("Variant Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = row.priceText,
                    onValueChange = { variants[idx] = row.copy(priceText = it) },
                    label = { Text("₹ Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { variants.removeAt(idx) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove variant", tint = TfcCrimson)
                }
            }
        }

        // Extra Ingredients / Add-ons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Extra Ingredients / Add-ons (Optional)", style = MaterialTheme.typography.titleSmall)
                Text(
                    "e.g., Extra Cheese ₹30, Peri-Peri Dip ₹35",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { addons.add(DraftOptionRow(nextRowId++, "", "")) },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add-on")
            }
        }
        addons.forEachIndexed { idx, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { addons[idx] = row.copy(name = it) },
                    label = { Text("Add-on Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = row.priceText,
                    onValueChange = { addons[idx] = row.copy(priceText = it) },
                    label = { Text("+ ₹ Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { addons.removeAt(idx) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove add-on", tint = TfcCrimson)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                val catObj = categories.firstOrNull { it.effectiveId == selectedCatId }
                val resolvedCatName = catObj?.name ?: customCategoryName.trim().ifBlank { "General" }
                val discount = discountText.toDoubleOrNull()?.takeIf { it > 0.0 && it < parsedPrice }
                val item = FoodItem(
                    docId = initial?.effectiveId ?: "",
                    id = initial?.effectiveId ?: "",
                    name = name.trim(),
                    description = description.trim(),
                    price = parsedPrice,
                    discountPrice = discount,
                    imageUrl = imageUrl.trim(),
                    category = resolvedCatName,
                    categoryId = catObj?.effectiveId ?: selectedCatId,
                    foodType = foodType.raw,
                    isAvailable = isAvailable,
                    isPopular = isPopular,
                    featured = featured,
                    prepTimeMinutes = prepText.toLongOrNull() ?: 15L,
                    calories = caloriesText.toLongOrNull() ?: 0L,
                    variants = variants.mapNotNull { r ->
                        val p = r.priceText.toDoubleOrNull()
                        if (r.name.isNotBlank() && p != null && p > 0.0) FoodVariant(r.name.trim(), p) else null
                    },
                    addons = addons.mapNotNull { r ->
                        val p = r.priceText.toDoubleOrNull()
                        if (r.name.isNotBlank() && p != null && p >= 0.0) FoodAddon(r.name.trim(), p) else null
                    }
                )
                isSaving = true
                onSave(item) {
                    isSaving = false
                    if (resetAfterSave) {
                        name = ""
                        description = ""
                        priceText = ""
                        discountText = ""
                        isPopular = false
                        featured = false
                        variants.clear()
                        addons.clear()
                    }
                }
            },
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .minimumInteractiveComponentSize()
                .testTag("save_menu_item_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isSaving) "Saving..." else submitButtonLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "+ Add New Food" else "Edit ${initial.name}") },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OwnerFoodEditorContent(
                    initial = initial,
                    categories = categories,
                    submitButtonLabel = if (initial == null) "Save New Food" else "Update Food Item",
                    resetAfterSave = false,
                    onSave = { item, done ->
                        onSave(item)
                        done()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun trimPrice(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
