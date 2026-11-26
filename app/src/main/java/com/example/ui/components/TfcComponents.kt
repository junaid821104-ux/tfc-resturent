package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.firebase.ImageUploadHelper
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
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

fun formatCurrency(amount: Double): String {
    val rounded = kotlin.math.round(amount * 100.0) / 100.0
    return if (abs(rounded % 1.0) < 0.005) {
        "₹${rounded.toLong()}"
    } else {
        String.format(Locale.US, "₹%.2f", rounded)
    }
}

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
    val trimmed = imageUrl.trim()
    val localDrawable = when (trimmed) {
        "local://img_crispy_bucket" -> R.drawable.img_crispy_bucket
        "local://img_burger_deluxe" -> R.drawable.img_burger_deluxe
        "local://img_hero_banner" -> R.drawable.img_hero_banner
        else -> null
    }
    val decodedBitmap = remember(trimmed) {
        if (trimmed.startsWith("data:image/")) {
            try {
                val base64Part = trimmed.substringAfter("base64,", "")
                if (base64Part.isNotEmpty()) {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    when {
        localDrawable != null -> {
            Image(
                painter = painterResource(id = localDrawable),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        decodedBitmap != null -> {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        trimmed.isNotBlank() -> {
            AsyncImage(
                model = trimmed,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.img_hero_banner),
                error = painterResource(id = R.drawable.img_hero_banner)
            )
        }
        else -> {
            Image(
                painter = painterResource(id = R.drawable.img_crispy_bucket),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
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
    val itemId = item.effectiveId
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("food_card_$itemId"),
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
                if (!item.isAvailable) {
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
                    VegTypeBadge(vegTypeRaw = item.foodType)
                    Text(
                        text = item.effectiveCategory,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isPopular) {
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

                if (item.variants.isNotEmpty() || item.addons.isNotEmpty()) {
                    Text(
                        text = buildList {
                            if (item.variants.isNotEmpty()) add("${item.variants.size} sizes")
                            if (item.addons.isNotEmpty()) add("${item.addons.size} add-ons")
                        }.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TfcCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

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

                    if (!item.isAvailable) {
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
                            tagPrefix = itemId
                        )
                    } else {
                        Button(
                            onClick = onAddClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("add_to_cart_$itemId")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodImageInputField(
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            imageError = null
            scope.launch {
                val res = ImageUploadHelper.compressUriToDataUrl(context, uri)
                isProcessing = false
                res.fold(
                    onSuccess = { onImageUrlChange(it) },
                    onFailure = { imageError = it.localizedMessage ?: "Failed to process gallery image" }
                )
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) {
            isProcessing = true
            imageError = null
            scope.launch {
                val res = ImageUploadHelper.compressUriToDataUrl(context, uri)
                isProcessing = false
                res.fold(
                    onSuccess = { onImageUrlChange(it) },
                    onFailure = { imageError = it.localizedMessage ?: "Failed to process captured photo" }
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                FoodPhoto(
                    imageUrl = imageUrl,
                    contentDescription = "Food preview",
                    modifier = Modifier.fillMaxSize()
                )
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TfcJetBlack.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TfcPureWhite,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Food Dish Photo", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pick_gallery_photo_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                    OutlinedButton(
                        onClick = {
                            val tempUri = ImageUploadHelper.createCameraTempUri(context)
                            pendingCameraUri = tempUri
                            cameraLauncher.launch(tempUri)
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("capture_camera_photo_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                }
            }
        }

        if (imageError != null) {
            Text(
                text = imageError ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TfcCrimson
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { onImageUrlChange("local://img_crispy_bucket") },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) { Text("Bucket Preset") }
            OutlinedButton(
                onClick = { onImageUrlChange("local://img_burger_deluxe") },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) { Text("Burger Preset") }
            OutlinedButton(
                onClick = { onImageUrlChange("local://img_hero_banner") },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) { Text("Platter Preset") }
        }

        if (!imageUrl.startsWith("data:image/")) {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = onImageUrlChange,
                label = { Text("Or paste Image URL") },
                placeholder = { Text("https://... or local://img_burger_deluxe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "Custom photo uploaded and ready to save.",
                style = MaterialTheme.typography.labelSmall,
                color = TfcSuccessGreen
            )
        }
    }
}
