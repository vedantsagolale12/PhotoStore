package com.example.photostore.presentation

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.photostore.ui.theme.PhotoStoreTheme
import com.example.photostore.utils.TOOL
import com.example.photostore.utils.applyFilters
import com.example.photostore.utils.createHueRotationMatrix
import com.example.photostore.utils.createSaturationMatrix
import com.example.photostore.utils.multiplyMatrices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


data class ToolButton(
    val tool: TOOL,
    val onClick: () -> Unit,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color = Color(0xFF667eea)
)

class EditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = Color(0xFF0F0F23)
                ) {
                    var uriString: String? = null

                    uriString = intent.extras?.getString("imageUri")
                    if (uriString == null) uriString =
                        intent.extras?.get(Intent.EXTRA_STREAM).toString()
                    val uri = uriString.toUri()

                    val currentTool = remember { mutableStateOf<TOOL?>(null) }
                    val brightness = remember { mutableFloatStateOf(0f) }
                    val saturation = remember { mutableFloatStateOf(1f) }
                    val contrast = remember { mutableFloatStateOf(1f) }
                    val hue = remember { mutableFloatStateOf(0f) }

                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()

                    // Functions for reset, save, and share
                    val resetValues = {
                        brightness.floatValue = 0f
                        saturation.floatValue = 1f
                        contrast.floatValue = 1f
                        hue.floatValue = 0f
                        currentTool.value = null
                        Toast.makeText(context, "Settings reset", Toast.LENGTH_SHORT).show()
                    }

                    val saveImage = {
                        coroutineScope.launch {
                            try {
                                val saved = saveEditedImageToGallery(
                                    context = context,
                                    originalUri = uri,
                                    brightness = brightness.floatValue,
                                    saturation = saturation.floatValue,
                                    contrast = contrast.floatValue,
                                    hue = hue.floatValue
                                )
                                if (saved != null) {
                                    Toast.makeText(
                                        context, "Image saved to gallery!", Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context, "Failed to save image", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    val shareImage = {
                        coroutineScope.launch {
                            try {
                                val tempUri = saveEditedImageTemp(
                                    context = context,
                                    originalUri = uri,
                                    brightness = brightness.floatValue,
                                    saturation = saturation.floatValue,
                                    contrast = contrast.floatValue,
                                    hue = hue.floatValue
                                )
                                if (tempUri != null) {
                                    shareEditedImage(context, tempUri)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to prepare image for sharing",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context, "Error sharing image: ${e.message}", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }


                    val tools = listOf(
                        ToolButton(
                            TOOL.BRIGHTNESS,
                            { currentTool.value = TOOL.BRIGHTNESS },
                            Icons.Default.WbSunny,
                            Color(0xFFFFB347)
                        ), ToolButton(
                            TOOL.SATURATION,
                            { currentTool.value = TOOL.SATURATION },
                            Icons.Default.Palette,
                            Color(0xFF9C27B0)
                        ), ToolButton(
                            TOOL.CONTRAST,
                            { currentTool.value = TOOL.CONTRAST },
                            Icons.Default.Tune,
                            Color(0xFF2196F3)
                        ), ToolButton(
                            TOOL.HUE,
                            { currentTool.value = TOOL.HUE },
                            Icons.Default.ColorLens,
                            Color(0xFF4CAF50)
                        )

                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Animated background
                        AnimatedEditBackground()

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top bar with enhanced design
                            EnhancedTopBar(onSave = saveImage)

                            // Image display area
                            EnhancedImageDisplay(
                                uri = uri,
                                brightness = brightness.floatValue,
                                saturation = saturation.floatValue,
                                contrast = contrast.floatValue,
                                hue = hue.floatValue
                            )

                            // Tool selection header
                            EnhancedToolHeader(tools = tools, currentTool = currentTool)

                            // Tool controls
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            ) {
                                when (currentTool.value) {
                                    TOOL.BRIGHTNESS -> EnhancedToolSlider(
                                        title = "Brightness",
                                        value = brightness,
                                        range = -100f..100f,
                                        color = Color(0xFFFFB347),
                                        icon = Icons.Default.WbSunny
                                    )

                                    TOOL.SATURATION -> EnhancedToolSlider(
                                        title = "Saturation",
                                        value = saturation,
                                        range = 0f..2f,
                                        color = Color(0xFF9C27B0),
                                        icon = Icons.Default.Palette
                                    )

                                    TOOL.CONTRAST -> EnhancedToolSlider(
                                        title = "Contrast",
                                        value = contrast,
                                        range = 0.5f..2f,
                                        color = Color(0xFF2196F3),
                                        icon = Icons.Default.Tune
                                    )

                                    TOOL.HUE -> EnhancedToolSlider(
                                        title = "Hue",
                                        value = hue,
                                        range = -180f..180f,
                                        color = Color(0xFF4CAF50),
                                        icon = Icons.Default.ColorLens
                                    )

                                    null -> DefaultToolState()
                                }
                            }

                            // Bottom action bar
                            EnhancedBottomBar(
                                onReset = resetValues, onShare = shareImage
                            )
                        }
                    }
                }
            }
        }
    }
}


suspend fun saveEditedImageToGallery(
    context: Context,
    originalUri: Uri,
    brightness: Float,
    saturation: Float,
    contrast: Float,
    hue: Float
): Uri? = withContext(Dispatchers.IO) {
    try {
        val originalBitmap = loadBitmapFromUri(context, originalUri) ?: return@withContext null
        val editedBitmap = applyFilters(originalBitmap, brightness, saturation, contrast, hue)

        saveImageToGallery(context, editedBitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun saveEditedImageTemp(
    context: Context,
    originalUri: Uri,
    brightness: Float,
    saturation: Float,
    contrast: Float,
    hue: Float
): Uri? = withContext(Dispatchers.IO) {
    try {
        val originalBitmap = loadBitmapFromUri(context, originalUri) ?: return@withContext null
        val editedBitmap = applyFilters(originalBitmap, brightness, saturation, contrast, hue)

        saveImageTemp(context, editedBitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        android.graphics.BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


private fun saveImageToGallery(context: Context, bitmap: Bitmap): Uri? {
    val filename =
        "PhotoStore_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // For Android 10 and above
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/PhotoStore"
            )
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
        }
        uri
    } else {
        // For Android 9 and below
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val photoStoreDir = File(picturesDir, "PhotoStore")
        if (!photoStoreDir.exists()) {
            photoStoreDir.mkdirs()
        }

        val file = File(photoStoreDir, filename)
        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            // Add to media store
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            )
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}

private fun saveImageTemp(context: Context, bitmap: Bitmap): Uri? {
    val filename = "temp_edited_${System.currentTimeMillis()}.jpg"
    val file = File(context.cacheDir, filename)

    return try {
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }

        androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareEditedImage(context: Context, imageUri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_TEXT, "Check out this edited photo from PhotoStore!")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share edited photo"))
}


@Composable
fun AnimatedEditBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing), repeatMode = RepeatMode.Restart
        ), label = "backgroundOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F0F23), Color(0xFF1a1a3a), Color(0xFF2d1b69)
                ), start = Offset(0f, 0f), end = Offset(size.width, size.height)
            )
        )
    }
}

