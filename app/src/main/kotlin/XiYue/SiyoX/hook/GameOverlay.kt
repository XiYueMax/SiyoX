// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import epic.verify.api.EpicVerifySDK
import epic.verify.api.Resp
import kotlin.concurrent.thread

object GameOverlay {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentDialog: AlertDialog? = null

    fun showOverlay(activity: Activity, onVerifiedSuccess: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (currentDialog != null && currentDialog?.isShowing == true) return

        mainHandler.post {
            try {
                val dialog = buildVerifyDialog(activity, onVerifiedSuccess)
                currentDialog = dialog
                dialog.show()
            } catch (e: Exception) {
                // Ignore if window token not ready
            }
        }
    }

    private fun buildVerifyDialog(activity: Activity, onVerifiedSuccess: () -> Unit): AlertDialog {
        val dp16 = dp(activity, 16)
        val dp8 = dp(activity, 8)
        val dp12 = dp(activity, 12)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F7F8FA"))
                cornerRadius = dp(activity, 20).toFloat()
            }
        }

        // Title
        val titleView = TextView(activity).apply {
            text = "SiyoX 网络验证"
            textSize = 20f
            setTextColor(Color.parseColor("#111111"))
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp8, 0, dp12)
        }
        root.addView(titleView)

        // Subtitle
        val subTitle = TextView(activity).apply {
            text = "欢迎使用 SiyoX 模块，请输入卡密继续"
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp16)
        }
        root.addView(subTitle)

        // Card input
        val sp = activity.getSharedPreferences("siyox_preferences", Context.MODE_PRIVATE)
        val savedCard = sp.getString("card_key", "") ?: ""

        val input = EditText(activity).apply {
            hint = "请输入卡密"
            setText(savedCard)
            textSize = 15f
            setSingleLine(true)
            setPadding(dp12, dp12, dp12, dp12)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(dp(activity, 1), Color.parseColor("#DDDDDD"))
                cornerRadius = dp(activity, 12).toFloat()
            }
        }
        root.addView(input)

        // Loading bar
        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
            setPadding(0, dp8, 0, dp8)
        }
        root.addView(progress)

        // Status text
        val statusText = TextView(activity).apply {
            text = ""
            textSize = 12f
            setTextColor(Color.parseColor("#0A84FF"))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp8, 0, dp8)
        }
        root.addView(statusText)

        // Verify button
        val btnVerify = Button(activity).apply {
            text = "立即验证"
            setTextColor(Color.WHITE)
            textSize = 16f
            paint.isFakeBoldText = true
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0A84FF"))
                cornerRadius = dp(activity, 12).toFloat()
            }
        }
        root.addView(btnVerify)

        val dialog = AlertDialog.Builder(activity)
            .setView(root)
            .setCancelable(false)
            .create()

        btnVerify.setOnClickListener {
            val card = input.text.toString().trim()
            if (card.isEmpty()) {
                Toast.makeText(activity, "请输入卡密", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnVerify.isEnabled = false
            statusText.text = "正在连接服务器验证..."

            thread {
                try {
                    val hosts = arrayOf("epic.z74d.top", "gl.t60.top", "test.t60.top", "epic.t5x.cc")
                    val sdk = EpicVerifySDK(hosts, 5000, "iJQfzsjaI5IHW7W6VKjDXmF7gMxpSy0s").apply {
                        setCard(card)
                    }

                    val resp: Resp = sdk.cardVerify()

                    mainHandler.post {
                        progress.visibility = View.GONE
                        btnVerify.isEnabled = true

                        if (resp.isSuccess) {
                            sp.edit().putString("card_key", card).apply()
                            Toast.makeText(activity, "验证成功，欢迎使用 SiyoX！", Toast.LENGTH_SHORT).show()
                            MainHook.notifyVerified()
                            onVerifiedSuccess()
                            dialog.dismiss()
                        } else {
                            statusText.text = "验证失败: ${resp.msg}"
                            Toast.makeText(activity, "验证失败: ${resp.msg}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        progress.visibility = View.GONE
                        btnVerify.isEnabled = true
                        statusText.text = "网络异常: ${e.message}"
                        Toast.makeText(activity, "连接异常: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return dialog
    }

    private fun dp(context: Context, v: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
