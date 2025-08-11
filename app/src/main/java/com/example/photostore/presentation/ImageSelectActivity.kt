package com.example.photostore.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photostore.utils.ComposeFileProvider
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SelectImage() {
    val ctx = LocalContext.current
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            imageUri.value = uri
            uri?.let { launchHandlingActivity(ctx, it) }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && imageUri.value != null) {
                launchHandlingActivity(ctx, imageUri.value!!)
            }
        }
    )

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "backgroundOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFF2B1B3D)
                    ),
                    center = Offset(500f, 500f)
                )
            )
    ) {
        // Animated background particles
        AnimatedBackground(backgroundOffset)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            var titleVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                titleVisible = true
            }

            AnimatedTitle(visible = titleVisible)

            Spacer(modifier = Modifier.height(60.dp))

            // Subtitle
            Text(
                text = "Choose how you want to add your photo",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Enhanced action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EnhancedActionButton(
                    icon = Icons.Default.AccountBox,
                    title = "Gallery",
                    subtitle = "Choose from library",
                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                    onClick = { imagePicker.launch("image/*") }
                )
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        val uri = ComposeFileProvider.getImageUri(ctx)
                        imageUri.value = uri
                        cameraLauncher.launch(uri)
                    }
                }
                EnhancedActionButton(
                    icon = Icons.Default.PlayArrow,
                    title = "Camera",
                    subtitle = "Take a photo",
                    colors = listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
                    onClick = {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Bottom hint text
            Text(
                text = "Your photos are processed locally and stay private",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}

@Composable
fun AnimatedBackground(offset: Float) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Draw floating particles
        repeat(20) { i ->
            val angle = (offset + i * 18f) * (kotlin.math.PI / 180f)
            val radius = 50f + (i * 20f)
            val x = centerX + (cos(angle) * radius).toFloat()
            val y = centerY + (sin(angle) * radius).toFloat()

            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 2f + (i % 3) * 2f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun AnimatedTitle(visible: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "titleScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(1000),
        label = "titleAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Text(
            text = "📸",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "PhotoStore",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )

        Text(
            text = "Professional Photo Editor",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .graphicsLayer(alpha = alpha)
        )
    }
}

@Composable
fun EnhancedActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4.dp.value else 12.dp.value,
        label = "buttonElevation"
    )

    Card(
        modifier = Modifier
            .size(140.dp, 180.dp)
            .scale(scale)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = colors.first().copy(alpha = 0.3f),
                spotColor = colors.first().copy(alpha = 0.3f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon with background circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

fun launchHandlingActivity(ctx: Context, uri: Uri) {
    val intent = Intent(ctx, EditActivity::class.java)
    intent.putExtra("imageUri", uri.toString())
    ctx.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
fun SelectImagePreview() {
    SelectImage()
}