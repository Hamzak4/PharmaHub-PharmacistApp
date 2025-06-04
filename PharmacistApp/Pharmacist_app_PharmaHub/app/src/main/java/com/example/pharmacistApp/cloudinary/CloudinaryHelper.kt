package com.example.pharmacistApp.cloudinary



import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.util.HashMap

object CloudinaryHelper {
    fun initialize(context: Context) {
        val config = HashMap<String, String>()
        config["cloud_name"] = "dkidsf3ud" // Replace with your Cloudinary cloud name
        config["api_key"] = "566781526617128" // Replace with your Cloudinary API key
        config["api_secret"] = "2GQUUt4d3Qlu7h0ROGwmwtRPkrA" // Replace with your API secret
        MediaManager.init(context, config)
    }

    fun uploadImage(
        uri: Uri,

        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        MediaManager.get().upload(uri)
            .option("folder", "products")

            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                    val url = resultData["secure_url"].toString()
                    onSuccess(url)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}