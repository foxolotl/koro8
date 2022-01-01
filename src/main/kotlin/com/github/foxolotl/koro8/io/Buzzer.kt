package com.github.foxolotl.koro8.io

import java.io.Closeable

interface Buzzer : Closeable {
    fun start()
    fun stop()
}
