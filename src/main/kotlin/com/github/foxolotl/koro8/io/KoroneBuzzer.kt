package com.github.foxolotl.koro8.io

import java.io.Closeable
import java.io.InputStream
import javax.sound.sampled.AudioSystem
import kotlin.random.Random

class KoroneBuzzer(private val rng: Random = Random.Default) : Buzzer, Closeable {
    private val clips by lazy {
        files.map { file ->
            val path = "/com/github/foxolotl/koro8/korone/$file.wav"
            val korone: InputStream = javaClass.getResourceAsStream(path)!!.buffered()
            val audioInputStream = AudioSystem.getAudioInputStream(korone)
            AudioSystem.getClip().also { it.open(audioInputStream) }
        }
    }

    override fun start() {
        val eligibleClips = clips.filter { !it.isRunning }.takeIf { it.isNotEmpty() } ?: clips
        val clip = eligibleClips[rng.nextInt(eligibleClips.size)]
        clip.framePosition = 0
        clip.start()
    }

    override fun stop() {
        // can't stop the korone
    }

    override fun close() {
        clips.forEach {
            it.stop()
            it.close()
        }
    }
    
    fun preload() {
        clips.size
    }
    
    private companion object {
        private val files = listOf(
            "pyonpyon",
            "sneeze",
            "sneeze2",
            "yes",
            "assassin",
            "ehehu",
            "goddamn",
            "orayo1",
            "orayo2",
            "orayo3",
            "yubi",
            "yubiyubi"
        )
    }
}
