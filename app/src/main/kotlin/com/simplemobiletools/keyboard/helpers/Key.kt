package com.simplemobiletools.keyboard.helpers

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.util.Xml
import com.simplemobiletools.keyboard.R

/**
 * Class for describing the position and characteristics of a single key in the keyboard.
 *
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_keyHeight
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 * @attr ref android.R.styleable#Keyboard_Key_codes
 * @attr ref android.R.styleable#Keyboard_Key_keyIcon
 * @attr ref android.R.styleable#Keyboard_Key_keyLabel
 * @attr ref android.R.styleable#Keyboard_Key_isRepeatable
 * @attr ref android.R.styleable#Keyboard_Key_popupKeyboard
 * @attr ref android.R.styleable#Keyboard_Key_popupCharacters
 * @attr ref android.R.styleable#Keyboard_Key_keyEdgeFlags
 */
class Key(parent: Row) {
    /** Key code that this key generates.  */
    var code = 0

    /** Label to display  */
    var label: CharSequence = ""

    /** First row of letters can also be used for inserting numbers by long pressing them, show those numbers  */
    var topSmallNumber: String = ""

    /** Icon to display instead of a label. Icon takes precedence over a label  */
    var icon: Drawable? = null

    /** Icon to display top left of an icon.*/
    var secondaryIcon: Drawable? = null

    /** Width of the key, not including the gap  */
    var width: Int

    /** Height of the key, not including the gap  */
    var height: Int

    /** The horizontal gap before this key  */
    var gap: Int

    /** X coordinate of the key in the keyboard layout  */
    var x = 0

    /** Y coordinate of the key in the keyboard layout  */
    var y = 0

    /** The current pressed state of this key  */
    var pressed = false

    /** Focused state, used after long pressing a key and swiping to alternative keys  */
    var focused = false

    /** Popup characters showing after long pressing the key  */
    var popupCharacters: CharSequence? = null

    /**
     * Flags that specify the anchoring to edges of the keyboard for detecting touch events that are just out of the boundary of the key.
     * This is a bit mask of [MyKeyboard.EDGE_LEFT], [MyKeyboard.EDGE_RIGHT].
     */
    private var edgeFlags = 0

    /** The keyboard that this key belongs to  */
    private val keyboard = parent.parent

    /** If this key pops up a mini keyboard, this is the resource id for the XML layout for that keyboard.  */
    var popupResId = 0

    /** Whether this key repeats itself when held down  */
    var repeatable = false

    /** Create a key with the given top-left coordinate and extract its attributes from the XML parser.
     * @param res resources associated with the caller's context
     * @param parent the row that this key belongs to. The row must already be attached to a [MyKeyboard].
     * @param x the x coordinate of the top-left
     * @param y the y coordinate of the top-left
     * @param parser the XML parser containing the attributes for this key
     */
    constructor(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser?) : this(parent) {
        this.x = x
        this.y = y
        var a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
        width = MyKeyboard.getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, keyboard.mDisplayWidth, parent.defaultWidth)
        height = parent.defaultHeight
        gap = MyKeyboard.getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, keyboard.mDisplayWidth, parent.defaultHorizontalGap)
        this.x += gap

        a.recycle()
        a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard_Key)
        code = a.getInt(R.styleable.MyKeyboard_Key_code, 0)

        popupCharacters = a.getText(R.styleable.MyKeyboard_Key_popupCharacters)
        popupResId = a.getResourceId(R.styleable.MyKeyboard_Key_popupKeyboard, 0)
        repeatable = a.getBoolean(R.styleable.MyKeyboard_Key_isRepeatable, false)
        edgeFlags = a.getInt(R.styleable.MyKeyboard_Key_keyEdgeFlags, 0)
        icon = a.getDrawable(R.styleable.MyKeyboard_Key_keyIcon)
        icon?.setBounds(0, 0, icon!!.intrinsicWidth, icon!!.intrinsicHeight)

        secondaryIcon = a.getDrawable(R.styleable.MyKeyboard_Key_secondaryKeyIcon)
        secondaryIcon?.setBounds(0, 0, secondaryIcon!!.intrinsicWidth, secondaryIcon!!.intrinsicHeight)

        label = a.getText(R.styleable.MyKeyboard_Key_keyLabel) ?: ""
        topSmallNumber = a.getString(R.styleable.MyKeyboard_Key_topSmallNumber) ?: ""

        if (label.isNotEmpty() && code != MyKeyboard.KEYCODE_MODE_CHANGE && code != MyKeyboard.KEYCODE_SHIFT) {
            code = label[0].code
        }
        a.recycle()
    }

    /** Create an empty key with no attributes.  */
    init {
        height = parent.defaultHeight
        width = parent.defaultWidth
        gap = parent.defaultHorizontalGap
    }

    /**
     * Detects if a point falls inside this key.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return whether or not the point falls inside the key. If the key is attached to an edge, it will assume that all points between the key and
     * the edge are considered to be inside the key.
     */
    fun isInside(x: Int, y: Int): Boolean {
        val leftEdge = edgeFlags and MyKeyboard.EDGE_LEFT > 0
        val rightEdge = edgeFlags and MyKeyboard.EDGE_RIGHT > 0
        return (
            (x >= this.x || leftEdge && x <= this.x + width) &&
                (x < this.x + width || rightEdge && x >= this.x) &&
                (y >= this.y && y <= this.y + height) &&
                (y < this.y + height && y >= this.y)
            )
    }
}
