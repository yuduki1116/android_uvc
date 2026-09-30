package com.example.uvcmeet

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Base64
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    @Volatile private var latest: String = ""
    @Volatile private var cameraOn = false
    private var client: CameraClient? = null

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)

        web = findViewById(R.id.web)
        web.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = userAgentString.replace("; wv", "")
        }
        web.addJavascriptInterface(object {
            @JavascriptInterface fun frame(): String = latest
            @JavascriptInterface fun available(): Boolean = cameraOn
        }, "UVC")

        val js = assets.open("inject.js").bufferedReader().readText()
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, js, setOf("https://meet.google.com"))
        } else {
            web.webViewClient = object : WebViewClient() {
                override fun onPageStarted(v: WebView, u: String?, f: android.graphics.Bitmap?) { v.evaluateJavascript(js, null) }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(r: PermissionRequest) { runOnUiThread { r.grant(r.resources) } }
        }
        web.loadUrl("https://meet.google.com/")
        startCamera()
    }

    private fun startCamera() {
        val view = findViewById<AspectRatioSurfaceView>(R.id.hiddenView)
        client = CameraClient.newBuilder(this)
            .setEnableGLES(true)
            .setRawImage(true)
            .setCameraStrategy(CameraUvcStrategy(this))
            .setCameraRequest(
                CameraRequest.Builder()
                    .setPreviewWidth(640).setPreviewHeight(480)
                    .setRawPreviewData(true)
                    .create()
            )
            .build()
        client?.addPreviewDataCallBack(object : IPreviewDataCallBack {
            override fun onPreviewData(data: ByteArray?, width: Int, height: Int, format: IPreviewDataCallBack.DataFormat) {
                if (data == null || format != IPreviewDataCallBack.DataFormat.NV21) return
                val out = ByteArrayOutputStream()
                YuvImage(data, ImageFormat.NV21, width, height, null)
                    .compressToJpeg(Rect(0, 0, width, height), 70, out)
                latest = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                cameraOn = true
            }
        })
        client?.openCamera(view)
    }

    override fun onDestroy() { client?.closeCamera(); super.onDestroy() }
    @Deprecated("Deprecated in Java") override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
