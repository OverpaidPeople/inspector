package com.uhufor.inspector.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.uhufor.inspector.R
import kotlin.math.roundToInt

/** Native handles retain screen-space MotionEvents even when resizing also moves the window. */
// This window-only container requires an explicitly supplied content View, not XML inflation.
@SuppressLint("ClickableViewAccessibility", "ViewConstructor")
internal class ResizableDetailsLayout(context: Context, content: View) : FrameLayout(context) {
    var onResizeStart: (ResizeCorner, Float, Float) -> Unit = { _, _, _ -> }
    var onResizeMove: (Float, Float) -> Unit = { _, _ -> }
    var onResizeEnd: () -> Unit = {}
    var onResizeCancel: () -> Unit = {}
    var onViewportChanged: () -> Unit = {}
    private var active = false

    init {
        isMotionEventSplittingEnabled = false
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val handleSize = (24 * resources.displayMetrics.density).roundToInt()
        ResizeCorner.entries.forEach { corner ->
            val handle = object : View(context) {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.DKGRAY
                    strokeWidth = resources.displayMetrics.density
                }
                override fun onDraw(canvas: Canvas) {
                    val dp = resources.displayMetrics.density
                    fun x(distance: Float) = if (corner.left) distance * dp else width - distance * dp
                    fun y(distance: Float) = if (corner.top) distance * dp else height - distance * dp
                    for (length in listOf(5f, 9f, 13f)) {
                        canvas.drawLine(x(3f), y(length), x(length), y(3f), paint)
                    }
                }
            }.apply {
                tag = corner.viewTag
                contentDescription = context.getString(when (corner) {
                    ResizeCorner.TOP_LEFT -> R.string.inspector_resize_top_left
                    ResizeCorner.TOP_RIGHT -> R.string.inspector_resize_top_right
                    ResizeCorner.BOTTOM_LEFT -> R.string.inspector_resize_bottom_left
                    ResizeCorner.BOTTOM_RIGHT -> R.string.inspector_resize_bottom_right
                })
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            active = true
                            onResizeStart(corner, event.rawX, event.rawY)
                        }
                        MotionEvent.ACTION_MOVE -> if (active) onResizeMove(event.rawX, event.rawY)
                        MotionEvent.ACTION_UP -> if (active) {
                            onResizeMove(event.rawX, event.rawY)
                            active = false
                            onResizeEnd()
                        }
                        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> if (active) {
                            active = false
                            onResizeCancel()
                        }
                    }
                    true
                }
            }
            addView(handle, LayoutParams(handleSize, handleSize,
                (if (corner.left) Gravity.LEFT else Gravity.RIGHT) or
                    (if (corner.top) Gravity.TOP else Gravity.BOTTOM)))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (active) {
            active = false
            onResizeCancel()
        }
        post { onViewportChanged() }
    }
}
