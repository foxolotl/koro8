package com.github.foxolotl.koro8.io

import com.github.foxolotl.koro8.Address
import com.github.foxolotl.koro8.Heap
import com.github.foxolotl.koro8.Word4
import com.github.foxolotl.koro8.Word8
import java.io.Closeable

@OptIn(ExperimentalUnsignedTypes::class)
interface Display : Closeable {
    fun clear()
    fun draw(heap: Heap, addr: Address, size: Word4, x: Word8, y: Word8): Boolean
}
