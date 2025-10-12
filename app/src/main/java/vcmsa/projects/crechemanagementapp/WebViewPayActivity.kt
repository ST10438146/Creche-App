package vcmsa.projects.crechemanagementapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * WebViewPayActivity
 * -------------------
 * Displays PayFast payment form and detects when the payment
 * is successful or canceled, returning the result to PaymentsFragment.
 */
class WebViewPayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HTML_FORM = "html_form"
        const val RESULT_SUCCESS = Activity.RESULT_OK
        const val RESULT_FAILURE = Activity.RESULT_CANCELED
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_pay)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        val htmlForm = intent.getStringExtra(EXTRA_HTML_FORM)
        if (htmlForm.isNullOrEmpty()) {
            Toast.makeText(this, "Payment form missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                when {
                    url.contains("payment/success", true) -> {
                        Toast.makeText(applicationContext, "Payment successful!", Toast.LENGTH_SHORT).show()
                        val resultIntent = Intent()
                        resultIntent.putExtra("payment_status", "success")
                        setResult(RESULT_SUCCESS, resultIntent)
                        finish()
                        return true
                    }

                    url.contains("payment/cancel", true) -> {
                        Toast.makeText(applicationContext, "Payment cancelled.", Toast.LENGTH_SHORT).show()
                        val resultIntent = Intent()
                        resultIntent.putExtra("payment_status", "cancel")
                        setResult(RESULT_FAILURE, resultIntent)
                        finish()
                        return true
                    }

                    else -> return false
                }
            }
        }

        // Load and auto-submit PayFast form
        webView.loadDataWithBaseURL(null, htmlForm, "text/html", "utf-8", null)
    }

    override fun onBackPressed() {
        setResult(RESULT_FAILURE)
        super.onBackPressed()
    }
}
