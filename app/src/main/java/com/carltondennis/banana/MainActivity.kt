package com.carltondennis.banana

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST = 1
    private val IMAGE_CAPTURE_REQUEST = 2

    private val PERMISSION_CAMERA = Manifest.permission.CAMERA
    private val PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE

    private val INPUT_SIZE = 299
    private val IMAGE_MEAN = 128
    private val IMAGE_STD = 128.0f
    private val INPUT_NAME = "Mul:0"
    private val OUTPUT_NAME = "final_result"

    private val MODEL_FILE = "file:///android_asset/rounded_graph.pb"
    private val LABEL_FILE = "file:///android_asset/retrained_labels.txt"

    private lateinit var classifier: Classifier
    private var disposible: Disposable? = null
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create classifier
        classifier = TensorFlowImageClassifier.create(assets, MODEL_FILE, LABEL_FILE, INPUT_SIZE,
                IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME)

        textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.ERROR) textToSpeech.language = Locale.UK
        }

        if (hasPermission()) {
            showCamera()
        } else {
            requestPermission()
        }

        fab.setOnClickListener {
            showCamera()
        }
    }

    override fun onDestroy() {
        // RxJava stuff
        disposible?.dispose()

        classifier.close()
        textToSpeech.shutdown()

        super.onDestroy()
    }

    private fun showCamera() {
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (pictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pictureIntent, IMAGE_CAPTURE_REQUEST)
        }
    }

    private fun requestPermission() {
        if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) || shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
            Toast.makeText(this@MainActivity, getString(R.string.permision_rationale), Toast.LENGTH_LONG).show()
        }
        requestPermissions(arrayOf(PERMISSION_CAMERA, PERMISSION_STORAGE), PERMISSIONS_REQUEST)

    }

    private fun hasPermission(): Boolean =
            checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    showCamera()
                } else {
                    requestPermission()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == IMAGE_CAPTURE_REQUEST && resultCode == Activity.RESULT_OK) {
            val extras = data.extras
            val imageBitmap = extras.get("data") as Bitmap

            // Display image to the user
            // output_image.setImageBitmap(imageBitmap)

            // Do inferencing
            inference(imageBitmap)
        }
    }

    private fun inferencing() {
        outputLabel.text = getString(R.string.loading_text)
        outputImage.setImageBitmap(null)
    }

    private fun display(recognitions: List<Classifier.Recognition>) {
        // Build string from results of the classifier
        val out = StringBuilder().apply {
            recognitions.map {
                append(getString(R.string.guess, (it.confidence * 100), it.title))
            }
        }
        outputLabel.text = out.toString()

        // Speak out the label
        val speech = recognitions.first().title
        textToSpeech.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "results")

        // Show an appropriate image
        when (recognitions.first().title) {
            "banana" -> outputImage.setImageResource(R.raw.banana)
            else -> outputImage.setImageResource(R.raw.not_banana)
        }
    }

    private fun inference(bm: Bitmap) {

        // Crop bitmap to fit input tensor
        val screenOrientation = windowManager.defaultDisplay.rotation
        val croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        cropAndRescaleBitmap(bm, croppedBitmap, screenOrientation)

        // Show message in UI to assure the use that things are happening
        inferencing()

        disposible = Single.just(croppedBitmap)
                // run the classifier on the image in the background
                .map { x -> classifier.recognizeImage(x) }
                .subscribeOn(Schedulers.computation())
                // Display classifications on the UI thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { x -> display(x) }

    }
}
