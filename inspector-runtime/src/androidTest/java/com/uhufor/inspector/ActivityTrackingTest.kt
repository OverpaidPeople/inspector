package com.uhufor.inspector

import android.os.ParcelFileDescriptor
import android.view.ContextThemeWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.uhufor.inspector.util.ActivityTracker
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ActivityTrackingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun lateApplicationRegistrationRecoversTheVisibleActivity() {
        rule.setContent { Text("Already resumed") }
        val tracker = ActivityTracker()
        try {
            rule.runOnIdle {
                tracker.register(rule.activity.application)
                assertSame(rule.activity, tracker.top)
            }
        } finally {
            rule.activity.application.unregisterActivityLifecycleCallbacks(tracker)
        }
    }

    @Test fun repeatedRegistrationWithActivityContextRefreshesTheCurrentActivity() {
        rule.setContent { Text("Already resumed") }
        val tracker = ActivityTracker(windows = { emptyList() })
        try {
            rule.runOnIdle {
                tracker.register(rule.activity.application)
                assertNull(tracker.top)
                tracker.register(ContextThemeWrapper(rule.activity, android.R.style.Theme_Material))
                tracker.register(rule.activity)
                assertSame(rule.activity, tracker.top)
            }
        } finally {
            rule.activity.application.unregisterActivityLifecycleCallbacks(tracker)
        }
    }

    @Test fun stoppedAndDestroyedActivitiesAreCleared() {
        rule.setContent { Text("Track lifecycle") }
        val tracker = ActivityTracker(windows = { emptyList() })
        try {
            rule.runOnIdle {
                tracker.register(rule.activity)
                assertSame(rule.activity, tracker.top)
                tracker.onActivityStopped(rule.activity)
                assertNull(tracker.top)
                tracker.onActivityResumed(rule.activity)
                assertSame(rule.activity, tracker.top)
                tracker.onActivityDestroyed(rule.activity)
                assertNull(tracker.top)
            }
        } finally {
            rule.activity.application.unregisterActivityLifecycleCallbacks(tracker)
        }
    }

    @Test fun firstInspectionToggleWorksAfterLateApplicationInstallWithoutBackgrounding() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
                .bufferedReader().use { it.readText() }
        }
        rule.setContent { Text("Inspect on the first toggle") }
        shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        try {
            rule.runOnIdle {
                Inspector.install(rule.activity.application)
                Inspector.showFloatingTrigger()
                assertFalse(Inspector.isInspectionEnabled)
                // The first toolbar button calls this same public method.
                Inspector.toggleInspection()
                assertTrue(Inspector.isInspectionEnabled)
            }
            rule.waitForIdle()
        } finally {
            rule.runOnIdle {
                Inspector.disableInspection()
                Inspector.hideFloatingTrigger()
            }
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW default")
        }
    }
}
