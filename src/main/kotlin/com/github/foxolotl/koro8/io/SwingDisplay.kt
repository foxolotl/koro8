package com.github.foxolotl.koro8.io

import com.github.foxolotl.koro8.Address
import com.github.foxolotl.koro8.Heap
import com.github.foxolotl.koro8.Word4
import com.github.foxolotl.koro8.Word8
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.JFrame
import javax.swing.JPanel

class SwingDisplay(
    private val scale: Int,
    private val fgColor: Int = Color.WHITE.rgb,
    private val bgColor: Int = Color.BLACK.rgb
) : Display, JPanel() {
    private val image: BufferedImage = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    private val frame: JFrame = JFrame().also {
        preferredSize = Dimension(WIDTH * scale, HEIGHT * scale)
        it.add(this)
        it.pack()
        it.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        it.isVisible = true
    }

    override fun clear() {
        val buf = (image.raster.dataBuffer as DataBufferInt).data
        buf.forEachIndexed { index, _ ->
           buf[index] = bgColor
        }
        repaint()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun draw(heap: Heap, addr: Address, size: Word4, x: Word8, y: Word8): Boolean {
        var collision = false
        val buf = (image.raster.dataBuffer as DataBufferInt).data
        val x0 = x.toInt()
        val y0 = y.toInt()
        val address = addr.toInt()
        (0 until size.toInt()).forEach { yOffset ->
            val row = heap[address + yOffset].toInt()
            (0..7).forEach { xOffset ->
                val px = row.shr(7 - xOffset) and 1 == 1
                val oldPx = buf[y0 + yOffset, x0 + xOffset]
                if (oldPx && px) {
                    collision = true
                }
                buf[y0 + yOffset, x0 + xOffset] = oldPx xor px
            }
        }
        repaint()
        return collision
    }

    override fun close() {
        frame.dispose()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g?.drawImage(image, 0, 0, WIDTH * scale, HEIGHT * scale, null)
    }

    private operator fun IntArray.get(y: Int, x: Int): Boolean = get((y % HEIGHT) * WIDTH + (x % WIDTH)) == fgColor
    private operator fun IntArray.set(y: Int, x: Int, value: Boolean) =
        set((y % HEIGHT) * WIDTH + (x % WIDTH), if (value) fgColor else bgColor)

    companion object {
        private const val WIDTH: Int = 64
        private const val HEIGHT: Int = 32
    }
}
