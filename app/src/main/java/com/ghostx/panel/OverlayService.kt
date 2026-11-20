package com.ghostx.panel

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private var panel: View? = null

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotification()
        showBubble()
    }

    private fun createNotification() {
        val channelId = "ghost_x_overlay"

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "GHOST X Floating Panel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("ROBLOX GHOST X")
            .setContentText("پنل شناور فعال است")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    private fun params(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 35
            y = 250
        }
    }

    private fun showBubble() {
        if (bubble != null) return

        val button = TextView(this).apply {
            text = "GX"
            textSize = 19f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(105, 35, 190))
            elevation = 12f
        }

        val p = params(65, 65)
        bubble = button
        windowManager.addView(button, p)

        button.setOnClickListener {
            if (panel == null) showPanel() else closePanel()
        }

        button.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x
                        startY = p.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                        p.x = startX + dx
                        p.y = startY + dy
                        windowManager.updateViewLayout(button, p)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) button.performClick()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun showPanel() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            setBackgroundColor(Color.rgb(20, 18, 38))
            elevation = 18f
        }

        val header = TextView(this).apply {
            text = "GHOST X  •  ROBLOX"
            textSize = 19f
            setTextColor(Color.CYAN)
            setPadding(0, 0, 0, 18)
        }

        val info = TextView(this).apply {
            text = "پنل شناور فعال است\\n\\nاطلاعات عمومی بازی در مراحل بعد اضافه می‌شود.\\n\\nاین برنامه از اطلاعات عمومی و مجاز Roblox استفاده می‌کند."
            textSize = 14f
            setTextColor(Color.WHITE)
        }

        val close = Button(this).apply {
            text = "بستن پنل"
            setOnClickListener { closePanel() }
        }

        box.addView(header)
        box.addView(info)
        box.addView(close)

        val p = params(290, WindowManager.LayoutParams.WRAP_CONTENT)
        p.x = 85
        p.y = 300

        panel = box
        windowManager.addView(box, p)
    }

    private fun closePanel() {
        panel?.let {
            windowManager.removeView(it)
            panel = null
        }
    }

    override fun onDestroy() {
        closePanel()
        bubble?.let {
            windowManager.removeView(it)
            bubble = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
