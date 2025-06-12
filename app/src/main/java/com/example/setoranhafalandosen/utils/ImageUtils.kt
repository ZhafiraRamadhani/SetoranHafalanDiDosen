package com.example.setoranhafalandosen.utils


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import kotlin.math.min

/**
 * Utility class untuk optimasi gambar dan mencegah error "Canvas: trying to draw too large bitmap"
 */
object ImageUtils {

    // Maksimum ukuran bitmap yang aman (dalam pixel)
    private const val MAX_BITMAP_SIZE = 2048

    /**
     * Decode resource dengan ukuran yang aman
     */
    fun decodeResourceSafely(context: Context, resourceId: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            // Dapatkan dimensi asli tanpa memuat bitmap
            BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Hitung sample size untuk mengurangi ukuran
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            // Decode dengan sample size yang sudah dihitung
            BitmapFactory.decodeResource(context.resources, resourceId, options)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Hitung sample size untuk mengurangi ukuran bitmap
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Hitung sample size terbesar yang masih menghasilkan ukuran >= reqWidth/reqHeight
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Resize bitmap dengan mempertahankan aspect ratio
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Jika sudah dalam ukuran yang aman, return bitmap asli
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        // Hitung ratio untuk resize
        val ratio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap // Return bitmap asli jika gagal resize
        }
    }

    /**
     * Konversi Dp ke Pixel (versi sederhana)
     */
    fun dpToPx(dp: Dp, density: Float): Int {
        return (dp.value * density).toInt()
    }

    /**
     * Buat ImageRequest yang aman untuk Coil
     */
    fun createSafeImageRequest(
        data: Any,
        context: Context,
        targetSizePx: Int = 360 // Default 120dp * 3 (density)
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .size(targetSizePx, targetSizePx)
            .crossfade(true)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
    }

    /**
     * Buat ImageLoader yang dioptimasi
     */
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .respectCacheHeaders(false)
            .allowHardware(false) // Disable hardware bitmaps untuk kompatibilitas
            .build()
    }

    /**
     * Cek apakah ukuran bitmap aman
     */
    fun isBitmapSafe(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return false

        val width = bitmap.width
        val height = bitmap.height

        // Cek ukuran total pixel
        val totalPixels = width * height
        val maxPixels = MAX_BITMAP_SIZE * MAX_BITMAP_SIZE

        return totalPixels <= maxPixels && width <= MAX_BITMAP_SIZE && height <= MAX_BITMAP_SIZE
    }

    /**
     * Konversi Bitmap ke ImageBitmap dengan aman
     */
    fun toImageBitmapSafely(bitmap: Bitmap?): ImageBitmap? {
        return try {
            if (bitmap != null && isBitmapSafe(bitmap)) {
                bitmap.asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Buat bitmap placeholder jika gambar gagal dimuat
     */
    fun createPlaceholderBitmap(width: Int, height: Int, color: Int = android.graphics.Color.GRAY): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
    }
}
