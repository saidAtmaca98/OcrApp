package com.example.ocrapptwo

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.core.graphics.get
import com.example.ocrapptwo.Global.HATA
import com.example.ocrapptwo.Global.IMAGE_BITMAP
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TextAnalyser(val coroutineScope: CoroutineScope) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var imageCapture: ImageCapture
    @SuppressLint("UnSafeOptInUsageError")
    override fun analyze(image: ImageProxy) {

       val inputImage =InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)




        coroutineScope.launch {
            val result= recognizer.process(inputImage).await()


            val blocks = result.textBlocks
            Log.d("ewrer",blocks.toString())
           // Global.TEXT.postValue(firstBlock.text)
            for (block in result.textBlocks) {
                val blockText = block.text
                val blockCornerPoints = block.cornerPoints
                val blockFrame = block.boundingBox
                val lineFirstText = block.lines[0].text
                val lineSecondText = block.lines[0].text
                val lineThirdText = block.lines[0].text




                val firstWord =lineFirstText.split(",").first()
                val lastWord=lineFirstText.split(",").last()

                if (firstWord == lastWord){
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



            Log.d("sddffds",result.text)
            println("The text is ${result.text}")
            image.close()
        }

        }


}