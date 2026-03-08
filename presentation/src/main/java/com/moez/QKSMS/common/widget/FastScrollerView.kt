/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

package com.moez.QKSMS.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.prauga.messages.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FastScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface SectionTitleProvider {
        fun getSectionTitle(position: Int): String
    }

    private var recyclerView: RecyclerView? = null
    private var thumb: View
    private var popup: TextView

    private var isDragging = false
    private var hidePopupRunnable = Runnable { popup.visibility = View.GONE }

    // --- OPTIMIZATION CACHES ---
    private var lastTargetPos = -1
    private var lastOffset = -1
    private var lastPopupPos = -1
    private var cachedItemHeight = 0
    private var cachedMaxTargetPos = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.fast_scroller_view, this, true)
        thumb = findViewById(R.id.fast_scroller_thumb)
        popup = findViewById(R.id.fast_scroller_popup)
        popup.visibility = View.GONE
    }

    fun setupWithRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!isDragging) {
                    updateThumbPosition()
                }
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateThumbPosition()
    }

    private fun updateThumbPosition() {
        val rv = recyclerView ?: return
        val extent = rv.computeVerticalScrollExtent()
        val range = rv.computeVerticalScrollRange()
        val offset = rv.computeVerticalScrollOffset()

        if (range <= extent || range == 0) {
            thumb.visibility = View.GONE
            return
        }

        thumb.visibility = View.VISIBLE
        val proportion = offset.toFloat() / (range - extent).toFloat()
        val maxThumbY = height - thumb.height
        val safeProportion = if (proportion.isNaN()) 0f else min(max(0f, proportion), 1f)
        thumb.y = safeProportion * maxThumbY
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val touchSlop = width - (48 * resources.displayMetrics.density)
            if (ev.x >= touchSlop && thumb.visibility == View.VISIBLE) {
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rv = recyclerView ?: return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchSlop = width - (48 * resources.displayMetrics.density)
                if (event.x >= touchSlop) {
                    isDragging = true
                    thumb.isPressed = true
                    removeCallbacks(hidePopupRunnable)

                    // Pre-calculate expensive math once when dragging starts
                    prepareScrollMath(rv)
                    updateScrollAndPopup(event.y)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateScrollAndPopup(event.y)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    thumb.isPressed = false
                    postDelayed(hidePopupRunnable, 500)

                    // Reset caches
                    lastTargetPos = -1
                    lastOffset = -1
                    lastPopupPos = -1
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun prepareScrollMath(rv: RecyclerView) {
        val adapter = rv.adapter ?: return
        val itemCount = adapter.itemCount

        // Grab height from the first visible view safely
        val firstView = rv.getChildAt(0)
        cachedItemHeight = firstView?.height?.takeIf { it > 0 } ?: (72 * resources.displayMetrics.density).toInt()

        val visibleItemCount = max(1, rv.height / cachedItemHeight)
        cachedMaxTargetPos = max(0, itemCount - visibleItemCount)
    }

    private fun updateScrollAndPopup(y: Float) {
        val rv = recyclerView ?: return
        val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
        val adapter = rv.adapter ?: return
        if (cachedMaxTargetPos <= 0) return

        // 1. Move the thumb visually
        val maxThumbY = height - thumb.height
        val newY = min(max(0f, y - thumb.height / 2f), maxThumbY.toFloat())
        thumb.y = newY
        val proportion = newY / maxThumbY

        // 2. Calculate smooth position
        val exactPosition = proportion * cachedMaxTargetPos
        val targetPos = exactPosition.toInt().coerceIn(0, cachedMaxTargetPos)
        val subItemFraction = exactPosition - targetPos
        val offset = -(subItemFraction * cachedItemHeight).toInt()

        // 3. ONLY trigger a layout pass if the position or offset actually changed
        // This prevents the UI thread from being spammed with redundant draw calls
        if (targetPos != lastTargetPos || abs(offset - lastOffset) > 1) {
            layoutManager.scrollToPositionWithOffset(targetPos, offset)
            lastTargetPos = targetPos
            lastOffset = offset
        }

        // 4. Update Date Modal Y-position constantly so it tracks the thumb perfectly
        val maxPopupY = height - popup.height
        popup.y = min(max(0f, newY + thumb.height / 2f - popup.height / 2f), maxPopupY.toFloat())

        // 5. ONLY re-format the date text if we crossed into a new item
        if (targetPos != lastPopupPos && adapter is SectionTitleProvider) {
            val title = adapter.getSectionTitle(targetPos)
            if (title.isNotEmpty()) {
                popup.text = title
                if (popup.visibility != View.VISIBLE) popup.visibility = View.VISIBLE
            } else {
                popup.visibility = View.GONE
            }
            lastPopupPos = targetPos
        }
    }
}
