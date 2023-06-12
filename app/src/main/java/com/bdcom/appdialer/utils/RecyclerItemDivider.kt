package com.bdcom.appdialer.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class RecyclerItemDivider(
    context: Context,
    orientation: Int = 0,
    marginStart: Int = 0,
    marginEnd: Int = 0
) : RecyclerView.ItemDecoration() {
    private val attributes = intArrayOf(android.R.attr.listDivider)
    private val horizontal = LinearLayoutManager.HORIZONTAL
    private val vertical = LinearLayoutManager.VERTICAL
    private val mDivider: Drawable?
    private var mOrientation: Int
    private val mContext: Context = context
    private val mMarginStart: Int
    private val mMarginEnd: Int

    init {
        mOrientation = orientation
        mMarginStart = marginStart
        mMarginEnd = marginEnd
        val a = context.obtainStyledAttributes(attributes)
        mDivider = a.getDrawable(0)
        a.recycle()
        setOrientation(mOrientation)
    }

    private fun setOrientation(orientation: Int) {
        if (orientation != horizontal && orientation != vertical) {
            throw IllegalArgumentException("invalid orientation")
        }
        mOrientation = orientation
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (mOrientation == vertical) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(c: Canvas, parent: RecyclerView) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount - 1
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child
                .layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + mDivider!!.intrinsicHeight
            mDivider.setBounds(left + dpToPx(mMarginStart), top, right - dpToPx(mMarginEnd), bottom)
            mDivider.draw(c)
        }
    }

    private fun drawHorizontal(c: Canvas, parent: RecyclerView) {
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom

        val childCount = parent.childCount - 1
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child
                .layoutParams as RecyclerView.LayoutParams
            val left = child.right + params.rightMargin
            val right = left + mDivider!!.intrinsicHeight
            mDivider.setBounds(left, top + dpToPx(mMarginStart), right, bottom - dpToPx(mMarginEnd))
            mDivider.draw(c)
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (mOrientation == vertical) {
            outRect.set(0, 0, 0, mDivider!!.intrinsicHeight)
        } else {
            outRect.set(0, 0, mDivider!!.intrinsicWidth, 0)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val rsc = mContext.resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), rsc.displayMetrics).roundToInt()
    }
}
