package org.offlinemesh.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {

    /**
     * Reads an image Uri, downsizes it to maxDimension (e.g. 512px) to keep packet size small,
     * compresses to JPEG (quality 75%), and returns a Base64 string (~15-25 KB)
     * suitable for multi-hop transmission across Bluetooth and Wi-Fi Direct mesh.
     */
    fun compressUriToBase64(context: Context, uri: Uri, maxDimension: Int = 512): String? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds to compute inSampleSize
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            val maxOriginal = max(origWidth, origHeight)
            while ((maxOriginal / inSampleSize) > maxDimension * 2) {
                inSampleSize *= 2
            }

            // 2. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // 16-bit color saves 50% memory
            }

            val sampledBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Scale down accurately to maxDimension
            val scaleFactor = maxDimension.toFloat() / max(sampledBitmap.width, sampledBitmap.height).toFloat()
            val finalBitmap = if (scaleFactor < 1.0f) {
                val targetW = (sampledBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
            } else {
                sampledBitmap
            }

            // 4. Compress to JPEG
            val byteStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteStream)
            val jpegBytes = byteStream.toByteArray()

            Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes Base64 string into an Android Bitmap for rendering.
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
