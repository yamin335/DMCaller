package com.bdcom.appdialer.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class FavToogle(context: Context?, attrs: AttributeSet?) : View(context, attrs), View.OnTouchListener {

    private var isOn: Boolean = false
    private var bitmap: Bitmap? = null

    var onToggleListener: OnToggleListener? = null

    init {
        setOnTouchListener(this)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.d("===>", "onLayout")
        val width = width
        val height = height

        if (width != 0 && height != 0) {
            // Need_to_fix var drawable = if (isOn) R.drawable.ic_star_on else R.drawable.ic_star_off
            // Need_to_fix val bmp = BitmapFactory.decodeResource(resources, drawable)
            // Need_to_fix bitmap = Bitmap.createScaledBitmap(bmp, width, height, false)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.d("===>", "onDraw")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // paint.setColor(getResources().getColor(R.color.colorPrimary));
        paint.style = Paint.Style.FILL
        val rect = Rect(0, 0, width, height)

        bitmap?.let {
            canvas?.drawBitmap(it, rect, rect, paint)
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        Log.d("===>", "onTouch")
        if (event?.action == MotionEvent.ACTION_DOWN) {
            toggle()

            if (onToggleListener != null) {
                onToggleListener?.onToggle(isOn)
            }
            return true
        }
        return false
    }

    fun toggle() {
        isOn = !isOn
        requestLayout()
        invalidate()
    }

    interface OnToggleListener {
        fun onToggle(isOn: Boolean)
    }
}
