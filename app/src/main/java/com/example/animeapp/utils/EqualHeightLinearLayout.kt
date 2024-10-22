package com.example.animeappkotlin.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlin.math.max

class EqualHeightLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var maxHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != ConstraintSet.GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                maxHeight = max(maxHeight, child.measuredHeight)
            }
        }

        if (maxHeight > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility != ConstraintSet.GONE) {
                    val params = child.layoutParams
                    params.height = maxHeight
                    child.layoutParams = params
                }
            }

            setMeasuredDimension(measuredWidth, maxHeight)
        }
    }
}