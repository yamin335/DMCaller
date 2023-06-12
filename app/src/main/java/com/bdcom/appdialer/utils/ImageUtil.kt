package com.bdcom.appdialer.utils

import com.amulyakhare.textdrawable.TextDrawable

class ImageUtil {

    companion object {

        private var shapeBuilder: TextDrawable.IShapeBuilder? = null

        fun circularImage(name: String): TextDrawable {
            val color = AndroidUtil.getRandomColor()
            return circularImage(name, color)
        }

        fun circularImage(name: String, color: Int): TextDrawable {
            val nameLetters = getNameLetters(name)
            return getTextDrawableBuilder()!!.buildRound(nameLetters, color)
        }

        private fun getTextDrawableBuilder(): TextDrawable.IShapeBuilder? {
            if (shapeBuilder == null) {
                shapeBuilder = TextDrawable.builder()
            }
            return shapeBuilder
        }

        private fun getNameLetters(name: String): String {
            if (name.isEmpty())
                return "#"

            val strings = name.split(" ")
            val nameLetters = StringBuilder()
            for (s in strings) {
                if (nameLetters.length >= 2)
                    return nameLetters.toString().toUpperCase()
                if (!s.isEmpty()) {
                    nameLetters.append(s.trim { it <= ' ' }[0])
                }
            }
            return nameLetters.toString().toUpperCase()
        }
    }
}
