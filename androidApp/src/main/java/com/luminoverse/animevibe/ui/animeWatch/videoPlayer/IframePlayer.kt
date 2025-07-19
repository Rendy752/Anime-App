package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IframePlayer(
    modifier: Modifier = Modifier,
    fallbackUrl: String
) {
    Log.d("IframePlayer", "Loading iframe with fallbackUrl: $fallbackUrl")

    val iframeHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
            <style>
                html, body { 
                    margin: 0; 
                    padding: 0; 
                    height: 100%; 
                    width: 100%; 
                    background-color: black; 
                    overflow: hidden; 
                }
                iframe { 
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%; 
                    height: 100%; 
                    border: none; 
                }
            </style>
        </head>
        <body>
            <iframe 
                src="$fallbackUrl" 
                frameborder="0" 
                scrolling="no" 
                allow="fullscreen; autoplay" 
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36"
                webChromeClient = WebChromeClient()
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.mediaPlaybackRequiresUserGesture = false
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                webViewClient = object : WebViewClient() {
                    // FIX: This method is updated to block all navigation attempts.
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString()
                        Log.d("IframePlayer", "Blocked navigation attempt to: $url")
                        // By returning true, we tell the WebView that we have handled the
                        // URL ourselves, so it should do nothing. This effectively blocks all redirects.
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val script = """
                            javascript:(function() {
                                var adOverlays = document.querySelectorAll('[class*="ads"], [id*="ads"], [class*="overlay"]');
                                adOverlays.forEach(function(el) { el.style.display = 'none'; });
                                document.querySelectorAll('body > div:not(iframe)').forEach(function(el) {
                                    if (el.querySelector('iframe') === null) { el.style.display = 'none'; }
                                });
                            })();
                        """.trimIndent()
                        view?.loadUrl(script)
                    }
                }

                loadDataWithBaseURL("https://megaplay.buzz", iframeHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val lastUrlLoaded = webView.tag as? String
            if (lastUrlLoaded != fallbackUrl) {
                webView.tag = fallbackUrl
                webView.loadDataWithBaseURL(
                    "https://megaplay.buzz",
                    iframeHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = modifier
    )
}