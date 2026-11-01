package com.example.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlockyFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thread-safe memory cache and utility for loading installed application icons.
 * Ensures smooth, fast 60fps list rendering without redundant PackageManager IPC calls.
 */
object AppIconHelper {
    private val iconCache = LruCache<String, Bitmap>(250)

    fun getCachedIcon(packageName: String): Bitmap? {
        return iconCache.get(packageName)
    }

    fun loadIcon(context: Context, packageName: String, targetSizePx: Int = 128): Bitmap? {
        val cached = iconCache.get(packageName)
        if (cached != null) return cached

        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable: Drawable = pm.getApplicationIcon(appInfo)
            val bitmap = drawableToBitmap(drawable, targetSizePx)
            if (bitmap != null) {
                iconCache.put(packageName, bitmap)
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else sizePx
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else sizePx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
    }
}

/**
 * High-performance App Icon component matching the user's request:
 * Displays real app icons instead of only names or initial letters.
 */
@Composable
fun MiniAppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 24.dp
) {
    AppIconImage(
        packageName = packageName,
        modifier = modifier,
        sizeDp = sizeDp,
        cornerRadius = 6.dp
    )
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 36.dp,
    fallbackLetter: String? = null,
    cornerRadius: Dp = 10.dp
) {
    val context = LocalContext.current

    val iconState = produceState<Bitmap?>(
        initialValue = AppIconHelper.getCachedIcon(packageName),
        key1 = packageName
    ) {
        if (value == null) {
            val bmp = withContext(Dispatchers.IO) {
                AppIconHelper.loadIcon(context, packageName)
            }
            value = bmp
        }
    }

    val bmp = iconState.value

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Ícone de $packageName",
                modifier = Modifier
                    .size(sizeDp)
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            val letter = (fallbackLetter ?: packageName.split(".").lastOrNull() ?: "A")
                .firstOrNull()?.uppercase() ?: "A"
            Text(
                text = letter,
                fontSize = (sizeDp.value * 0.45f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = BlockyFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
