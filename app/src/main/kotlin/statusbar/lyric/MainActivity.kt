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

package statusbar.lyric

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import statusbar.lyric.config.ActivityOwnSP
import statusbar.lyric.config.ActivityOwnSP.config
import statusbar.lyric.tools.ActivityTools
import statusbar.lyric.tools.ActivityTools.isHook
import statusbar.lyric.tools.BackupTools
import statusbar.lyric.tools.ConfigTools
import statusbar.lyric.tools.LogTools
import statusbar.lyric.tools.Tools.isNotNull

class MainActivity : ComponentActivity() {
    lateinit var createDocumentLauncher: ActivityResultLauncher<Intent>
    lateinit var openDocumentLauncher: ActivityResultLauncher<Intent>

    companion object {
        lateinit var appContext: Context private set

        var isLoad: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = this
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false  // Xiaomi moment, this code must be here

        createDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data.isNotNull()) {
                BackupTools.handleCreateDocument(this, result.data!!.data)
            }
        }

        openDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data.isNotNull()) {
                BackupTools.handleReadDocument(this, result.data!!.data)
                Thread {
                    Thread.sleep(500)
                    ActivityTools.restartApp()
                }.start()
            }
        }

        isLoad = isHook()
        init()

        setContent {
            App()
        }
    }

    private fun init() {
        ConfigTools(ActivityOwnSP.ownSP)
        if (!BuildConfig.DEBUG) {
            LogTools.init(true)
        }
        LogTools.init(config.outLog)
    }
}
