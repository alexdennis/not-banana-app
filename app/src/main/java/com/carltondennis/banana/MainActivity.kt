package com.carltondennis.banana

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val INPUT_SIZE = 299
    private val IMAGE_MEAN = 128
    private val IMAGE_STD = 128.0f
    private val INPUT_NAME = "Mul:0"
    private val OUTPUT_NAME = "final_result"

    private val MODEL_FILE = "file:///android_asset/rounded_graph.pb"
    private val LABEL_FILE = "file:///android_asset/retrained_labels.txt"

    private lateinit var classifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create classifier
        classifier = TensorFlowImageClassifier.create(assets, MODEL_FILE, LABEL_FILE, INPUT_SIZE,
                IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME)

        // Get bitmap for image to classify
        val bm = BitmapFactory.decodeResource(resources, R.raw.test_banana)

        // Crop it for inferencing
        var croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val screenOrientation = windowManager.defaultDisplay.rotation
        cropAndRescaleBitmap(bm, croppedBitmap, screenOrientation)

        // Get classification
        val recognitions = classifier.recognizeImage(croppedBitmap)

        // Print classifications
        recognitions.map { Timber.d(it.toString()) }
    }
}
