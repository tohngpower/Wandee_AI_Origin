package com.psn.myai

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins

class WebSiteView : ComponentActivity() {
    private lateinit var contentContainer: LinearLayout
    private lateinit var webView: WebView
    private lateinit var closeButton: Button
    private val systemDatabase = SystemDatabase(this)
    private var htmlText:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApp.setAppTheme(this)
        enableEdgeToEdge()
        val i = intent
        htmlText = i.getStringExtra(MainActivity.HTML_TEXT)
        setContentView(R.layout.activity_websiteview)

        contentContainer = findViewById(R.id.web_view)
        webView = findViewById(R.id.webView1)
        closeButton = findViewById(R.id.close_btn)

        ViewCompat.setOnApplyWindowInsetsListener(contentContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply insets as padding to the view:
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.updateMargins(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        closeButton.setOnClickListener {
            finish()
        }

        if(!htmlText.isNullOrEmpty()) {
            webView.loadDataWithBaseURL(null, htmlText!!, "text/html", "UTF-8", null)
        }
    }

    override fun onDestroy() {
        systemDatabase.close()
        super.onDestroy()
    }
}