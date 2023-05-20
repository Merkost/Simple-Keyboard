package com.simplemobiletools.keyboard.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.util.TypedValue
import android.util.Xml
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
import androidx.annotation.XmlRes
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard consists of rows of keys.
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 */
class MyKeyboard {
    /** Horizontal gap default for all rows  */
    internal var mDefaultHorizontalGap = 0

    /** Default key width  */
    internal var mDefaultWidth = 0

    /** Default key height  */
    private var mDefaultHeight = 0

    /** Multiplier for the keyboard height */
    var mKeyboardHeightMultiplier: Float = 1F

    /** Is the keyboard in the shifted state  */
    var mShiftState = ShiftState.OFF

    /** Total height of the keyboard, including the padding and keys  */
    var mHeight = 0

    /** Total width of the keyboard, including left side gaps and keys, but not any gaps on the right side. */
    var mMinWidth = 0

    /** List of keys in this keyboard  */
    var mKeys: MutableList<Key?>? = null

    /** Width of the screen available to fit the keyboard  */
    internal var mDisplayWidth = 0

    /** What icon should we show at Enter key  */
    private var mEnterKeyType = IME_ACTION_NONE

    /** Keyboard rows  */
    private val mRows = ArrayList<Row?>()

    companion object {
        private const val TAG_KEYBOARD = "Keyboard"
        private const val TAG_ROW = "Row"
        private const val TAG_KEY = "Key"
        const val EDGE_LEFT = 0x01
        const val EDGE_RIGHT = 0x02
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_MODE_CHANGE = -2
        const val KEYCODE_ENTER = -4
        const val KEYCODE_DELETE = -5
        const val KEYCODE_SPACE = 32
        const val KEYCODE_EMOJI = -6

        fun getDimensionOrFraction(a: TypedArray, index: Int, base: Int, defValue: Int): Int {
            val value = a.peekValue(index) ?: return defValue
            return when (value.type) {
                TypedValue.TYPE_DIMENSION -> a.getDimensionPixelOffset(index, defValue)
                TypedValue.TYPE_FRACTION -> Math.round(a.getFraction(index, base, base, defValue.toFloat()))
                else -> defValue
            }
        }
    }

    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows that have a keyboard mode defined but don't match the specified mode.
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param enterKeyType determines what icon should we show on Enter key
     */
    @JvmOverloads
    constructor(context: Context, @XmlRes xmlLayoutResId: Int, enterKeyType: Int) {
        mDisplayWidth = context.resources.displayMetrics.widthPixels
        mDefaultHorizontalGap = 0
        mDefaultWidth = mDisplayWidth / 10
        mDefaultHeight = mDefaultWidth
        mKeyboardHeightMultiplier = getKeyboardHeightMultiplier(context.config.keyboardHeightMultiplier)
        mKeys = ArrayList()
        mEnterKeyType = enterKeyType
        loadKeyboard(context, context.resources.getXml(xmlLayoutResId))
    }

    /**
     * Creates a blank keyboard from the given resource file and populates it with the specified characters in left-to-right, top-to-bottom fashion,
     * using the specified number of columns. If the specified number of columns is -1, then the keyboard will fit as many keys as possible in each row.
     * @param context the application or service context
     * @param layoutTemplateResId the layout template file, containing no keys.
     * @param characters the list of characters to display on the keyboard. One key will be created for each character.
     * @param keyWidth the width of the popup key, make sure it is the same as the key itself
     */
    constructor(context: Context, layoutTemplateResId: Int, characters: CharSequence, keyWidth: Int) :
        this(context, layoutTemplateResId, 0) {
        var x = 0
        var y = 0
        var column = 0
        mMinWidth = 0
        val row = Row(this)
        row.defaultHeight = mDefaultHeight
        row.defaultWidth = keyWidth
        row.defaultHorizontalGap = mDefaultHorizontalGap
        mKeyboardHeightMultiplier = getKeyboardHeightMultiplier(context.config.keyboardHeightMultiplier)

        characters.forEachIndexed { index, character ->
            val key = Key(row)
            if (column >= MAX_KEYS_PER_MINI_ROW) {
                column = 0
                x = 0
                y += mDefaultHeight
                mRows.add(row)
                row.mKeys.clear()
            }

            key.x = x
            key.y = y
            key.label = character.toString()
            key.code = character.code
            column++
            x += key.width + key.gap
            mKeys!!.add(key)
            row.mKeys.add(key)
            if (x > mMinWidth) {
                mMinWidth = x
            }
        }
        mHeight = y + mDefaultHeight
        mRows.add(row)
    }

