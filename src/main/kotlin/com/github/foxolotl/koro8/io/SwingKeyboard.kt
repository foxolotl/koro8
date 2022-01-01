package com.github.foxolotl.koro8.io

import com.github.foxolotl.koro8.NUM_KEYS
import com.github.foxolotl.koro8.Word8
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JPanel

class SwingKeyboard(private val keymap: Map<Int, Int> = DEFAULT_KEYMAP) : Keyboard, KeyListener {
    private val keyState: BooleanArray = BooleanArray(NUM_KEYS)
    private var awaitedKey: Word8? = null
    private var reset: Boolean = false

    override fun keyTyped(event: KeyEvent) { }

    override fun keyPressed(event: KeyEvent) {
        if (event.keyCode == 27) {
            reset = true
        }
        keymap[event.keyCode]?.let {
            awaitedKey = it.toUByte()
            keyState[it] = true
        }
    }

    override fun keyReleased(event: KeyEvent) {
        keymap[event.keyCode]?.let {
            keyState[it] = false
        }
    }

    override fun pressed(key: Word8): Boolean = keyState[key.toInt()]

    override fun waitKey(): Word8 {
        awaitedKey = null
        while (awaitedKey == null) {
            Thread.sleep(2)
        }
        return awaitedKey!!
    }

    override fun close() {
        awaitedKey = 0u
    }

    fun resetSignal(): Boolean = reset.also {
        reset = false
    }

    companion object {
        /**
         * Maps QWER to 123C, ASDF to 456D, ZXCV to 789E, and 1234 to A0BF.
         */
        val DEFAULT_KEYMAP: Map<Int, Int> = mapOf(
            81 to 0x1,
            87 to 0x2,
            69 to 0x3,
            65 to 0x4,
            83 to 0x5,
            68 to 0x6,
            90 to 0x7,
            88 to 0x8,
            67 to 0x9,
            82 to 0xC,
            70 to 0xD,
            86 to 0xE,
            49 to 0xA,
            50 to 0x0,
            51 to 0xB,
            52 to 0xF,
        )
    }
}

fun JPanel.keyboard(keymap: Map<Int, Int> = SwingKeyboard.DEFAULT_KEYMAP): SwingKeyboard {
    val keyboard = SwingKeyboard(keymap)
    this.addKeyListener(keyboard)
    this.requestFocus()
    return keyboard
}
