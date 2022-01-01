package com.github.foxolotl.koro8

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

class Config(parser: ArgParser) {
    val keymap: String? by parser.option(
        type = ArgType.String,
        shortName = "k",
        description = "Comma-separated list of 'keycode:chip8-keycode' pairs.\n\t" +
            "For instance, a mapping of '32:1,48:f' would map the space key to CHIP-8 key 1\n\t" +
            "and the '0' key to CHIP-8 key F.\n\t" +
            "By default, QWER maps to 123C, ASDF to 456D, ZXCV to 789E, and 1234 to A0BF."
    )

    val foreground: String by parser.option(
        type = ArgType.String,
        shortName = "f",
        description = "Foreground color, in RGB hex notation."
    ).default("FFFFFF")

    val background: String by parser.option(
        type = ArgType.String,
        shortName = "b",
        description = "Background color, in RGB hex notation."
    ).default("000000")

    val scale: Int by parser.option(
        type = ArgType.Int,
        shortName = "s",
        description = "Ratio of CHIP-8 pixels to actual pixels."
    ).default(5)

    val multiplier: Int by parser.option(
        type = ArgType.Int,
        shortName = "m",
        description = "Value to multiply by the timer frequency (60 Hz)\n\tto obtain the CPU clock frequency."
    ).default(9)
    
    val rom: String by parser.argument(
        type = ArgType.String,
        description = "Path to .ch8 ROM file to load."
    )
}