@Composable
fun EnhancedTopBar(onSave: () -> Job) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { (context as ComponentActivity).finish() },
                modifier = Modifier
                    .background(Color.White.copy(0.1f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White
                )
            }

            Text(
                text = "Photo Editor",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = { onSave() }, modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                        ), shape = CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.Done, contentDescription = "Save", tint = Color.White
                )
            }
        }
    }
}

@Composable
fun EnhancedImageDisplay(
    uri: Uri, brightness: Float, saturation: Float, contrast: Float, hue: Float
) {
    val matrixFilter: androidx.compose.ui.graphics.ColorMatrix =
        remember(brightness, saturation, contrast, hue) {

            val matrix = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,  // Red
                0f, 1f, 0f, 0f, 0f,  // Green
                0f, 0f, 1f, 0f, 0f,  // Blue
                0f, 0f, 0f, 1f, 0f   // Alpha
            )


            val satMatrix = createSaturationMatrix(saturation)
            multiplyMatrices(matrix, satMatrix)


            matrix[4] += brightness / 255f   // Red offset
            matrix[9] += brightness / 255f   // Green offset
            matrix[14] += brightness / 255f  // Blue offset


            val contrastScale = contrast
            val contrastOffset = (1f - contrast) / 2f

            matrix[0] *= contrastScale
            matrix[6] *= contrastScale
            matrix[12] *= contrastScale
            matrix[4] += contrastOffset
            matrix[9] += contrastOffset
            matrix[14] += contrastOffset


            if (hue != 0f) {
                val hueMatrix = createHueRotationMatrix(hue)
                multiplyMatrices(matrix, hueMatrix)
            }

            ColorMatrix(matrix)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Image to edit",
                colorFilter = if (brightness != 0f || saturation != 1f || contrast != 1f || hue != 0f) {
                    ColorFilter.colorMatrix(matrixFilter)
                } else null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, Color.Black.copy(alpha = 0.1f)
                            )
                        ), shape = RoundedCornerShape(24.dp)
                    )
            )
        }
    }
}

@Composable
fun EnhancedToolHeader(tools: List<ToolButton>, currentTool: MutableState<TOOL?>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        LazyRow(
            modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tools) { toolButton ->
                EnhancedToolButton(
                    tool = toolButton,
                    isSelected = currentTool.value == toolButton.tool,
                    onClick = toolButton.onClick
                )
            }
        }
    }
}

@Composable
fun EnhancedToolButton(
    tool: ToolButton, isSelected: Boolean, onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) tool.color else Color.White.copy(alpha = 0.1f),
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    Card(
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource, indication = null
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp, 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.tool.name,
                tint = if (isSelected) Color.White else Color.White.copy(0.7f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.tool.name,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.White.copy(0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EnhancedToolSlider(
    title: String,
    value: MutableState<Float>,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${value.value.roundToInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    modifier = Modifier
                        .background(
                            color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Slider(
                value = value.value,
                onValueChange = { value.value = it },
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DefaultToolState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PhotoFilter,
                contentDescription = null,
                tint = Color.White.copy(0.6f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select a tool to start editing",
                fontSize = 16.sp,
                color = Color.White.copy(0.8f),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Choose from brightness, saturation, contrast, or hue",
                fontSize = 12.sp,
                color = Color.White.copy(0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun EnhancedBottomBar(
    onReset: () -> Unit, onShare: () -> Job
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.Refresh,
                text = "Reset",
                onClick = { onReset() })

            ActionButton(
                icon = Icons.Default.Compare,
                text = "Compare",
                onClick = { /* Compare functionality */ })

            ActionButton(
                icon = Icons.Default.Share, text = "Share", onClick = { onShare() })
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White.copy(0.8f),
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.White.copy(0.8f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}