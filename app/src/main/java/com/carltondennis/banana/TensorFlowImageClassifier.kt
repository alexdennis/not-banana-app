package com.carltondennis.banana

import android.content.res.Resources
import android.graphics.Bitmap
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Created by alex on 8/26/17.
 */
class TensorFlowImageClassifier: Classifier {

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

    private var inferenceInterface: TensorFlowInferenceInterface? = null

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
    fun create(
            resources: Resources,
            modelResourceId: Int,
            labelsResourceId: Int,
            inputSize: Int,
            imageMean: Int,
            imageStd: Float,
            inputName: String,
            outputName: String): Classifier {

        val c = TensorFlowImageClassifier()
        c.inputName = inputName
        c.outputName = outputName

        var br:BufferedReader?;
        try {
            br = BufferedReader(InputStreamReader(resources.openRawResource(labelsResourceId)))
            var line:String;
            do {
                line = br?.readLine()
                if (line != null) {
                    c.labels.add(line)
                }
            } while(line != null)
        } catch (e:IOException) {
            throw RuntimeException("Problem reading label file!", e)
        }

//        c.inferenceInterface = TensorFlowInferenceInterface()

        return c
    }


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
}