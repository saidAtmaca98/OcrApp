package com.example.ocrapptwo

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData

object Global {


    var TEXT : MutableLiveData<String> = MutableLiveData()
    var HATA : MutableLiveData<Boolean> = MutableLiveData(true)

    var IMAGE_BITMAP : Bitmap? = null
}