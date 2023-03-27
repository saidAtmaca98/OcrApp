package com.example.ocrapptwo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.ocrapptwo.Global.HATA
import com.example.ocrapptwo.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
   // private lateinit var textAnalyser: TextAnalyser
    private lateinit var imageCapture: ImageCapture
    private var pickedImage: Uri? = null
    private lateinit var textRecognizer: TextRecognizer
    private var photoTaken = false
    private var photoUri : Uri? =null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater,null,false)
        setContentView(binding.root)
        cameraExecutor= Executors.newSingleThreadExecutor()
       // textAnalyser = TextAnalyser(CoroutineScope(Dispatchers.Default))
        requestPermission()



        textRecognizer =TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        binding.cameraButton.setOnClickListener {
            openCamera()
        }
        binding.galleryButton.setOnClickListener{
            openGalery()
        }

        binding.analyzeButton.setOnClickListener {
            if (pickedImage == null ){
                Toast.makeText(this,"Pick A Photo",Toast.LENGTH_SHORT)
            }else{
                analysePhoto()
            }
        }




        Global.TEXT.observe(this, Observer {

            binding.textView.text = it

            if (photoTaken){
                binding.imageView.setImageURI(photoUri)
            }
        })

        HATA.observe(this, Observer {
            if (it){
                binding.warningImage.setImageResource(R.drawable.baseline_warning_24)
            }else{
                binding.warningImage.setImageResource(R.drawable.baseline_beenhere_24)
            }

        })
    }


    private fun requestPermission() {
        requestCameraPermissionIfMissing{ granted ->
            if (granted){
                startCamera()
             //   onClick()
            }else{
                Toast.makeText(this,"Please Allow Permission", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun requestCameraPermissionIfMissing(onResult: ((Boolean) -> Unit)) {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED )
            onResult(true)
        else
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                onResult(it)
            }.launch(android.Manifest.permission.CAMERA)
    }


    private fun startCamera(){
        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        processCameraProvider.addListener({
            try {
                val cameraProvider = processCameraProvider.get()
                val previewUseCase = buildPreviewUseCase()
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val imageAnalysisUseCase = buildImageAnalysisUseCase()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    imageCapture,
                    imageAnalysisUseCase)
            }  catch (e : Exception){
                Toast.makeText(this, "Error Starting Camera", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }



    private fun openCamera() {

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera")
        pickedImage = this.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )!!
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pickedImage)
        resultCameraLauncher.launch(intent)

    }

    private var resultCameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                //pickedImage = data?.extras?.get("data") as Uri?

                pickedImage?.let {
                    binding.imageView.setImageURI(it)




                }


            }
        }

    private fun openGalery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultGalleryLauncher.launch(galleryIntent)
    }



    private var resultGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                pickedImage = data?.data


                pickedImage?.let {
                    binding.imageView.setImageURI(it)


                }
            }
        }



    val REQUEST_IMAGE_CAPTURE = 1

    private fun analysePhoto(){

        try {
            val inputImage = InputImage.fromFilePath(this, pickedImage!!)

            val result= textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->

                    val recognizedText = text.text

                    //Global.TEXT.postValue(recognizedText)

                    val blocks = text.textBlocks

                }
                .addOnFailureListener {e ->

                    e.printStackTrace()

                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }




    private fun buildImageAnalysisUseCase(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also { it.setAnalyzer(cameraExecutor, object : ImageAnalysis.Analyzer{
                @SuppressLint("UnSafeOptInUsageError")
                override fun analyze(image: ImageProxy) {
                    val inputImage =InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)


                    val currentTime = System.currentTimeMillis()

                    val contentValues = ContentValues()

                    val outputDirectory: String by lazy {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            "${Environment.DIRECTORY_DCIM}/CameraXDemo/"
                        } else {
                            "${this@MainActivity.getExternalFilesDir(Environment.DIRECTORY_DCIM)}/CameraXDemo/"
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 29) {
                        contentValues.apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis())
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, outputDirectory)
                        }

                        val contentResolver = this@MainActivity.contentResolver

                        // Create the output uri
                        val contentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                        ImageCapture.OutputFileOptions.Builder(contentResolver, contentUri, contentValues)

                    } else {
                        File(outputDirectory).mkdirs()
                        val file = File(outputDirectory, "${System.currentTimeMillis()}a.jpg")

                        ImageCapture.OutputFileOptions.Builder(file)
                    }


                        val result= textRecognizer.process(inputImage)
                            .addOnSuccessListener {

                                val blocks = it.textBlocks
                                Log.d("ewrer",blocks.toString())
                                // Global.TEXT.postValue(firstBlock.text)
                                for (block in it.textBlocks) {
                                    val blockText = block.text
                                    val blockCornerPoints = block.cornerPoints
                                    val blockFrame = block.boundingBox
                                    val lineFirstText = block.lines[0].text
                                    val lineSecondText = block.lines[0].text
                                    val lineThirdText = block.lines[0].text




                                    val firstWord =lineFirstText.split(",").first()
                                    val lastWord=lineFirstText.split(",").last()

                                    if (firstWord == lastWord){

                                        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
                                        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onError(error: ImageCaptureException)
                                                {
                                                  Log.d("sadsdsdas","sdfsdfsdf")
                                                }
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                   photoUri =outputFileResults.savedUri
                                                    Log.d("uyuyuyuyuy","sdfsdfsdf")

                                                }
                                            })



                                        Global.TEXT.postValue(lineFirstText)
                                        HATA.postValue(false)


                                    }else{
                                        Global.TEXT.postValue("")
                                        HATA.postValue(true)
                                    }



                                    //val blockFrame = block.boundingBox

                                    Log.d("uyuyu1",blockText.toString())
                                    Log.d("uyuyu2",blockCornerPoints.toString())
                                    //Log.d("uyuyu3",blockFrame.toString())
                                }



                                Log.d("sddffds",it.text)
                                println("The text is ${it.text}")
                                image.close()

                            }
                            .addOnFailureListener{

                            }




                }

            })
            }

    }

    private fun buildPreviewUseCase(): Preview {
        return Preview.Builder().build().also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }
    }



}