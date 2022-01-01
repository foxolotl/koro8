package com.github.foxolotl.koro8

import com.github.foxolotl.koro8.io.KoroneBuzzer
import com.github.foxolotl.koro8.io.SwingDisplay
import com.github.foxolotl.koro8.io.SwingKeyboard
import com.github.foxolotl.koro8.io.keyboard
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.system.exitProcess
import kotlinx.cli.ArgParser

fun main(args: Array<String>) {
    val parser = ArgParser("koro8")
    val config = Config(parser)
    parser.parse(args)
    val keymap = config.keymap?.let { parseKeymap(it) } ?: emptyMap()
    
    val rom = loadRom(config.rom)

    val display = SwingDisplay(
        scale = config.scale,
        fgColor = config.foreground.toInt(16),
        bgColor = config.background.toInt(16),
    )
    val keyboard = display.keyboard(SwingKeyboard.DEFAULT_KEYMAP + keymap)
    val buzzer = KoroneBuzzer().also { it.preload() }

    CPU(display, keyboard, buzzer, keyboard::resetSignal, config.multiplier).use { cpu ->
        cpu.load(rom)
        cpu.run()
    }
}

private fun loadRom(file: String): ByteArray {
    val romData = try {
        Path.of(file).inputStream().use {
            it.readAllBytes()
        }
    } catch (e: IOException) {
        System.err.println("Unable to load ROM '$file'")
        exitProcess(1)
    }
    
    if (romData.size > 3584) {
        System.err.println("ROM '$file' is too big to fit in memory (${romData.size} > 3548)")
        exitProcess(1)
    }
    
    return romData
}

private fun parseKeymap(keymap: String): Map<Int, Int> =
    keymap.split(',').associate {
        val (key, value) = it.split(':')
        key.toInt(10) to value.toInt(16)
    }
