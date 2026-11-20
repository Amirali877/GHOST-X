package com.ghostx.panel

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.concurrent.Executors

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var bubble: TextView? = null
    private var panel: View? = null
    private var result: TextView? = null
    private var clock: TextView? = null
    private var placeInput: EditText? = null
    private var searchInput: EditText? = null

    private val prefs by lazy {
        getSharedPreferences("ghost_data", Context.MODE_PRIVATE)
    }

    private var timerStart = 0L
    private var timerRunning = false

    private val clockTask = object : Runnable {
        override fun run() {
            clock?.text = java.text.SimpleDateFormat(
                "HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date()) +
                if (timerRunning) {
                    "\nTimer: ${((System.currentTimeMillis() - timerStart) / 1000)} sec"
                } else "\nTimer: متوقف"

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        notification()
        showBubble()
        handler.post(clockTask)
    }

    private fun notification() {
        val id = "ghost_ultimate"

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                id,
                "GHOST X",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val n = Notification.Builder(this, id)
            .setContentTitle("ROBLOX GHOST X")
            .setContentText("پنل شناور فعال است")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(7, n)
    }

    private fun bg(color: Int, radius: Float = 18f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(2, Color.rgb(0, 210, 255))
        }
    }

    private fun params(w: Int, h: Int, focus: Boolean = false): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            w,
            h,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            if (focus) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 25
            y = 250
        }
    }

    private fun showBubble() {
        if (bubble != null) return

        val b = TextView(this).apply {
            text = "GX"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = bg(Color.rgb(90, 25, 170), 60f)
            elevation = 15f
        }

        val p = params(65, 65)
        bubble = b
        wm.addView(b, p)

        b.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0
            var sy = 0
            var tx = 0f
            var ty = 0f
            var moved = false

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        sx = p.x
                        sy = p.y
                        tx = e.rawX
                        ty = e.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - tx).toInt()
                        val dy = (e.rawY - ty).toInt()
                        if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                        p.x = sx + dx
                        p.y = sy + dy
                        wm.updateViewLayout(b, p)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            if (panel == null) openPanel() else closePanel()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun text(value: String, size: Float = 14f): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(8, 9, 8, 9)
        }
    }

    private fun button(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setOnClickListener { action() }
        }
    }

    private fun openPanel() {
        if (panel != null) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 13, 15, 15)
            background = bg(Color.rgb(16, 15, 32), 24f)
        }

        val title = text("👻 GHOST X ULTIMATE", 20f).apply {
            setTextColor(Color.CYAN)
        }

        clock = text("در حال دریافت ساعت...", 13f)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        placeInput = EditText(this).apply {
            hint = "Place ID بازی را وارد کن"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            background = bg(Color.rgb(35, 33, 55))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val gameResult = text("اطلاعات بازی هنوز دریافت نشده است.")
        result = gameResult

        content.addView(text("🎮 اطلاعات بازی", 17f))
        content.addView(placeInput)
        content.addView(button("دریافت اطلاعات بازی") {
            val id = placeInput?.text.toString().trim()
            if (id.isNotEmpty()) loadGame(id)
            else gameResult.text = "ابتدا Place ID را وارد کن."
        })

        content.addView(button("⭐ ذخیره بازی در علاقه‌مندی‌ها") {
            val id = placeInput?.text.toString().trim()
            if (id.isNotEmpty()) {
                val old = prefs.getString("favorites", "") ?: ""
                val list = old.split(",").filter { it.isNotBlank() }.toMutableSet()
                list.add(id)
                prefs.edit().putString("favorites", list.joinToString(",")).apply()
                gameResult.text = "بازی $id ذخیره شد."
            }
        })

        content.addView(button("نمایش علاقه‌مندی‌ها") {
            gameResult.text = "علاقه‌مندی‌ها:\n" +
                (prefs.getString("favorites", "") ?: "موردی وجود ندارد")
        })

        content.addView(button("نمایش تاریخچه") {
            gameResult.text = "تاریخچه:\n" +
                (prefs.getString("history", "") ?: "هنوز جست‌وجویی انجام نشده")
        })

        content.addView(gameResult)

        content.addView(text("👤 جست‌وجوی کاربر Roblox", 17f))

        searchInput = EditText(this).apply {
            hint = "نام کاربری Roblox"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            background = bg(Color.rgb(35, 33, 55))
        }

        val userResult = text("نتیجه جست‌وجوی کاربر اینجا نمایش داده می‌شود.")

        content.addView(searchInput)
        content.addView(button("جست‌وجوی کاربر") {
            val name = searchInput?.text.toString().trim()
            if (name.isNotEmpty()) searchUser(name, userResult)
        })
        content.addView(userResult)

        content.addView(text("🌐 سرورهای عمومی", 17f))
        content.addView(button("دریافت سرورهای بازی") {
            val id = placeInput?.text.toString().trim()
            if (id.isNotEmpty()) loadServers(id, userResult)
            else userResult.text = "ابتدا Place ID را وارد کن."
        })

        content.addView(text("⏱️ ابزار زمان", 17f))
        content.addView(button("شروع تایمر") {
            timerStart = System.currentTimeMillis()
            timerRunning = true
        })
        content.addView(button("توقف تایمر") {
            timerRunning = false
        })
        content.addView(button("بازنشانی تایمر") {
            timerRunning = false
            timerStart = System.currentTimeMillis()
        })

        content.addView(button("بستن پنل") { closePanel() })

        scroll.addView(content)
        root.addView(title)
        root.addView(clock)
        root.addView(scroll)

        val p = params(320, 650, true)
        p.x = 15
        p.y = 120

        panel = root
        wm.addView(root, p)
    }

    private fun request(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "GHOST-X-Android")
        connection.setRequestProperty("Accept", "application/json")

        try {
            val stream = if (connection.responseCode in 200..299)
                connection.inputStream else connection.errorStream

            val data = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299)
                throw Exception("HTTP ${connection.responseCode}: $data")

            return JSONObject(data)
        } finally {
            connection.disconnect()
        }
    }

    private fun loadGame(placeId: String) {
        val output = result ?: return
        output.text = "در حال دریافت اطلاعات بازی..."

        worker.execute {
            try {
                val details = request(
                    "https://games.roblox.com/v1/games/multiget-place-details?placeIds=$placeId"
                )

                val item = details.optJSONObject("data")
                    ?: details.optJSONArray("data")?.optJSONObject(0)
                    ?: throw Exception("اطلاعات بازی پیدا نشد.")

                val universe = item.optLong("universeId")
                val name = item.optString("name", "بدون نام")
                val description = item.optString("description", "")
                val rootPlace = item.optLong("placeId", placeId.toLongOrNull() ?: 0L)

                val game = request(
                    "https://games.roblox.com/v1/games?universeIds=$universe"
                )
                val data = game.optJSONArray("data")?.optJSONObject(0)
                    ?: throw Exception("اطلاعات عمومی بازی در دسترس نیست.")

                val playing = data.optInt("playing")
                val visits = data.optLong("visits")
                val creator = data.optJSONObject("creator")?.optString("name") ?: "نامشخص"

                val outputText =
                    "🎮 $name\n\n" +
                    "Place ID: $rootPlace\n" +
                    "Universe ID: $universe\n" +
                    "👥 بازیکنان آنلاین: $playing\n" +
                    "👁 بازدیدها: $visits\n" +
                    "سازنده: $creator\n\n" +
                    description.take(450)

                val old = prefs.getString("history", "") ?: ""
                val history = (listOf(placeId) + old.split(","))
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(20)
                    .joinToString(",")

                prefs.edit().putString("history", history).apply()

                handler.post { output.text = outputText }

            } catch (e: Exception) {
                handler.post {
                    output.text = "خطا در دریافت اطلاعات:\n${e.message}\n\nPlace ID را بررسی کن."
                }
            }
        }
    }

    private fun searchUser(name: String, output: TextView) {
        output.text = "در حال جست‌وجوی کاربر..."

        worker.execute {
            try {
                val encoded = URLEncoder.encode(name, "UTF-8")
                val json = request(
                    "https://users.roblox.com/v1/users/search?keyword=$encoded&limit=10"
                )

                val users = json.optJSONArray("data") ?: JSONArray()
                val lines = StringBuilder("نتایج جست‌وجو:\n\n")

                for (i in 0 until users.length()) {
                    val u = users.getJSONObject(i)
                    lines.append("نام: ${u.optString("name")}\n")
                    lines.append("نمایشی: ${u.optString("displayName")}\n")
                    lines.append("User ID: ${u.optLong("id")}\n\n")
                }

                if (users.length() == 0) lines.append("کاربری پیدا نشد.")

                handler.post { output.text = lines.toString() }

            } catch (e: Exception) {
                handler.post { output.text = "خطا: ${e.message}" }
            }
        }
    }

    private fun loadServers(placeId: String, output: TextView) {
        output.text = "در حال دریافت سرورهای عمومی..."

        worker.execute {
            try {
                val detail = request(
                    "https://games.roblox.com/v1/games/multiget-place-details?placeIds=$placeId"
                )

                val item = detail.optJSONObject("data")
                    ?: detail.optJSONArray("data")?.optJSONObject(0)
                    ?: throw Exception("بازی پیدا نشد.")

                val universe = item.optLong("universeId")

                val json = request(
                    "https://games.roblox.com/v1/games/$universe/servers/Public?sortOrder=Asc&limit=10"
                )

                val servers = json.optJSONArray("data") ?: JSONArray()
                val lines = StringBuilder("سرورهای عمومی:\n\n")

                for (i in 0 until servers.length()) {
                    val s = servers.getJSONObject(i)
                    lines.append("سرور ${i + 1}\n")
                    lines.append("بازیکنان: ${s.optInt("playing")} / ${s.optInt("maxPlayers")}\n")
                    lines.append("شناسه سرور: ${s.optString("id")}\n\n")
                }

                if (servers.length() == 0) lines.append("سروری پیدا نشد.")

                handler.post { output.text = lines.toString() }

            } catch (e: Exception) {
                handler.post { output.text = "خطا: ${e.message}" }
            }
        }
    }

    private fun closePanel() {
        panel?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        panel = null
    }

    override fun onDestroy() {
        closePanel()
        bubble?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bubble = null
        handler.removeCallbacks(clockTask)
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
