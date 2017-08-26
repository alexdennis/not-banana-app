package com.carltondennis.banana

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Created by alex on 8/26/17.
 */
class TensorFlowImageClassifier : Classifier {

    // Only return this many results with at least this confidence.
    private val MAX_RESULTS = 3
    private val THRESHOLD = 0.1f

    // Config values.
    private var inputName: String? = null
    private var outputName: String? = null
    private var inputSize: Int = 0
    private var imageMean: Int = 0
    private var imageStd: Float = 0.0F

    // Pre-allocated buffers.
    private val labels = Vector<String>()
    private var intValues: IntArray? = null
    private var floatValues: FloatArray? = null
    private var outputs: FloatArray? = null
    private var outputNames: Array<String>? = null

    private var logStats = false

    lateinit private var inferenceInterface: TensorFlowInferenceInterface

    override fun recognizeImage(bitmap: Bitmap): List<Classifier.Recognition> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enableStatLogging(debug: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStatString(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        /**
         * Initializes a native TensorFlow session for classifying images.
         *
         * @param resources The resources object to load tiems
         * @param modelResourceId The resource id for the model file
         * @param labelsResourceId The resource id for the label file
         * @param inputSize The input size. A square image of inputSize x inputSize is assumed.
         * @param imageMean The assumed mean of the image values.
         * @param imageStd The assumed std of the image values.
         * @param inputName The label of the image input node.
         * @param outputName The label of the output node.
         * @throws IOException
         */
        @JvmStatic
        fun create(assetManager: AssetManager, modelFilename: String,
                   labelsFilename: String,
                   inputSize: Int,
                   imageMean: Int,
                   imageStd: Float,
                   inputName: String,
                   outputName: String): Classifier {

            val c = TensorFlowImageClassifier()
            c.inputName = inputName
            c.outputName = outputName

            // Read the label names into memory.
            var actualFilename = labelsFilename.split("file:///android_asset/")[1]
            Timber.i("Reading labels from: " + actualFilename);
            var br: BufferedReader?
            try {
                br = BufferedReader(InputStreamReader(assetManager.open(actualFilename)))
                var line: String?
                do {
                    line = br?.readLine()
                    if (line != null) {
                        c.labels.add(line)
                    }
                } while (line != null)
            } catch (e: IOException) {
                throw RuntimeException("Problem reading label file!", e)
            }

            c.inferenceInterface = TensorFlowInferenceInterface(assetManager, modelFilename)

            // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
            val operation = c.inferenceInterface.graphOperation(outputName)
            var numClasses = operation.output(0).shape().size(1).toInt()
            Timber.i("Read " + c.labels.size + " labels, output layer size is " + numClasses)

            return c
        }
    }
}

