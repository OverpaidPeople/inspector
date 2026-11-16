package com.uhufor.inspector.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import androidx.lifecycle.findViewTreeLifecycleOwner
import java.lang.ref.WeakReference

internal class ActivityTracker(
    private val windows: () -> List<View> = {
        runCatching { WindowManagerUtils.getDecorViews().orEmpty() }.getOrDefault(emptyList())
    },
) : Application.ActivityLifecycleCallbacks {
    private var topReference: WeakReference<Activity>? = null
    private var registeredApplication: WeakReference<Application>? = null

    val top: Activity?
        get() {
            topReference?.get()?.takeIf { !it.isDestroyed && !it.isFinishing }?.let { return it }
            // Registration may happen after the first Activity has already resumed.
            // Use existing app windows to recover it without waiting for another resume.
            val activity = windows().asReversed().asSequence()
                .filter { it.isAttachedToWindow && it.isShown }
                .mapNotNull { window ->
                    (window.findViewTreeLifecycleOwner() as? Activity)
                        ?: window.context.activity()
                        ?: window.findViewById<View>(android.R.id.content)?.context?.activity()
                }.firstOrNull { !it.isDestroyed && !it.isFinishing }
            if (activity != null) updateTopActivity(activity)
            return activity
        }

    fun register(context: Context) {
        val application = context.applicationContext as? Application
        if (application != null && registeredApplication?.get() !== application) {
            registeredApplication?.get()?.unregisterActivityLifecycleCallbacks(this)
            application.registerActivityLifecycleCallbacks(this)
            registeredApplication = WeakReference(application)
        }

        context.activity()?.let(::updateTopActivity)
    }

    override fun onActivityStarted(activity: Activity) {
        updateTopActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        updateTopActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = clearIfTop(activity)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = clearIfTop(activity)

    private fun clearIfTop(activity: Activity) {
        if (topReference?.get() === activity) topReference = null
    }

    private fun updateTopActivity(activity: Activity) {
        topReference = WeakReference(activity)
    }

    private fun Context.activity(): Activity? {
        var current: Context = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            val base = current.baseContext
            if (base === current) break
            current = base
        }
        return null
    }
}
