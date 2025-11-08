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

package statusbar.lyric.hook.module.xiaomi

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import statusbar.lyric.config.XposedOwnSP.config
import statusbar.lyric.hook.module.SystemUILyric
import statusbar.lyric.tools.Tools.callMethod
import statusbar.lyric.tools.Tools.getObjectField
import statusbar.lyric.tools.Tools.isNotNull
import statusbar.lyric.tools.Tools.setObjectField
import java.lang.ref.WeakReference

class XiaomiHooks {
    companion object {
        private var notificationBigTimeRef: WeakReference<View>? = null
        private var lastIslandWidth: Int = 0

        fun getNotificationBigTime(): View? = notificationBigTimeRef?.get()
        private fun setNotificationBigTime(view: View?) {
            notificationBigTimeRef = if (view.isNotNull()) WeakReference(view) else null
        }

        fun getLastIslandWidth(): Int = lastIslandWidth

        fun init(systemUILyric: SystemUILyric) {
            // 处理通知中心时间
            loadClassOrNull($$"com.android.systemui.controlcenter.shade.NotificationHeaderExpandController$notificationCallback$1").isNotNull {
                it.methodFinder().filterByName("onExpansionChanged").filterFinal().single().createHook {
                    before {
                        if (systemUILyric.isMusicPlaying && !systemUILyric.isHiding && config.hideTime) {
                            val notificationHeaderExpandController = it.thisObject.getObjectField("this$0")
                            notificationHeaderExpandController?.setObjectField("bigTimeTranslationY", 0)
                            notificationHeaderExpandController?.setObjectField("notificationTranslationX", 0)

                            val bigTimeView = notificationHeaderExpandController
                                ?.getObjectField("headerController")?.callMethod("get")
                                ?.getObjectField("notificationBigTime") as? View

                            setNotificationBigTime(bigTimeView)

                            val f = it.args[0] as Float
                            if (f < 0.8f) getNotificationBigTime()?.visibility = View.GONE
                            else getNotificationBigTime()?.visibility = View.VISIBLE
                        }
                    }
                }
            }

            // 获取超级岛宽度
            loadClassOrNull($$"com.android.systemui.statusbar.IslandMonitor$RealContainerIslandMonitor").isNotNull {
                it.methodFinder().filterByName("getIslandWidth").firstOrNull()?.createHook {
                    after {
                        val islandWidth = it.result as Int
                        lastIslandWidth = islandWidth
                        systemUILyric.superIslandWidth = if (islandWidth > 0) islandWidth + config.islandOffset else 0
                    }
                }
            }
        }
    }
}