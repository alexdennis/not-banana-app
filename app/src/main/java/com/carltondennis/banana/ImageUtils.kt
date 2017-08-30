package com.carltondennis.banana

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Environment
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Created by alex on 8/26/17.
 *
 * Adapted from https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/env/ImageUtils.java
 */
fun cropAndRescaleBitmap(src: Bitmap, dst: Bitmap, screenOrientation: Int) {
    Timber.d("width: ${src.width}, height: ${src.height}")
    val frameToCropTransform = getTransformationMatrix(
            src.width,
            src.height,
            dst.width,
            dst.height,
            screenOrientation,
            true
    )

    val canvas = Canvas(dst)
    canvas.drawBitmap(src, frameToCropTransform, null)
    Timber.d("Cropped width: ${dst.width}, height: ${dst.height}")
}

fun getTransformationMatrix(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int,
        maintainAspectRatio: Boolean): Matrix {
    val matrix = Matrix()

    if (applyRotation != 0) {
        // Translate so center of image is at origin.
        matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

        // Rotate around origin.
        matrix.postRotate(applyRotation.toFloat())
    }

    // Account for the already applied rotation, if any, and then determine how
    // much scaling is needed for each axis.
    val transpose = (Math.abs(applyRotation) + 90) % 180 == 0

    val inWidth = if (transpose) srcHeight else srcWidth
    val inHeight = if (transpose) srcWidth else srcHeight

    // Apply scaling if necessary.
    if (inWidth != dstWidth || inHeight != dstHeight) {
        val scaleFactorX = dstWidth / inWidth.toFloat()
        val scaleFactorY = dstHeight / inHeight.toFloat()

        if (maintainAspectRatio) {
            // Scale by minimum factor so that dst is filled completely while
            // maintaining the aspect ratio. Some image may fall off the edge.
            val scaleFactor = Math.max(scaleFactorX, scaleFactorY)
            matrix.postScale(scaleFactor, scaleFactor)
        } else {
            // Scale exactly to fill dst from src.
            matrix.postScale(scaleFactorX, scaleFactorY)
        }
    }

    if (applyRotation != 0) {
        // Translate back from origin centered reference to destination frame.
        matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
    }

    return matrix
}

/**
 * Saves a Bitmap object to disk for analysis.
 *
 * @param bitmap The bitmap to save.
 */
fun saveBitmap(bitmap: Bitmap) {
    val root = Environment.getExternalStorageDirectory().absolutePath + File.separator + "tensorflow"
    Timber.i("Saving %dx%d bitmap to %s.", bitmap.width, bitmap.height, root)
    val myDir = File(root)

    if (!myDir.mkdirs()) {
        Timber.i("Make dir failed")
    }

    val fname = "preview.png"
    val file = File(myDir, fname)
    if (file.exists()) {
        file.delete()
    }
    try {
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 99, out)
        out.flush()
        out.close()
    } catch (e: Exception) {
        Timber.e(e, "Exception!")
    }
}