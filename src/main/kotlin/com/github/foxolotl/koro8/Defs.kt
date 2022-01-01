@file:OptIn(ExperimentalUnsignedTypes::class)

package com.github.foxolotl.koro8

typealias Word4 = UByte
typealias Word8 = UByte
typealias Word12 = UShort
typealias Word16 = UShort

@JvmInline
value class Register(val reg: Word4)
typealias Instruction = Word16
typealias Address = Word12

typealias Word8Array = UByteArray
typealias Word12Array = UShortArray

typealias Heap = Word8Array
typealias Stack = Word12Array
typealias DataRegisters = Word8Array

const val HEAP_SIZE: Int = 4096
val LAST_ADDR: Address = (HEAP_SIZE - 1).toUShort()

const val DATA_REGISTERS: Int = 16
val LAST_DATA_REGISTER: Register = Register((DATA_REGISTERS - 1).toUByte())

const val STACK_SIZE: Int = 16
const val NUM_KEYS: Int = 16

const val TIMER_HZ: Long = 60L