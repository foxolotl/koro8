# koro8
A [CHIP-8](https://en.wikipedia.org/wiki/CHIP-8) emulator with
a [Korone](https://twitter.com/KoroneNoises/)-based sound chip.

## Usage
0. Make sure you have Java installed, at version 11. If not, get it from [adoptium.net](https://adoptium.net).
1. Download the JAR from the [latest release](https://github.com/foxolotl/koro8/releases/latest).
2. Get some [roms](https://github.com/loktar00/chip8/tree/master/roms) to run.
    The default key map is QWER to 123C, ASDF to 456D, ZXCV to 789E, and 1234 to A0BF.
    Most roms use 2, 4, 6 and 8 for directional input, which maps to W, A, D and X respectively.

    [Breakout](https://github.com/loktar00/chip8/raw/master/roms/Breakout%20%5BCarmelo%20Cortez%2C%201979%5D.ch8)
    is a great choice for experiencing the cutting-edge doggo sound chip of koro8.
    Using the default key map, the A and D keys control the paddle.
3. Run koro8 with the path to one of your downloaded roms as argument: `java -jar koro8-0.1.2-min.jar /path/to/some/rom.ch8`.
    You can also fine-tune various parts of koro8's appearance and behaviour.


See (and, more importantly, listen to) koro8 in action:

https://user-images.githubusercontent.com/96795329/147975418-6754fd83-af18-410a-ba8c-135dcf1e411f.mp4
