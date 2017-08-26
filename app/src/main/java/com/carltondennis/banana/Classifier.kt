package com.carltondennis.banana

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Created by alex on 8/26/17.
 */
interface Classifier {

    class Recognition(val id:String, val title:String, val confidence:Float, var location:RectF?) {

        override fun toString(): String {
            var resultString = "[$id][$title][$confidence]"
            if (location != null) {
                resultString += "[$location]"
            }
            return resultString.trim()
        }
    }

    fun recognizeImage(bitmap: Bitmap): List<Recognition>
    fun enableStatLogging(debug: Boolean)
    fun getStatString(): String
    fun close()
}