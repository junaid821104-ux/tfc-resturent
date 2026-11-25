package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FoodItem
import com.example.data.model.OrderStatus
import com.example.data.model.VegType
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcInfoBlue
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcNonVegRed
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.theme.TfcSuccessGreen
import com.example.ui.theme.TfcVegGreen
import com.example.ui.theme.TfcWarningAmber
import java.util.Locale

fun formatCurrency(amount: Double): String =
    String.format(Locale.US, "$%.2f", amount)

@Composable
fun TfcLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showWordmark: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(listOf(TfcJetBlack, Color(0xFF2B090C)))
                )
                .border(1.5.dp, TfcCrimson, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TFC",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TfcPureWhite
                )
            )
        }
        if (showWordmark) {
            Column {
                Text(
                    text = "TFC",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "RESTAURANT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TfcCrimson,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun VegTypeBadge(vegTypeRaw: String, modifier: Modifier = Modifier) {
    val vegType = VegType.fromRaw(vegTypeRaw)
    val badgeColor = if (vegType == VegType.VEG) TfcVegGreen else TfcNonVegRed
    Box(
        modifier = modifier
            .size(16.dp)
            .border(1.5.dp, badgeColor, RoundedCornerShape(4.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
    }
}

@Composable
fun FoodPhoto(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val localDrawable = when (imageUrl.trim()) {
        "local://img_crispy_bucket" -> R.drawable.img_crispy_bucket
        "local://img_burger_deluxe" -> R.drawable.img_burger_deluxe
        "local://img_hero_banner" -> R.drawable.img_hero_banner
        else -> null
    }
    if (localDrawable != null) {
        Image(
            painter = painterResource(id = localDrawable),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.img_hero_banner),
            error = painterResource(id = R.drawable.img_hero_banner)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_crispy_bucket),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun FoodItemCard(
    item: FoodItem,
    cartQuantity: Int,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("food_card_${item.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                FoodPhoto(
                    imageUrl = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (!item.available) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TfcJetBlack.copy(alpha = 0.68f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TfcPureWhite,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (item.hasDiscount) {
                    val pct = (((item.price - item.effectivePrice) / item.price) * 100).toInt()
                    Surface(
                        color = TfcCrimson,
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "$pct% OFF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TfcPureWhite,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VegTypeBadge(vegTypeRaw = item.vegType)
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.popular) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Popular item",
                            tint = TfcCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${item.prepTimeMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!item.available) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Unavailable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } else if (cartQuantity > 0) {
                        QuantityStepper(
                            quantity = cartQuantity,
                            onIncrement = onIncrement,
                            onDecrement = onDecrement,
                            tagPrefix = item.id
                        )
                    } else {
                        Button(
                            onClick = onAddClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("add_to_cart_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = TfcCrimson,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("decrement_$tagPrefix")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease quantity",
                    tint = TfcPureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TfcPureWhite,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("increment_$tagPrefix")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase quantity",
                    tint = TfcPureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun OrderStatusBadge(statusRaw: String, modifier: Modifier = Modifier) {
    val status = OrderStatus.fromRaw(statusRaw)
    val (bg, fg) = when (status) {
        OrderStatus.ORDER_PLACED -> TfcSoftRed to TfcCrimson
        OrderStatus.ACCEPTED -> Color(0xFFDBEAFE) to TfcInfoBlue
        OrderStatus.PREPARING -> Color(0xFFFEF3C7) to TfcWarningAmber
        OrderStatus.READY -> Color(0xFFDCFCE7) to TfcSuccessGreen
        OrderStatus.OUT_FOR_DELIVERY -> Color(0xFFEDE9FE) to Color(0xFF6D28D9)
        OrderStatus.DELIVERED -> Color(0xFFDCFCE7) to TfcVegGreen
        OrderStatus.CANCELLED -> Color(0xFFFEE2E2) to TfcNonVegRed
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = status.raw,
            style = MaterialTheme.typography.labelMedium.copy(
                color = fg,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun OrderTimelineStepper(
    currentStatusRaw: String,
    isPickup: Boolean,
    modifier: Modifier = Modifier
) {
    val currentStatus = OrderStatus.fromRaw(currentStatusRaw)
    if (currentStatus == OrderStatus.CANCELLED) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = Color(0xFFFEE2E2),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TfcNonVegRed)
                Text(
                    text = "This order has been cancelled.",
                    style = MaterialTheme.typography.titleSmall.copy(color = TfcNonVegRed)
                )
            }
        }
        return
    }

    val steps = if (isPickup) {
        listOf(
            OrderStatus.ORDER_PLACED,
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.DELIVERED
        )
    } else {
        listOf(
            OrderStatus.ORDER_PLACED,
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
        )
    }

    val currentIndex = steps.indexOf(currentStatus).coerceAtLeast(0)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        steps.forEachIndexed { index, step ->
            val reached = index <= currentIndex
            val label = if (isPickup && step == OrderStatus.DELIVERED) "Picked Up" else step.raw
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = label,
                    tint = if (reached) TfcCrimson else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = label,
                    style = if (index == currentIndex) {
                        MaterialTheme.typography.titleSmall.copy(
                            color = TfcCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (reached) {
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingView(message: String = "Loading TFC Restaurant...", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = TfcCrimson)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_plate),
            contentDescription = title,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = TfcSoftRed),
        border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TfcCrimson)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TfcJetBlack
            )
        }
    }
}

@Composable
fun FoodImageInputField(
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageUrlChange(uri.toString())
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = imageUrl,
            onValueChange = onImageUrlChange,
            label = { Text("Image URL or Photo URI") },
            placeholder = { Text("https://... or local://img_burger_deluxe") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pick Photo")
            }
            OutlinedButton(
                onClick = { onImageUrlChange("local://img_crispy_bucket") },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Chicken Preset")
            }
            OutlinedButton(
                onClick = { onImageUrlChange("local://img_burger_deluxe") },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Burger Preset")
            }
        }
    }
}
