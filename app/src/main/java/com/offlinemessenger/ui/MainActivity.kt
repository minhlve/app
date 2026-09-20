package com.offlinemessenger.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.offlinemessenger.transport.MeshForegroundService

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(16)); setBackgroundColor(Color.rgb(247, 248, 243)) }
        root.addView(TextView(this).apply { text = "Offline\nMessenger"; textSize = 28f; setTextColor(Color.rgb(19, 47, 30)); setTypeface(null, 1) })
        root.addView(TextView(this).apply { text = "TIN NHẮN KHÔNG CẦN INTERNET"; textSize = 11f; setTextColor(Color.rgb(62, 91, 71)); setPadding(0, dp(6), 0, dp(20)) })
        status = TextView(this).apply { text = "● Chưa quét thiết bị lân cận"; textSize = 15f; setTextColor(Color.rgb(133, 82, 0)); setPadding(dp(16), dp(12), dp(16), dp(12)); setBackgroundColor(Color.rgb(255, 246, 218)) }
        root.addView(status, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply { text = "Chế độ an toàn"; textSize = 18f; setTypeface(null, 1); setPadding(0, dp(24), 0, dp(4)) })
        root.addView(TextView(this).apply { text = "Tin nhắn chỉ đi qua các điện thoại Offline Messenger ở gần nhau. Không có Internet, không có dữ liệu di động, không có máy chủ trung tâm."; textSize = 15f; setTextColor(Color.DKGRAY) })
        val recipient = EditText(this).apply { hint = "ID người nhận hoặc số điện thoại"; inputType = 1; setPadding(dp(12), dp(20), dp(12), 0) }
        root.addView(recipient, LinearLayout.LayoutParams(-1, dp(66)))
        val message = EditText(this).apply { hint = "Nhập tin nhắn"; minLines = 3; gravity = Gravity.TOP; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(Color.WHITE) }
        root.addView(message, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(10) })
        root.addView(Button(this).apply { text = "BẮT ĐẦU MẠNG MESH"; setOnClickListener { startMesh() } }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14) })
        root.addView(TextView(this).apply { text = "Giới hạn vật lý: ứng dụng không thể gửi tới người ở xa nếu giữa hai người không có thiết bị relay hoặc hạ tầng vô tuyến khác."; textSize = 12f; setTextColor(Color.GRAY); setPadding(0, dp(12), 0, 0) })
        setContentView(root)
    }
    private fun startMesh() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.NEARBY_WIFI_DEVICES) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) { requestPermissions(permissions, 5); return }
        startForegroundService(Intent(this, MeshForegroundService::class.java)); status.text = "● Đang quét thiết bị lân cận — chưa có tuyến"
        status.setTextColor(Color.rgb(12, 92, 56))
    }
}
