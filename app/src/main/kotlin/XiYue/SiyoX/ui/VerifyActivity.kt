// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import XiYue.SiyoX.hook.MainHook
import XiYue.SiyoX.ui.pages.VerifyPage
import XiYue.SiyoX.ui.theme.SiyoXTheme

class VerifyActivity : ComponentActivity() {

    private var isVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isVerified) {
                    Toast.makeText(this@VerifyActivity, "请先完成网络验证以继续使用", Toast.LENGTH_SHORT).show()
                } else {
                    finish()
                }
            }
        })

        setContent {
            SiyoXTheme {
                VerifyPage(
                    isOverlayMode = true,
                    onVerifiedSuccess = {
                        isVerified = true
                        MainHook.notifyVerified()
                        Toast.makeText(this@VerifyActivity, "授权通过，正在进入游戏…", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}
