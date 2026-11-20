package com.ghostx.panel

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 45, 28, 25)
            setBackgroundColor(Color.rgb(12, 12, 25))
        }

        val title = TextView(this).apply {
            text = "GHOST X ULTIMATE"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.CYAN)
        }

        val subtitle = TextView(this).apply {
            text = "\nROBLOX COMPANION PANEL\n\nاطلاعات عمومی بازی‌ها و پروفایل‌های Roblox"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val button = Button(this).apply {
            text = "فعال‌سازی پنل شناور"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(this@MainActivity, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent)
                    else startService(intent)
                    Toast.makeText(this@MainActivity, "GHOST X فعال شد", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
        }

        val stop = Button(this).apply {
            text = "خاموش کردن پنل"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(button)
        root.addView(stop)
        setContentView(root)
    }
}