    fun setShifted(shiftState: ShiftState): Boolean {
        if (this.mShiftState != shiftState) {
            this.mShiftState = shiftState
            return true
        }
        return false
    }

    private fun createRowFromXml(res: Resources, parser: XmlResourceParser?): Row {
        return Row(res, this, parser)
    }

    private fun createKeyFromXml(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser?): Key {
        return Key(res, parent, x, y, parser)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadKeyboard(context: Context, parser: XmlResourceParser) {
        var inKey = false
        var inRow = false
        var row = 0
        var x = 0
        var y = 0
        var key: Key? = null
        var currentRow: Row? = null
        val res = context.resources
        try {
            var event: Int
            while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    when (parser.name) {
                        TAG_ROW -> {
                            currentRow = createRowFromXml(res, parser)
                            if (currentRow.isNumbersRow && !context.config.showNumbersRow) {
                                continue
                            }
                            inRow = true
                            x = 0
                            mRows.add(currentRow)
                        }

                        TAG_KEY -> {
                            if (currentRow?.isNumbersRow == true && !context.config.showNumbersRow) {
                                continue
                            }
                            inKey = true
                            key = createKeyFromXml(res, currentRow!!, x, y, parser)
                            if (context.config.showNumbersRow) {
                                // Removes numbers (i.e 0-9) from the popupCharacters if numbers row is enabled
                                key.apply {
                                    popupCharacters = popupCharacters?.replace(Regex("\\d+"), "")
                                    if (popupCharacters.isNullOrEmpty()) {
                                        popupResId = 0
                                    }
                                }

                            }
                            mKeys!!.add(key)
                            if (key.code == KEYCODE_ENTER) {
                                val enterResourceId = when (mEnterKeyType) {
                                    EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_search_vector
                                    EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_GO -> R.drawable.ic_arrow_right_vector
                                    EditorInfo.IME_ACTION_SEND -> R.drawable.ic_send_vector
                                    else -> R.drawable.ic_enter_vector
                                }
                                key.icon = context.resources.getDrawable(enterResourceId, context.theme)
                            }
                            currentRow.mKeys.add(key)
                        }

                        TAG_KEYBOARD -> {
                            parseKeyboardAttributes(res, parser)
                        }
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false
                        x += key!!.gap + key.width
                        if (x > mMinWidth) {
                            mMinWidth = x
                        }
                    } else if (inRow) {
                        inRow = false
                        y += currentRow!!.defaultHeight
                        row++
                    }
                }
            }
        } catch (e: Exception) {
        }
        mHeight = y
    }

    private fun parseKeyboardAttributes(res: Resources, parser: XmlResourceParser) {
        val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
        mDefaultWidth = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, mDisplayWidth, mDisplayWidth / 10)
        mDefaultHeight = res.getDimension(R.dimen.key_height).toInt()
        mDefaultHorizontalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, mDisplayWidth, 0)
        a.recycle()
    }

    private fun getKeyboardHeightMultiplier(multiplierType: Int): Float {
        return when (multiplierType) {
            KEYBOARD_HEIGHT_MULTIPLIER_SMALL -> 1.0F
            KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM -> 1.2F
            KEYBOARD_HEIGHT_MULTIPLIER_LARGE -> 1.4F
            else -> 1.0F
        }
    }
}
