import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import io.github.sceneview.sample.arcursorplacement.R

class ViewFragment : Fragment(R.layout.viewmodel) {
    private lateinit var webView: WebView
    private lateinit var webAppInterface: WebAppInterface
    private var modelId: String? = null

    companion object {
        private const val EXTRA_ID = "EXTRA_ID"

        // Static factory method to create new instances of ViewFragment
        fun newInstance(receivedId: String): ViewFragment {
            val fragment = ViewFragment()
            val args = Bundle()
            args.putString(EXTRA_ID, receivedId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.e("ViewFragment", "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.modelWebView)
        modelId = arguments?.getString(EXTRA_ID) // Retrieve the ID passed to this fragment
        Log.e("ViewFragment", "modelId: $modelId")

        setupWebView()

        // Initialize the WebAppInterface and set it to the WebView
        webAppInterface = WebAppInterface(this, webView)
        webView.addJavascriptInterface(webAppInterface, "AndroidInterface")

        // Load the URL of the HTML page that contains the Three.js scene
        webView.loadUrl("file:///android_asset/modelView.html")
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Now the page is loaded, and we can send the model ID if it's available
                modelId?.let { id ->
                    webView.evaluateJavascript("javascript:loadModelFromId('$id')", null)
                }
            }
        }
    }

    class WebAppInterface(private val fragment: ViewFragment, private val webView: WebView) {

        @JavascriptInterface
        fun modelId(id: String) {
            // This will call the JavaScript function loadModelFromId with the provided ID
            webView.post {
                webView.evaluateJavascript("javascript:loadModelFromId('$id')", null)
            }
        }
    }
}
