// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class CustomLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    init {
        registry.currentState = Lifecycle.State.RESUMED
    }

    override val lifecycle: Lifecycle
        get() = registry

    fun handleEvent(event: Lifecycle.Event) {
        registry.handleLifecycleEvent(event)
    }
}

class CustomSavedStateRegistryOwner(
    private val lifecycleOwner: LifecycleOwner
) : SavedStateRegistryOwner {
    private val controller = SavedStateRegistryController.create(this).apply {
        performRestore(Bundle())
    }

    override val lifecycle: Lifecycle
        get() = lifecycleOwner.lifecycle

    override val savedStateRegistry: SavedStateRegistry
        get() = controller.savedStateRegistry
}

class CustomViewModelStoreOwner : ViewModelStoreOwner {
    private val store = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = store
}

object ViewTreeHelper {
    fun setupViewTree(view: View, activity: Activity) {
        val lifecycleOwner = if (activity is LifecycleOwner) activity else CustomLifecycleOwner()
        val savedStateOwner = if (activity is SavedStateRegistryOwner) activity else CustomSavedStateRegistryOwner(lifecycleOwner)
        val viewModelStoreOwner = if (activity is ViewModelStoreOwner) activity else CustomViewModelStoreOwner()

        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(savedStateOwner)
        view.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
    }
}
