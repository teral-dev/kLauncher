package com.klauncher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val Background = Color(0xFF080A10)
private val Muted = Color(0xFF858B9C)
private val Accent = Color(0xFF9C8BFF)

data class LauncherApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(8, 10, 16)
        window.navigationBarColor = android.graphics.Color.rgb(8, 10, 16)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                KLauncher()
            }
        }
    }
}

@Composable
fun KLauncher() {
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager

            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            pm.queryIntentActivities(intent, 0)
                .mapNotNull { info ->
                    try {
                        val packageName = info.activityInfo.packageName

                        val drawable = info.loadIcon(pm)
                        val bitmap = Bitmap.createBitmap(
                            144,
                            144,
                            Bitmap.Config.ARGB_8888
                        )

                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, 144, 144)
                        drawable.draw(canvas)

                        LauncherApp(
                            packageName = packageName,
                            label = info.loadLabel(pm).toString(),
                            icon = bitmap.asImageBitmap()
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        }

        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (showDrawer) {
            AppDrawer(
                apps = apps.filter {
                    it.label.contains(search, ignoreCase = true)
                },
                search = search,
                onSearchChange = { search = it },
                onBack = {
                    showDrawer = false
                    search = ""
                },
                onLaunch = { app ->
                    context.packageManager
                        .getLaunchIntentForPackage(app.packageName)
                        ?.let { context.startActivity(it) }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        WindowInsets.systemBars.asPaddingValues()
                    )
                    .padding(horizontal = 22.dp)
            ) {

                Spacer(Modifier.height(12.dp))

                MediaCard()

                Spacer(Modifier.height(12.dp))

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        KText(
                            text = "Uygulamalar yükleniyor",
                            color = Muted,
                            size = 13
                        )
                    }
                } else if (apps.isNotEmpty()) {
                    SpiralCarousel(
                        apps = apps,
                        selectedIndex = selectedIndex,
                        onSelectionChange = {
                            selectedIndex = it
                        },
                        onLaunch = { app ->
                            context.packageManager
                                .getLaunchIntentForPackage(app.packageName)
                                ?.let { context.startActivity(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        KText(
                            "Uygulama bulunamadı",
                            color = Muted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.09f),
                            RoundedCornerShape(30.dp)
                        )
                        .clickable {
                            showDrawer = true
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KText(
                        text = "TÜM UYGULAMALAR   ↑",
                        size = 10,
                        color = Color.White.copy(alpha = 0.8f),
                        weight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MediaCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF191D2A),
                        Color(0xFF10131C),
                        Color(0xFF151525)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(25.dp)
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            KText("♫", size = 25, color = Accent)
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            KText(
                "MEDIA",
                size = 9,
                color = Accent,
                letterSpacing = 2.sp,
                weight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(5.dp))

            KText(
                "Henüz bir şey çalmıyor",
                size = 13,
                color = Color.White
            )

            Spacer(Modifier.height(3.dp))

            KText(
                "Müzik başlatıldığında burada görünecek",
                size = 10,
                color = Muted
            )
        }

        KText(
            "♫",
            size = 22,
            color = Muted
        )
    }
}

@Composable
fun SpiralCarousel(
    apps: List<LauncherApp>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(apps.size, selectedIndex) {
                var drag = 0f

                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount ->
                        drag += amount

                        if (drag < -42f) {
                            onSelectionChange(
                                (selectedIndex + 1) % apps.size
                            )
                            drag = 0f
                        } else if (drag > 42f) {
                            onSelectionChange(
                                (selectedIndex - 1 + apps.size) % apps.size
                            )
                            drag = 0f
                        }
                    },
                    onDragEnd = {
                        drag = 0f
                    },
                    onDragCancel = {
                        drag = 0f
                    }
                )
            }
    ) {
        val centerY = maxHeight / 2

        for (relative in -3..3) {
            val index = (selectedIndex + relative + apps.size) % apps.size
            val app = apps[index]

            val distance = abs(relative)

            val iconSize = when (distance) {
                0 -> 82.dp
                1 -> 62.dp
                2 -> 48.dp
                else -> 37.dp
            }

            val x = 8.dp + (distance * 23).dp
            val y = centerY + (relative * 91).dp - iconSize / 2

            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .zIndex((4 - distance).toFloat())
                    .graphicsLayer {
                        rotationZ = relative * 2.2f
                        alpha = 1f - distance * 0.17f
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIcon(
                        app = app,
                        size = iconSize,
                        focused = relative == 0,
                        onClick = {
                            if (relative == 0) {
                                onLaunch(app)
                            } else {
                                onSelectionChange(index)
                            }
                        }
                    )

                    if (relative == 0) {
                        Spacer(Modifier.width(15.dp))

                        Column {
                            KText(
                                app.label,
                                size = 17,
                                color = Color.White,
                                weight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(4.dp))

                            KText(
                                "ODAK UYGULAMA",
                                size = 8,
                                color = Accent,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassIcon(
    app: LauncherApp,
    size: Dp,
    focused: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(
        if (focused) 27.dp else 19.dp
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
    shadowElevation = if (focused) 22f else 7f
    this.shape = shape
    clip = true
}
            }
            .background(
                Brush.linearGradient(
                    colors = if (focused) {
                        listOf(
                            Color(0xFF34364C),
                            Color(0xFF111522),
                            Color(0xFF252B40)
                        )
                    } else {
                        listOf(
                            Color(0xFF282D3D),
                            Color(0xFF121620),
                            Color(0xFF1D2230)
                        )
                    }
                ),
                shape
            )
            .border(
                width = if (focused) 1.3.dp else 0.7.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (focused) 0.55f else 0.24f),
                        Color(0xFF9B8BFF).copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(if (focused) 14.dp else 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun AppDrawer(
    apps: List<LauncherApp>,
    search: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onLaunch: (LauncherApp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(
                WindowInsets.systemBars.asPaddingValues()
            )
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                KText(
                    "UYGULAMALAR",
                    size = 10,
                    color = Accent,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(5.dp))

                KText(
                    "Kütüphane",
                    size = 28,
                    color = Color.White,
                    weight = FontWeight.Light
                )
            }

            KText(
                "KAPAT  ×",
                size = 10,
                color = Muted,
                modifier = Modifier.clickable(onClick = onBack)
            )
        }

        Spacer(Modifier.height(25.dp))

        BasicTextField(
            value = search,
            onValueChange = onSearchChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 14.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(17.dp),
            decorationBox = { inner ->
                Box {
                    if (search.isEmpty()) {
                        KText(
                            "Uygulama ara...",
                            color = Muted,
                            size = 13
                        )
                    }
                    inner()
                }
            }
        )

        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(76.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(23.dp)
        ) {
            items(
                items = apps,
                key = { it.packageName }
            ) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onLaunch(app)
                    }
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    KText(
                        app.label,
                        size = 10,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun KText(
    text: String,
    size: Int = 13,
    color: Color = Color.White,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        fontSize = size.sp,
        color = color,
        fontWeight = weight,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
