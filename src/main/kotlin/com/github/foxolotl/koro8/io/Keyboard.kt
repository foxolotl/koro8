package com.github.foxolotl.koro8.io

import com.github.foxolotl.koro8.Word8
import java.io.Closeable

interface Keyboard : Closeable {
    fun pressed(key: Word8): Boolean
    fun waitKey(): Word8
}