package com.carltondennis.banana

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.Trace
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Created by alex on 8/26/17.
 *
 * Adapted from https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/TensorFlowImageClassifier.java
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
    private lateinit var intValues: IntArray
    private lateinit var floatValues: FloatArray
    private lateinit var outputs: FloatArray
    private lateinit var outputNames: Array<String>

    private var logStats = false

    private lateinit var inferenceInterface: TensorFlowInferenceInterface

    override fun recognizeImage(bitmap: Bitmap): List<Classifier.Recognition> {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage")

        Trace.beginSection("preprocessBitmap")
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in intValues.indices) {
            val value = intValues[i]
            floatValues[i * 3 + 0] = ((value shr 16 and 0xFF) - imageMean) / imageStd
            floatValues[i * 3 + 1] = ((value shr 8 and 0xFF) - imageMean) / imageStd
            floatValues[i * 3 + 2] = ((value and 0xFF) - imageMean) / imageStd
        }
        Trace.endSection()

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed")
        inferenceInterface.feed(inputName, floatValues, 1L, inputSize.toLong(), inputSize.toLong(), 3L)
        Trace.endSection()

        // Run the inference call.
        Trace.beginSection("run")
        inferenceInterface.run(outputNames, logStats)
        Trace.endSection()

        // Copy the output Tensor back into the output array.
        Trace.beginSection("fetch")
        inferenceInterface.fetch(outputName, outputs)
        Trace.endSection()

        val pq: PriorityQueue<Classifier.Recognition> = PriorityQueue(3)
            { lhs, rhs -> if (rhs.confidence > lhs.confidence) 1 else if (rhs.confidence < lhs.confidence) -1 else 0 }

        for (i in outputs.indices) {
            if (outputs[i] > THRESHOLD) {
                pq.add(Classifier.Recognition(
                        i.toString(),
                        if (labels.size > i) labels.get(i) else "unknown",
                        outputs[i],
                        null
                ))
            }
        }
        val recognitions: ArrayList<Classifier.Recognition> = arrayListOf()
        val recognitionsSize = Math.min(pq.size, MAX_RESULTS)
        for (i in 1..recognitionsSize) {
            recognitions.add(pq.poll())
        }
        Trace.endSection()
        return recognitions
    }

    override fun enableStatLogging(debug: Boolean) {
        logStats = debug
    }

    override fun getStatString(): String {
        return inferenceInterface.statString
    }

    override fun close() {
        inferenceInterface.close()
    }

    companion object {
        /**
         * Initializes a native TensorFlow session for classifying images.
         *
         * @param assetManager The Asset Manager for loading assets.
         * @param modelFilename The name of the model file
         * @param labelsFilename The name of the label file
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
            val actualFilename = labelsFilename.split("file:///android_asset/")[1]
            Timber.i("Reading labels from: %s", actualFilename)
            try {
                val br = BufferedReader(InputStreamReader(assetManager.open(actualFilename)))
                var line: String?
                do {
                    line = br.readLine()
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
            val numClasses = operation.output<Int>(0).shape().size(1).toInt()
            Timber.i("Read %d labels, output layer size is %d", c.labels.size, numClasses)

            // Ideally, inputSize could have been retrieved from the shape of the input operation. Alas,
            // the placeholder node for input in the graphdef typically used does not specify a shape, so it
            // must be passed in as a parameter.
            c.inputSize = inputSize
            c.imageMean = imageMean
            c.imageStd = imageStd

            // Pre-allocate buffers.
            c.outputNames = arrayOf(outputName)
            c.intValues = IntArray(inputSize * inputSize)
            c.floatValues = FloatArray(inputSize * inputSize * 3)
            c.outputs = FloatArray(numClasses)

            return c
        }
    }
}

