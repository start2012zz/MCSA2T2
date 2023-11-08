package io.github.sceneview.sample.arcursorplacement

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment


class ViewFragment: Fragment(R.layout.viewmodel)  {
    private lateinit var webView: WebView
    private lateinit var webAppInterface: ViewFragment.WebAppInterface
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.modelWebView)
        setupWebView()

        webAppInterface = ViewFragment.WebAppInterface(this, webView)
        webView.addJavascriptInterface(webAppInterface, "AndroidInterface")
        webView.loadUrl("https://appassets.androidplatform.net/assets/3.html")

    }


    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
//                sendDataToWebView()
            }

        }
    }

    class WebAppInterface(private val context: ViewFragment, private val webView: WebView) {
        @android.webkit.JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(context.requireContext(), toast, Toast.LENGTH_SHORT).show()
        }

    }

}