package com.ghostx.panel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 55, 32, 32)
            setBackgroundColor(0xFF10101C.toInt())
        }

        val title = TextView(this).apply {
            text = "ROBLOX GHOST X"
            textSize = 27f
            setTextColor(0xFF00E5FF.toInt())
        }

        val description = TextView(this).apply {
            text = "\nپنل شناور همراه Roblox\n\nبرای فعال‌سازی، اجازه نمایش روی برنامه‌های دیگر را بده."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
        }

        val start = Button(this).apply {
            text = "فعال‌سازی پنل شناور"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(this@MainActivity, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    Toast.makeText(this@MainActivity, "پنل فعال شد", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
        }

        root.addView(title)
        root.addView(description)
        root.addView(start)
        setContentView(root)
    }
}
