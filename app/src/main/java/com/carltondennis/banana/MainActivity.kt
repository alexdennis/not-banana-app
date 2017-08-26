package com.carltondennis.banana

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

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

        classifier = TensorFlowImageClassifier.create(assets, MODEL_FILE, LABEL_FILE, INPUT_SIZE,
                IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME)
    }
}
