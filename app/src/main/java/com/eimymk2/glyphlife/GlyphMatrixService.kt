package com.eimymk2.glyphlife

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Glyph Matrix の Toy Service 用の基底クラス。
 *
 * 継承して performOnServiceConnected / performOnServiceDisconnected を実装するだけで
 * Toy として動作する。
 *
 * 元コード: Nothing 公式 Example Project の GlyphMatrixService.kt
 * 改変: register() を Phone (4a) Pro 用の DEVICE_25111p に変更
 */
abstract class GlyphMatrixService(private val tag: String) : Service() {

    /**
     * Glyph Button からのイベントを受け取るハンドラ。
     * Phone (4a) Pro は Glyph Touch 非対応なので、これらのイベントは
     * 実機では発火しないが、互換性のため残してある。
     */
    private val buttonPressedHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    msg.data?.let { data ->
                        if (data.containsKey(KEY_DATA)) {
                            data.getString(KEY_DATA)?.let { value ->
                                when (value) {
                                    GlyphToy.EVENT_ACTION_DOWN -> onTouchPointPressed()
                                    GlyphToy.EVENT_ACTION_UP -> onTouchPointReleased()
                                    GlyphToy.EVENT_CHANGE -> onTouchPointLongPress()
                                }
                            }
                        }
                    }
                }
                else -> {
                    Log.d(LOG_TAG, "Message: ${msg.what}")
                    super.handleMessage(msg)
                }
            }
        }
    }

    private val serviceMessenger = Messenger(buttonPressedHandler)

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(p0: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                Log.d(LOG_TAG, "$tag: onServiceConnected")
                // ⚠️ Phone (4a) Pro 用デバイスID
                gmm.register(Glyph.DEVICE_23112)
                performOnServiceConnected(applicationContext, gmm)
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {}
    }

    final override fun startService(intent: Intent?): ComponentName? {
        Log.d(LOG_TAG, "$tag: startService")
        return super.startService(intent)
    }

    final override fun onBind(intent: Intent?): IBinder? {
        Log.d(LOG_TAG, "$tag: onBind")
        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(gmmCallback)
            Log.d(LOG_TAG, "$tag: onBind completed")
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        Log.d(LOG_TAG, "$tag: onUnbind")
        glyphMatrixManager?.let {
            Log.d(LOG_TAG, "$tag: onServiceDisconnected")
            performOnServiceDisconnected(applicationContext)
        }
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        return false
    }

    /** bind 後、SDK が ready になったら呼ばれる。サブクラスで描画ループを開始する */
    open fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {}

    /** unbind 時に呼ばれる。サブクラスでタイマー停止などを行う */
    open fun performOnServiceDisconnected(context: Context) {}

    /** Glyph Button のイベント（Phone (4a) Pro では発火しない） */
    open fun onTouchPointPressed() {}
    open fun onTouchPointLongPress() {}
    open fun onTouchPointReleased() {}

    private companion object {
        private val LOG_TAG = GlyphMatrixService::class.java.simpleName
        private const val KEY_DATA = "data"
    }
}