package com.miapli.tclremap

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class TclRemapService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressDetected = false
    private var clickRunnable: Runnable? = null
    private val LONG_PRESS_DELAY = 550L
    private lateinit var prefs: SharedPreferences

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("TclRemapPrefs", Context.MODE_PRIVATE)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        val ES_NETFLIX = (keyCode == KeyEvent.KEYCODE_BUTTON_3 || keyCode == 201)
        val ES_PRIME = (keyCode == KeyEvent.KEYCODE_BUTTON_4 || keyCode == 202)
        val ES_YOUTUBE = (keyCode == KeyEvent.KEYCODE_BUTTON_1 || keyCode == 200)
        val ES_TCL_CHANNEL = (keyCode == 258 || keyCode == KeyEvent.KEYCODE_BUTTON_2)
        val ES_MENU = (keyCode == KeyEvent.KEYCODE_MENU || keyCode == 82)

        if (ES_NETFLIX || ES_PRIME || ES_YOUTUBE || ES_TCL_CHANNEL || ES_MENU) {
            val keyPrefix = when {
                ES_NETFLIX -> "netflix"
                ES_PRIME -> "prime"
                ES_YOUTUBE -> "youtube"
                ES_TCL_CHANNEL -> "tcl"
                else -> "menu"
            }

            if (action == KeyEvent.ACTION_DOWN) {
                isLongPressDetected = false
                
                clickRunnable = Runnable {
                    isLongPressDetected = true
                    val targetApp = prefs.getString("${keyPrefix}_long", obtenerPorDefecto(keyPrefix, true))
                    if (targetApp != "default_system") lanzarApp(targetApp)
                }
                handler.postDelayed(clickRunnable!!, LONG_PRESS_DELAY)
                return true
            }

            if (action == KeyEvent.ACTION_UP) {
                clickRunnable?.let { handler.removeCallbacks(it) }

                if (!isLongPressDetected) {
                    val targetApp = prefs.getString("${keyPrefix}_click", obtenerPorDefecto(keyPrefix, false))
                    if (targetApp == "default_system" && ES_MENU) {
                        return false // El botón de menú sigue abriendo los ajustes nativos de la TV
                    } else {
                        lanzarApp(targetApp)
                    }
                }
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun obtenerPorDefecto(prefix: String, esLargo: Boolean): String {
        return if (esLargo) {
            when (prefix) {
                "netflix" -> "com.movistar.android.tv"
                "prime" -> "org.xbmc.kodi"
                "youtube" -> "com.spotify.tv.android"
                "tcl" -> "com.alexvas.tv.tv"
                else -> "com.crunchyroll.crunchyroid"
            }
        } else {
            when (prefix) {
                "netflix" -> "com.netflix.mediaclient"
                "prime" -> "com.amazon.amazonvideo.livingroom"
                "youtube" -> "com.google.android.youtube.tv"
                "tcl" -> "tv.tivify.tv"
                else -> "default_system"
            }
        }
    }

    private fun lanzarApp(packageName: String?) {
        if (packageName.isNullOrEmpty() || packageName == "default_system") return
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Silencioso en caso de error
        }
    }
}
