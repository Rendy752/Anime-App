package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * A data class to hold all relevant state from the iframe's video player.
 */
data class IframePlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L
)

/**
 * A JavaScript bridge to send data from the WebView's JS context to the app's Kotlin context.
 */
private class WebAppInterface(val onStateChange: (IframePlayerState) -> Unit) {
    @JavascriptInterface
    fun updatePlayerState(
        isPlaying: Boolean,
        isBuffering: Boolean,
        hasEnded: Boolean,
        position: Double,
        duration: Double,
        buffered: Double
    ) {
        onStateChange(
            IframePlayerState(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                hasEnded = hasEnded,
                positionMs = (position * 1000).toLong(),
                durationMs = (duration * 1000).toLong(),
                bufferedMs = (buffered * 1000).toLong()
            )
        )
    }
}

/**
 * A custom WebView that is completely non-interactive. It passes all touch events
 * up to its parent Composable, allowing gesture detection on the container.
 */
@SuppressLint("SetJavaScriptEnabled")
class NonClickableWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }
}

sealed class IframePlayerAction {
    data object Play : IframePlayerAction()
    data object Pause : IframePlayerAction()
    data class SeekTo(val positionMs: Long) : IframePlayerAction()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IframePlayer(
    modifier: Modifier = Modifier,
    fallbackUrl: String,
    isAutoPlay: Boolean,
    initialPositionMs: Long,
    onWebViewReady: (WebView) -> Unit,
    onStateChange: (IframePlayerState) -> Unit
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
                    overflow: hidden;
                    background-color: #000;
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
            <iframe id="videoPlayer" src="$fallbackUrl" frameborder="0" scrolling="no" allow="fullscreen; autoplay" allowfullscreen></iframe>
        </body>
        </html>
    """.trimIndent()


    val controlScript = """
        javascript:(function() {
            var iframe = document.getElementById('videoPlayer');
            if (!iframe) return;

            var interval = setInterval(function() {
                if (!iframe.contentWindow || !iframe.contentWindow.document) return;
                var video = iframe.contentWindow.document.querySelector('video');

                if (video) {
                    clearInterval(interval);

                    video.controls = false;

                    var style = iframe.contentWindow.document.createElement('style');
                    
                    style.innerHTML = `
                        video::-webkit-media-controls,
                        video::-webkit-media-controls-enclosure {
                            display: none !important;
                            -webkit-appearance: none;
                        }

                        div[class*="controls"],
                        div[class*="player-ui"],
                        div[class*="overlay"],
                        .vjs-control-bar,
                        .plyr__controls {
                            display: none !important;
                            opacity: 0 !important;
                            visibility: hidden !important;
                        }
                    `;
                    iframe.contentWindow.document.head.appendChild(style);

                    function getBufferedEnd() {
                        if (video.buffered.length > 0) {
                            return video.buffered.end(video.buffered.length - 1);
                        }
                        return 0;
                    }

                    function reportState() {
                        if (window.AndroidBridge && typeof window.AndroidBridge.updatePlayerState === 'function') {
                            var isBuffering = video.readyState < 4 && !video.paused;
                            window.AndroidBridge.updatePlayerState(
                                !video.paused,
                                isBuffering,
                                video.ended,
                                video.currentTime,
                                video.duration || 0,
                                getBufferedEnd()
                            );
                        }
                    }

                    ['timeupdate', 'play', 'pause', 'durationchange', 'ended', 'waiting', 'playing', 'progress'].forEach(event => {
                        video.addEventListener(event, reportState);
                    });
                    
                    reportState();
                }
            }, 100);
        })();
    """.trimIndent()

    AndroidView(
        factory = {
            NonClickableWebView(it).apply {
                onWebViewReady(this)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                addJavascriptInterface(WebAppInterface(onStateChange), "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        Log.d("IframePlayer", "Blocked navigation attempt to: ${request?.url}")
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.let { webView ->
                            webView.loadUrl(controlScript)
                            if (isAutoPlay) {
                                postDelayed({
                                    controlIframe(IframePlayerAction.SeekTo(initialPositionMs))
                                    controlIframe(IframePlayerAction.Play)
                                }, 500)
                            }
                        }
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

/**
 * Executes a JavaScript command on the video element inside the iframe.
 */
fun WebView.controlIframe(action: IframePlayerAction) {
    val command = when (action) {
        IframePlayerAction.Play -> "play()"
        IframePlayerAction.Pause -> "pause()"
        is IframePlayerAction.SeekTo -> "currentTime = ${action.positionMs / 1000.0}"
    }
    val script =
        "document.getElementById('videoPlayer').contentWindow.document.querySelector('video').${command};"
    evaluateJavascript(script, null)
}

/**
 * Captures the visible content of a WebView as a Bitmap using PixelCopy.
 * This is the recommended way to capture hardware-accelerated content.
 */
private suspend fun View.getBitmapWithPixelCopy(): Bitmap? {
    val window: Window = (context as? Activity)?.window ?: return null
    val bitmap = createBitmap(width, height)

    val locationOfViewInWindow = IntArray(2)
    getLocationInWindow(locationOfViewInWindow)
    val x = locationOfViewInWindow[0]
    val y = locationOfViewInWindow[1]
    val scope = Rect(x, y, x + width, y + height)

    return suspendCancellableCoroutine { continuation ->
        PixelCopy.request(
            window,
            scope,
            bitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    continuation.resume(bitmap) { cause, _, _ -> null }
                } else {
                    Log.e("WebViewUtils", "PixelCopy failed with result: $copyResult")
                    continuation.resume(null) { cause, _, _ -> null }
                }
            },
            Handler(Looper.getMainLooper())
        )
    }
}

/**
 * Captures the WebView content and encodes it as a Base64 String.
 */
suspend fun WebView.captureScreenshot(): String? = withContext(Dispatchers.IO) {
    try {
        val bitmap = withContext(Dispatchers.Main) {
            getBitmapWithPixelCopy()
        } ?: return@withContext null

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        bitmap.recycle()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: Exception) {
        Log.e("WebViewUtils", "Failed to capture WebView screenshot", e)
        null
    }
}