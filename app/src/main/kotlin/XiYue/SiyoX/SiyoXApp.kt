// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX

import android.app.Application
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager

class SiyoXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppSettings.init(this)
        VerifyManager.init(this)
    }

    companion object {
        lateinit var instance: SiyoXApp
            private set
    }
}
