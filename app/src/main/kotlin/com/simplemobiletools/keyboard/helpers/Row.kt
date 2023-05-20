package com.simplemobiletools.keyboard.helpers

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Xml
import com.simplemobiletools.keyboard.R
import kotlin.math.roundToInt

/**
 * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate. Some of the key size defaults can be overridden per row from
 * what the [MyKeyboard] defines.
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 */
class Row {
    /** Default width of a key in this row.  */
    var defaultWidth = 0

    /** Default height of a key in this row.  */
    var defaultHeight = 0

    /** Default horizontal gap between keys in this row.  */
    var defaultHorizontalGap = 0

    var mKeys = ArrayList<Key>()

    var parent: MyKeyboard

    var isNumbersRow: Boolean = false

    constructor(parent: MyKeyboard) {
        this.parent = parent
    }

    constructor(res: Resources, parent: MyKeyboard, parser: XmlResourceParser?) {
        this.parent = parent
        val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
        defaultWidth = MyKeyboard.getDimensionOrFraction(
            a,
            R.styleable.MyKeyboard_keyWidth,
            parent.mDisplayWidth,
            parent.mDefaultWidth
        )
        defaultHeight = (res.getDimension(R.dimen.key_height) * this.parent.mKeyboardHeightMultiplier).roundToInt()
        defaultHorizontalGap = MyKeyboard.getDimensionOrFraction(
            a,
            R.styleable.MyKeyboard_horizontalGap,
            parent.mDisplayWidth,
            parent.mDefaultHorizontalGap
        )
        isNumbersRow = a.getBoolean(R.styleable.MyKeyboard_isNumbersRow, false)
        a.recycle()
    }
}
