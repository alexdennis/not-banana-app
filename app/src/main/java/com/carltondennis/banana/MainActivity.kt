package com.carltondennis.banana

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create classifier
        classifier = TensorFlowImageClassifier.create(assets, MODEL_FILE, LABEL_FILE, INPUT_SIZE,
                IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME)

        if (hasPermission()) {
            showCamera()
        } else {
            requestPermission()
        }

        fab.setOnClickListener {
            showCamera()
        }
    }

    private fun showCamera() {
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (pictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pictureIntent, IMAGE_CAPTURE_REQUEST)
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) || shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(this@MainActivity, "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show()
            }
            requestPermissions(arrayOf(PERMISSION_CAMERA, PERMISSION_STORAGE), PERMISSIONS_REQUEST)
        }
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

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
            output_image.setImageBitmap(imageBitmap)
            inference(imageBitmap)
        }
    }

    private fun inference(bm: Bitmap) {
        // Crop it for inferencing
        var croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val screenOrientation = windowManager.defaultDisplay.rotation
        cropAndRescaleBitmap(bm, croppedBitmap, screenOrientation)

        // Get classification
        val recognitions = classifier.recognizeImage(croppedBitmap)

        // Print classifications
        var out = StringBuilder();
        recognitions.map {
            out.append(it.title)
            out.append(" - ")
            out.append(it.confidence)
            out.append("\n")
        }
        output_label.text = out.toString()
    }
}
