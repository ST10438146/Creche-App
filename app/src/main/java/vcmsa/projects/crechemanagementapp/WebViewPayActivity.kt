package vcmsa.projects.crechemanagementapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WebViewPayActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_pay)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        val htmlContent = intent.getStringExtra("htmlContent")
        val paymentUrl = intent.getStringExtra("PAYFAST_URL")

        // Configure WebView client (works across all SDKs)
        webView.webViewClient = object : WebViewClient() {

            // For Android 21+
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: ""
                return handleUrl(url)
            }

            // For Android <21
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUrl(url ?: "")
            }

            private fun handleUrl(url: String): Boolean {
                when {
                    url.contains("cancel", ignoreCase = true) -> {
                        Toast.makeText(this@WebViewPayActivity, "Payment cancelled.", Toast.LENGTH_SHORT).show()
                        finish()
                        return true
                    }

                    url.contains("return", ignoreCase = true) ||
                            url.contains("success", ignoreCase = true) -> {
                        Toast.makeText(this@WebViewPayActivity, "Payment completed! Thank you.", Toast.LENGTH_SHORT).show()
                        // Keep page open; optionally trigger backend verification here.
                        return false
                    }

                    else -> return false // Continue loading normally
                }
            }
        }

        // ✅ Priority 1: If backend sends full PayFast URL (redirect)
        if (!paymentUrl.isNullOrEmpty()) {
            webView.loadUrl(paymentUrl)
        }
        // ✅ Fallback: If backend sends an HTML auto-post form instead
        else if (!htmlContent.isNullOrEmpty()) {
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
        // ✅ Neither provided
        else {
            Toast.makeText(this, "No payment information found.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
