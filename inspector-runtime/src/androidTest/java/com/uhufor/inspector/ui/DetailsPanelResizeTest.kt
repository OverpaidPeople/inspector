package com.uhufor.inspector.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.uhufor.inspector.UnitMode
import com.uhufor.inspector.engine.SelectionState
import com.uhufor.inspector.engine.UiNodeProperties
import com.uhufor.inspector.engine.UiNodeType
import com.uhufor.inspector.engine.UiNodeActionProperties
import com.uhufor.inspector.engine.UiNodeStyleProperties
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DetailsPanelResizeTest {
    @get:Rule val rule = createComposeRule()

    private fun event(view: View, action: Int, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        try { view.dispatchTouchEvent(event) } finally { event.recycle() }
    }

    @Test fun allNativeCornerHandlesDeliverDragAndReleaseWithoutTouchingContent() {
        lateinit var panel: ResizableDetailsLayout
        var contentClicks = 0
        val started = mutableListOf<ResizeCorner>()
        val positions = mutableListOf<Pair<Float, Float>>()
        var ended = 0
        rule.setContent {
            AndroidView(factory = { context ->
                val content = View(context).apply {
                    setBackgroundColor(Color.WHITE)
                    setOnClickListener { contentClicks++ }
                }
                ResizableDetailsLayout(context, content).apply {
                    panel = this
                    onResizeStart = { corner, _, _ -> started += corner }
                    onResizeMove = { x, y -> positions += x to y }
                    onResizeEnd = { ended++ }
                }
            }, modifier = Modifier.size(240.dp, 320.dp))
        }
        rule.runOnIdle {
            ResizeCorner.entries.forEach { corner ->
                val handle = panel.findViewWithTag<View>(corner.viewTag)
                event(handle, MotionEvent.ACTION_DOWN, 10f, 10f)
                event(handle, MotionEvent.ACTION_MOVE, 35f, 45f)
                event(handle, MotionEvent.ACTION_UP, 35f, 45f)
            }
            assertEquals(ResizeCorner.entries, started)
            assertEquals(4, ended)
            assertEquals(List(8) { 35f to 45f }, positions)
            assertEquals(0, contentClicks)
            event(panel, MotionEvent.ACTION_DOWN, panel.width / 2f, panel.height / 2f)
            event(panel, MotionEvent.ACTION_UP, panel.width / 2f, panel.height / 2f)
        }
        rule.runOnIdle {
            assertEquals(1, contentClicks)
            assertEquals(4, started.size)
        }
    }

    @Test fun cancelledGestureDoesNotCommitResize() {
        lateinit var panel: ResizableDetailsLayout
        var cancelled = 0
        var ended = 0
        rule.setContent {
            AndroidView(factory = { context ->
                ResizableDetailsLayout(context, View(context)).apply {
                    panel = this
                    onResizeCancel = { cancelled++ }
                    onResizeEnd = { ended++ }
                }
            }, modifier = Modifier.size(240.dp, 320.dp))
        }
        rule.runOnIdle {
            val handle = panel.findViewWithTag<View>(ResizeCorner.BOTTOM_RIGHT.viewTag)
            event(handle, MotionEvent.ACTION_DOWN, 10f, 10f)
            event(handle, MotionEvent.ACTION_CANCEL, 10f, 10f)
            event(handle, MotionEvent.ACTION_UP, 10f, 10f)
            assertEquals(1, cancelled)
            assertEquals(0, ended)
        }
    }

    @Test fun preferenceStoreRestoresSizeAcrossInstancesAndRejectsCorruptData() {
        lateinit var context: Context
        rule.setContent { context = LocalContext.current.applicationContext }
        val name = "inspector_resize_test_${SystemClock.uptimeMillis()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            assertNull(DetailsPanelSizeStore(context, name).load())
            DetailsPanelSizeStore(context, name).save(DetailsPanelSize(280f, 420f))
            assertEquals(DetailsPanelSize(280f, 420f), DetailsPanelSizeStore(context, name).load())
            DetailsPanelSizeStore(context, name).save(DetailsPanelSize(Float.NaN, 1f))
            assertEquals(DetailsPanelSize(280f, 420f), DetailsPanelSizeStore(context, name).load())
            preferences.edit().putString("width_dp", "corrupt").commit()
            assertNull(DetailsPanelSizeStore(context, name).load())
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun realOverlayWindowResizesAndRestoresAfterRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
                .bufferedReader().use { it.readText() }
        }
        shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        context.deleteSharedPreferences(DetailsPanelSizeStore.PREFERENCES_NAME)
        var floating: FloatingDetailsView? = null
        val selection = SelectionState(1, RectF(0f, 0f, 100f, 50f), null,
            UiNodeProperties(UiNodeType.COMPOSE, "resize", Size(100, 50), RectF(), RectF(), RectF()))
        fun window() = checkNotNull(floating?.panelView)
        var expectedWidth = 0
        var expectedHeight = 0
        rule.setContent { }
        try {
            rule.runOnIdle {
                floating = FloatingDetailsView(context).also {
                    it.install(selection, UnitMode.DP, 1f)
                    it.updateSticky(Rect(0, 100, 100, 200))
                }
            }
            rule.waitForIdle()
            rule.runOnIdle {
                val root = window()
                val params = root.layoutParams as WindowManager.LayoutParams
                expectedWidth = params.width + 60
                expectedHeight = params.height + 90
                val handle = root.findViewWithTag<View>(ResizeCorner.BOTTOM_RIGHT.viewTag)
                event(handle, MotionEvent.ACTION_DOWN, 10f, 10f)
                event(handle, MotionEvent.ACTION_MOVE, 70f, 100f)
                event(handle, MotionEvent.ACTION_UP, 70f, 100f)
                assertEquals(expectedWidth, params.width)
                assertEquals(expectedHeight, params.height)
            }
            rule.waitForIdle()
            rule.runOnIdle {
                assertEquals(expectedWidth, window().width)
                assertEquals(expectedHeight, window().height)
                assertNotNull(DetailsPanelSizeStore(context).load())
                floating!!.uninstall()
                floating = FloatingDetailsView(context).also {
                    it.install(selection, UnitMode.DP, 1f)
                    it.updateSticky(Rect(0, 100, 100, 200))
                }
            }
            rule.waitForIdle()
            rule.runOnIdle {
                assertEquals(expectedWidth, window().width)
                assertEquals(expectedHeight, window().height)
            }
        } finally {
            rule.runOnIdle { floating?.uninstall() }
            context.deleteSharedPreferences(DetailsPanelSizeStore.PREFERENCES_NAME)
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW default")
        }
    }

    @Test fun repeatedSectionTogglesKeepTheSameOverlayWindowAndSize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
                .bufferedReader().use { it.readText() }
        }
        shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        context.deleteSharedPreferences(DetailsPanelSizeStore.PREFERENCES_NAME)
        DetailsPanelSizeStore(context).save(DetailsPanelSize(300f, 640f))
        var floating: FloatingDetailsView? = null
        var root: ResizableDetailsLayout? = null
        var detached = 0
        var width = 0
        var height = 0
        val selection = SelectionState(1, RectF(0f, 0f, 100f, 50f), null,
            UiNodeProperties(UiNodeType.COMPOSE, "stress", Size(100, 50), RectF(), RectF(), RectF(),
                actions = setOf(UiNodeActionProperties.CLICKABLE),
                styles = setOf(
                    UiNodeStyleProperties.TextStyle("Long section content for repeated collapse and expansion", Color.BLUE,
                        null, false, false, textSizeSp = 16f, fontWeight = 400, fontFamily = "sans-serif"),
                    UiNodeStyleProperties.ColorStyle("Modifier.background", Color.WHITE),
                    UiNodeStyleProperties.AppearanceStyle(listOf(8f, 8f, 8f, 8f), 2f, Color.BLUE, 1f),
                )))
        rule.setContent { }
        try {
            rule.runOnIdle {
                floating = FloatingDetailsView(context).also {
                    it.install(selection, UnitMode.DP, 1f)
                    it.updateSticky(Rect(0, 0, 100, 100))
                }
            }
            rule.waitForIdle()
            rule.runOnIdle {
                root = floating!!.panelView!!
                width = root!!.width
                height = root!!.height
                assertTrue((root!!.layoutParams as WindowManager.LayoutParams).flags and
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM != 0)
                root!!.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) = Unit
                    override fun onViewDetachedFromWindow(view: View) { detached++ }
                })
            }
            // Exercise real rendering/disposal, not just the standalone section state model.
            repeat(6) {
                DetailsSection.entries.forEach { section ->
                    rule.onNodeWithTag(section.headerTag).performScrollTo().performClick()
                }
            }
            DetailsSection.entries.forEach { rule.onNodeWithTag(it.headerTag).assertIsOn() }
            rule.runOnIdle {
                assertSame(root, floating!!.panelView)
                assertEquals(0, detached)
                assertEquals(width, root!!.width)
                assertEquals(height, root!!.height)
                assertTrue(DetailsSectionStore(context).load().isEmpty())
            }
        } finally {
            rule.runOnIdle { floating?.uninstall() }
            context.deleteSharedPreferences(DetailsPanelSizeStore.PREFERENCES_NAME)
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW default")
        }
    }
}
