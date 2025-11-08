/*
 * StatusBarLyric
 * Copyright (C) 2021-2022 fkj@fkj233.cn
 * https://github.com/Block-Network/StatusBarLyric
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as
 * published by Block-Network contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/Block-Network/StatusBarLyric/blob/main/LICENSE>.
 */

package statusbar.lyric.tools

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.Toast
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import statusbar.lyric.BuildConfig
import statusbar.lyric.MainActivity
import statusbar.lyric.tools.ActivityTools.isHook
import statusbar.lyric.tools.LogTools.log
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

@SuppressLint("StaticFieldLeak")
object Tools {
    val buildTime: String = SimpleDateFormat("yyyy/M/d H:m:s", Locale.CHINA).format(BuildConfig.BUILD_TIME)

    val getPhoneName by lazy {
        val xiaomiMarketName = getSystemProperties("ro.product.marketname")
        when {
            xiaomiMarketName.isNotEmpty() -> xiaomiMarketName.uppercaseFirstChar()
            else -> "${Build.BRAND.uppercaseFirstChar()} ${Build.MODEL}"
        }
    }

    fun String.uppercaseFirstChar(): String {
        val formattedBrand = this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        return formattedBrand
    }

    fun dp2px(context: Context, dpValue: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            context.resources.displayMetrics
        ).toInt()

    @SuppressLint("PrivateApi")
    fun getSystemProperties(key: String): String {
        val ret: String = try {
            Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java).invoke(null, key) as String
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (_: Exception) {
            ""
        }
        return ret
    }

    fun <T> observableChange(
        initialValue: T, onChange: (oldValue: T, newValue: T) -> Unit
    ): ReadWriteProperty<Any?, T> {
        return Delegates.observable(initialValue) { _, oldVal, newVal ->
            if (oldVal != newVal) {
                onChange(oldVal, newVal)
            }
        }
    }

    fun goMainThread(delayed: Long = 0, callback: () -> Unit): Boolean {
        return Handler(Looper.getMainLooper()).postDelayed({ callback() }, delayed)
    }

    fun Context.isLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun getPref(key: String): XSharedPreferences? {
        return try {
            val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, key)
            if (pref.file.canRead()) pref else null
        } catch (e: Throwable) {
            e.log()
            null
        }
    }

    fun getSP(context: Context, key: String): SharedPreferences {
        @Suppress("DEPRECATION", "WorldReadableFiles")
        return context.createDeviceProtectedStorageContext()
            .getSharedPreferences(key, if (isHook()) Context.MODE_WORLD_READABLE else Context.MODE_PRIVATE)
    }

    fun shell(command: String, isSu: Boolean) {
        try {
            if (isSu) {
                try {
                    val p = Runtime.getRuntime().exec("su")
                    val outputStream = p.outputStream
                    DataOutputStream(outputStream).apply {
                        writeBytes(command)
                        flush()
                        close()
                    }
                    outputStream.close()
                } catch (_: Exception) {
                    // Su shell command failed
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(MainActivity.appContext, "Root permissions required!!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Runtime.getRuntime().exec(command)
            }
        } catch (_: Throwable) {
            // Shell command failed
        }
    }

    inline fun <T> T?.isNotNull(callback: (T) -> Unit): Boolean {
        if (this != null) {
            callback(this)
            return true
        }
        return false
    }

    inline fun Any?.isNull(callback: () -> Unit): Boolean {
        if (this == null) {
            callback()
            return true
        }
        return false
    }

    inline fun <T> T?.ifNotNull(callback: (T) -> Any?): Any? {
        if (this != null) {
            return callback(this)
        }
        return null
    }

    fun Any?.isNull() = this == null

    fun Any?.isNotNull() = this != null

    fun Any.getObjectField(fieldName: String): Any? {
        return XposedHelpers.getObjectField(this, fieldName)
    }

    fun Any?.existField(fieldName: String): Boolean {
        if (this == null) return false
        return XposedHelpers.findFieldIfExists(this.javaClass, fieldName).isNotNull()
    }

    fun Any.getObjectFieldIfExist(fieldName: String): Any? {
        return try {
            XposedHelpers.getObjectField(this, fieldName)
        } catch (_: Throwable) {
            null
        }
    }

    fun Any.setObjectField(fieldName: String, value: Any?) {
        XposedHelpers.setObjectField(this, fieldName, value)
    }

    fun Any.callMethod(methodName: String, vararg args: Any): Any? {
        return XposedHelpers.callMethod(this, methodName, *args)
    }
}
