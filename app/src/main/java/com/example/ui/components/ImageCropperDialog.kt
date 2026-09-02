package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun ImageCropperDialog(
    currentAvatarUrl: String,
    onDismiss: () -> Unit,
    onAvatarCropped: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val presetAvatars = remember {
        listOf(
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
            "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=300&q=80"
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    loadedBitmap = bmp
                    scale = 1f
                    offset = Offset.Zero
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Foto de Perfil & Recorte",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Área Visual de Recorte Circular
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(2.dp, DramaCrimson.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (loadedBitmap != null) {
                        val bmp = loadedBitmap!!
                        val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 4f)
                                        offset = Offset(
                                            x = offset.x + pan.x,
                                            y = offset.y + pan.y
                                        )
                                    }
                                }
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val circleRadius = canvasWidth * 0.42f
                            val centerOffset = Offset(canvasWidth / 2, canvasHeight / 2)

                            // Desenha o bitmap transformado
                            val dstWidth = (bmp.width * scale).toInt()
                            val dstHeight = (bmp.height * scale).toInt()

                            val left = (centerOffset.x - dstWidth / 2 + offset.x).toInt()
                            val top = (centerOffset.y - dstHeight / 2 + offset.y).toInt()

                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset(left, top),
                                dstSize = IntSize(dstWidth, dstHeight)
                            )

                            // Máscara Escura Exterior com Guia Circular
                            val circlePath = Path().apply {
                                addOval(
                                    Rect(
                                        center = centerOffset,
                                        radius = circleRadius
                                    )
                                )
                            }

                            // Desenha a moldura de corte
                            drawCircle(
                                color = DramaCrimsonBright,
                                radius = circleRadius,
                                center = centerOffset,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )
                        }
                    } else {
                        // Exibe avatar atual
                        Box(contentAlignment = Alignment.Center) {
                            if (currentAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentAvatarUrl,
                                    contentDescription = "Avatar Atual",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, DramaCrimson, CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(120.dp)
                                )
                            }
                        }
                    }
                }

                if (loadedBitmap != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.5f..3.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = DramaCrimsonBright,
                                activeTrackColor = DramaCrimson
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "Arraste com o dedo para centralizar a foto",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Escolher da Galeria
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = DramaCrimsonBright, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escolher Imagem da Galeria", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Galeria de Avatares Rápidos
                Text(
                    text = "Ou escolha um avatar temático:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetAvatars) { avatarUrl ->
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar preset",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (currentAvatarUrl == avatarUrl) 2.dp else 1.dp,
                                    color = if (currentAvatarUrl == avatarUrl) DramaCrimsonBright else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onAvatarCropped(avatarUrl)
                                    onDismiss()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botões de Ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (loadedBitmap != null) {
                                try {
                                    val original = loadedBitmap!!
                                    val cropSize = 400
                                    val cropped = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(cropped)

                                    val matrix = Matrix().apply {
                                        postScale(scale, scale)
                                        postTranslate(offset.x, offset.y)
                                    }
                                    canvas.drawColor(android.graphics.Color.BLACK)
                                    canvas.drawBitmap(original, matrix, null)

                                    // Salva no armazenamento interno do app
                                    val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { out ->
                                        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                    }
                                    onAvatarCropped(file.absolutePath)
                                } catch (e: Exception) {
                                    if (selectedUri != null) {
                                        onAvatarCropped(selectedUri.toString())
                                    }
                                }
                            }
                            onDismiss()
                        },
                        enabled = loadedBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salvar Recorte", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
