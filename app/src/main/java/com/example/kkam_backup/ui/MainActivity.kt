package com.example.kkam_backup.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kkam_backup.R
import com.example.kkam_backup.util.NotificationHelper
import com.google.android.material.appbar.MaterialToolbar
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject
import java.net.URISyntaxException
import android.media.MediaPlayer

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val REQ_NOTIF = 100
    }

    private lateinit var topWebView: WebView
    private lateinit var bottomWebView: WebView
    private var mSocketTop: Socket? = null
    private var mSocketBottom: Socket? = null

    private var THurl = "http://192.168.219.154:5000";
    private var SHurl = "http://192.168.219.180:5000";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        setContentView(R.layout.activity_main)

        // 툴바 세팅
        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }

        // Android 13+ 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_NOTIF
            )
        }

        initWebViews()
        initSocketIO()
    }

    /** 메뉴 인플레이트 */
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /** 메뉴 선택 처리 */
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    /** WebView + JSBridge 설정 */
    private fun initWebViews() {
        topWebView = findViewById<WebView>(R.id.streamTop).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(JSBridge { param ->
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Top param: $param",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    Log.d(TAG, "Top loaded $url")
                    view.evaluateJavascript(
                        """
                        (function(){
                          val e = document.querySelector('#probText');
                          AndroidBridge.call(e ? e.textContent : '');
                        })();
                        """.trimIndent(), null
                    )
                }
            }
            loadUrl(THurl)
        }

        bottomWebView = findViewById<WebView>(R.id.streamBottom).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(JSBridge { param ->
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Bottom param: $param",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    Log.d(TAG, "Bottom loaded $url")
                }
            }
            loadUrl(SHurl)
        }
    }

    /** Socket.IO 설정 (Top/Bottom 각각) */
    private fun initSocketIO() {
        val opts = IO.Options().apply {
            transports = arrayOf(WebSocket.NAME)
        }

        // Top 소켓
        mSocketTop = IO.socket(THurl, opts).apply {
            connect()
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Top Socket connected")
                runOnUiThread {
                    NotificationHelper.showHeadsUp(
                        this@MainActivity, 9998,
                        "Socket.IO(Top)", "서버 연결 성공!"
                    )
                }
            }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Top Socket connect error: ${args.getOrNull(0)}")
            }
            on("class_update") { args ->
                val cls = (args[0] as JSONObject).optInt("class", 0)
                if (cls != 0) {
                    runOnUiThread{
                        val player = MediaPlayer.create(this@MainActivity, R.raw.alert_sound)
                        player.setOnCompletionListener { mp -> mp.release() }
                        player.start()
                    }
                    val label = when (cls) {
                        1 -> "전도"
                        2 -> "파손"
                        3 -> "절도"
                        else -> "알 수 없음"
                    }
                    runOnUiThread {
                        NotificationHelper.showHeadsUp(
                            this@MainActivity, 4001,
                            "경성점 이상행동 감지", "$label 감지"
                        )
                    }
                }
            }
            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Top Socket disconnected")
            }
        }

        // Bottom 소켓
        mSocketBottom = IO.socket(SHurl, opts).apply {
            connect()
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Bottom Socket connected")
                runOnUiThread {
                    NotificationHelper.showHeadsUp(
                        this@MainActivity, 9999,
                        "Socket.IO(Bottom)", "서버 연결 성공!"
                    )
                }
            }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Bottom Socket connect error: ${args.getOrNull(0)}")
            }
            on("class_update") { args ->
                val cls = (args[0] as JSONObject).optInt("class", 0)
                if (cls != 0) {
                    runOnUiThread{
                        val player = MediaPlayer.create(this@MainActivity, R.raw.alert_sound)
                        player.setOnCompletionListener { mp -> mp.release() }
                        player.start()
                    }
                    val label = when (cls) {
                        1 -> "전도"
                        2 -> "파손"
                        3 -> "절도"
                        else -> "알 수 없음"
                    }
                    runOnUiThread {
                        NotificationHelper.showHeadsUp(
                            this@MainActivity, 4002,
                            "한양점 이상행동 감지", "$label 감지"
                        )
                    }
                }
            }
            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Bottom Socket disconnected")
            }
        }
    }

    /** 권한 결과 콜백 */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationHelper.showHeadsUp(
                this, 9999,
                "알림 권한 확인", "헤드업 알림 정상 작동!"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocketTop?.disconnect()?.off()
        mSocketBottom?.disconnect()?.off()
    }

    /** JS → Android 콜백 브릿지 */
    private class JSBridge(val cb: (String) -> Unit) {
        @JavascriptInterface
        fun call(params: String) = cb(params)
    }
}
