package com.example.kkam_backup.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val REQ_NOTIF = 100
    }

    private lateinit var topWebView: WebView
    private lateinit var bottomWebView: WebView
    private lateinit var mSocketTop: Socket
    private lateinit var mSocketBottom: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        setContentView(R.layout.activity_main)

        // 툴바
        findViewById<MaterialToolbar>(R.id.toolbar).also {
            setSupportActionBar(it)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }

        // Android13+ 알림 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_NOTIF
            )
        }

        initWebViews()
        initSocketIO()
    }

    /** ── 메뉴 인플레이트 & 선택 처리 ───────────────── */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initWebViews() {
        topWebView = findViewById<WebView>(R.id.streamTop).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(JSBridge { p ->
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Top param: $p",
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
                          const e = document.querySelector('#probText');
                          AndroidBridge.call(e ? e.textContent : '');
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
            loadUrl("http://192.168.219.180:5000")
        }

        bottomWebView = findViewById<WebView>(R.id.streamBottom).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(JSBridge { p ->
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Bottom param: $p",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    Log.d(TAG, "Bottom loaded $url")
                }
            }
            loadUrl("http://192.168.219.56:5000")
        }
    }

    private fun initSocketIO() {
        val opts = IO.Options().apply {
            transports = arrayOf(WebSocket.NAME)
        }

        // ① Top 카메라 서버용 소켓
        mSocketTop = IO.socket("http://192.168.219.180:5000", opts)
        mSocketTop.connect()
        mSocketTop.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Top Socket connected")
            runOnUiThread {
                NotificationHelper.showHeadsUp(
                    this, 9998, "Socket.IO(Top)", "서버 연결 성공!"
                )
            }
        }
        mSocketTop.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Top Socket connect error: ${args.getOrNull(0)}")
        }
        mSocketTop.on("class_update") { args ->
            val cls = (args[0] as JSONObject).getInt("class")
            if (cls != 0) runOnUiThread {
                NotificationHelper.showHeadsUp(
                    this, 4001,
                    "CAM1 이상행동 감지", "새 클래스: $cls"
                )
            }
        }
        mSocketTop.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Top Socket disconnected")
        }

        // ② Bottom 카메라 서버용 소켓
        mSocketBottom = IO.socket("http://192.168.219.56:5000", opts)
        mSocketBottom.connect()
        mSocketBottom.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Bottom Socket connected")
            runOnUiThread {
                NotificationHelper.showHeadsUp(
                    this, 9999, "Socket.IO(Bottom)", "서버 연결 성공!"
                )
            }
        }
        mSocketBottom.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Bottom Socket connect error: ${args.getOrNull(0)}")
        }
        mSocketBottom.on("class_update") { args ->
            val cls = (args[0] as JSONObject).getInt("class")
            if (cls != 0) runOnUiThread {
                NotificationHelper.showHeadsUp(
                    this, 4002,
                    "CAM2 이상행동 감지", "새 클래스: $cls"
                )
            }
        }
        mSocketBottom.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Bottom Socket disconnected")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            NotificationHelper.showHeadsUp(
                this, 9999,
                "알림 권한 확인", "헤드업 알림 정상 작동!"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mSocketTop.isInitialized) {
            mSocketTop.disconnect()
            mSocketTop.off()
        }
        if (::mSocketBottom.isInitialized) {
            mSocketBottom.disconnect()
            mSocketBottom.off()
        }
    }

    /** JS → Android 콜백 브릿지 */
    private class JSBridge(val cb: (String) -> Unit) {
        @JavascriptInterface
        fun call(params: String) = cb(params)
    }
}
