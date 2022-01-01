@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("NOTHING_TO_INLINE")

package com.github.foxolotl.koro8

import com.github.foxolotl.koro8.io.Buzzer
import com.github.foxolotl.koro8.io.Display
import com.github.foxolotl.koro8.io.Keyboard
import java.io.Closeable
import java.util.Arrays
import kotlin.random.Random
import kotlin.random.nextUInt

class CPU(
    private val display: Display,
    private val keyboard: Keyboard,
    private val buzzer: Buzzer,
    private val resetSignal: (() -> Boolean)? = null,
    private val clockMultiplier: Int = 9,
    private val rng: Random = Random.Default,
    private var font: Font = Font.default,
) : Closeable {
    private val cyclesPerSecond: Long = clockMultiplier * TIMER_HZ
    private val cycleTimeNanos: Long = 1000000000L / cyclesPerSecond
    private val cycleSleepMillis: Long = cycleTimeNanos / 1000000L
    private var cycles: Long = 0
    private var nextCycleDeadline: Long = 0

    private val heap: Heap = Heap(HEAP_SIZE)
    private val stack: Stack = Stack(STACK_SIZE)
    private var regs: Registers = Registers()
    private var loadedRom: Heap? = null

    init {
        font.data.copyInto(heap, 0)
    }

    private inner class Registers(
        val V: DataRegisters = DataRegisters(DATA_REGISTERS),
        var I: Address = 0u,
        var SP: Word8 = 0u,
        var PC: Word16 = 0x200u,
        var DT: Word8 = 0u,
        var ST_val: Word8 = 0u,
    ) {
        var VF: Word8
            get() = V[LAST_DATA_REGISTER]
            set(value) { V[LAST_DATA_REGISTER] = value }

        var V0: Word8
            get() = V[0]
            set(value) { V[0] = value }

        var ST: Word8
            get() = ST_val
            set(value) {
                if (ST_val <= 0u && value > 0u) {
                    buzzer.start()
                } else if (ST_val > 0u && value <= 0u) {
                    buzzer.stop()
                }
                ST_val = value
            }

        val reg0 = Register(0u)
    }
    
    fun reset() {
        cycles = 0
        regs = Registers()
        Arrays.fill(heap.asByteArray(), 0)
        Arrays.fill(stack.asShortArray(), 0)
        font.data.copyInto(heap, 0)
        loadedRom?.copyInto(heap, 0x200)
        display.clear()
    }

    fun run(ticks: Long = Long.MAX_VALUE) {
        nextCycleDeadline = System.nanoTime()
        execute(ticks)
    }

    private tailrec fun execute(ticks: Long) {
        if (ticks == 0L) {
            return
        }
        if (resetSignal?.invoke() == true) {
            reset()
        }
        val now = System.nanoTime()
        if (now >= nextCycleDeadline) {
            step()
            nextCycleDeadline += cycleTimeNanos
            execute(ticks - 1)
        } else {
            Thread.sleep(cycleSleepMillis)
            execute(ticks)
        }
    }
    
    fun load(rom: ByteArray) {
        loadedRom = rom.toUByteArray()
        reset()
    }

    private fun step() {
        if (cycles % clockMultiplier == 0L) {
            if (regs.DT != 0u.toUByte()) {
                regs.DT = (regs.DT - 1u).overflow8()
            }
            if (regs.ST != 0u.toUByte()) {
                regs.ST = (regs.ST - 1u).overflow8()
            }
        }
        val instr = heap[regs.PC].toUShort().shl(8) or heap[(regs.PC + 1u).toAddr()].toUShort()
        regs.PC = (regs.PC + 2u).overflow16()
        interpret(instr)
        cycles += 1
    }
    
    private fun interpret(instruction: Instruction) {
        val i = instruction.toInt()
        when {
            i == 0x00E0 -> display.clear()
            i == 0x00EE -> ret()
            i and 0xF000 == 0x0000 -> { /* SYS addr - ignored */ }
            i and 0xF000 == 0x1000 -> jp(instruction.addr)
            i and 0xF000 == 0x2000 -> call(instruction.addr)
            i and 0xF000 == 0x3000 -> se(instruction.x, instruction.byte)
            i and 0xF000 == 0x4000 -> sne(instruction.x, instruction.byte)
            i and 0xF000 == 0x5000 -> se(instruction.x, instruction.y)
            i and 0xF000 == 0x6000 -> ld(instruction.x, instruction.byte)
            i and 0xF000 == 0x7000 -> add(instruction.x, instruction.byte)
            i and 0xF00F == 0x8000 -> ld(instruction.x, instruction.y)
            i and 0xF00F == 0x8001 -> bitOp(Word8::or, instruction.x, instruction.y)
            i and 0xF00F == 0x8002 -> bitOp(Word8::and, instruction.x, instruction.y)
            i and 0xF00F == 0x8003 -> bitOp(Word8::xor, instruction.x, instruction.y)
            i and 0xF00F == 0x8004 -> add(instruction.x, instruction.y)
            i and 0xF00F == 0x8005 -> sub(instruction.x, instruction.y)
            i and 0xF00F == 0x8006 -> shr(instruction.x)
            i and 0xF00F == 0x8007 -> subn(instruction.x, instruction.y)
            i and 0xF00F == 0x800E -> shl(instruction.x)
            i and 0xF00F == 0x9000 -> sne(instruction.x, instruction.y)
            i and 0xF000 == 0xA000 -> ldi(instruction.addr)
            i and 0xF000 == 0xB000 -> jpv0(instruction.addr)
            i and 0xF000 == 0xC000 -> rnd(instruction.x, instruction.byte)
            i and 0xF000 == 0xD000 -> drw(instruction.x, instruction.y, instruction.n)
            i and 0xF0FF == 0xE09E -> skp(true, instruction.x)
            i and 0xF0FF == 0xE0A1 -> skp(false, instruction.x)
            i and 0xF0FF == 0xF007 -> stdt(instruction.x)
            i and 0xF0FF == 0xF00A -> ldk(instruction.x)
            i and 0xF0FF == 0xF015 -> lddt(instruction.x)
            i and 0xF0FF == 0xF018 -> ldst(instruction.x)
            i and 0xF0FF == 0xF01E -> ldi((regs.I + regs.V[instruction.x]).toAddr())
            i and 0xF0FF == 0xF029 -> ldi((regs.V[instruction.x] * 5u).toAddr())
            i and 0xF0FF == 0xF033 -> bcd(instruction.x)
            i and 0xF0FF == 0xF055 -> sth(instruction.x)
            i and 0xF0FF == 0xF065 -> ldh(instruction.x)
        }
    }

    private inline fun ldk(reg: Register) {
        val t0 = System.nanoTime()
        val key = keyboard.waitKey()
        val t1 = System.nanoTime()
        nextCycleDeadline += (t1 - t0)
        ld(reg, key)
    }
    private inline fun stdt(reg: Register) = ld(reg, regs.DT)
    private inline fun ld(reg1: Register, reg2: Register) = ld(reg1, regs.V[reg2])
    private inline fun sne(reg1: Register, reg2: Register) = sne(reg1, regs.V[reg2])
    private inline fun se(reg1: Register, reg2: Register) = se(reg1, regs.V[reg2])
    private inline fun jpv0(addr: Address) = jp((addr + regs.V0).toAddr())

    private fun sth(reg: Register) {
        (regs.reg0 .. reg).zip(regs.I .. LAST_ADDR) { r, addr ->
            heap[addr.toAddr()] = regs.V[r]
        }
    }

    private fun ldh(reg: Register) {
        (regs.reg0 .. reg).zip(regs.I .. LAST_ADDR) { r, addr ->
            regs.V[r] = heap[addr.toAddr()]
        }
    }

    private inline fun bcd(reg: Register) {
        val value = regs.V[reg]
        heap[regs.I] = (value / 100u).overflow8()
        heap[(regs.I + 1u).toAddr()] = ((value % 100u) / 10u).overflow8()
        heap[(regs.I + 2u).toAddr()] = (value % 10u).overflow8()
    }

    private inline fun skp(skipOnState: Boolean, reg: Register) {
        if (keyboard.pressed(regs.V[reg]) == skipOnState) {
            regs.PC = (regs.PC + 2u).overflow16()
        }
    }

    private inline fun drw(x: Register, y: Register, n: Word4) {
        val collision = display.draw(heap, regs.I, n, regs.V[x], regs.V[y])
        regs.VF = if (collision) 1u else 0u
    }

    private inline fun rnd(reg: Register, byte: Word8) {
        val random = rng.nextUInt().overflow8()
        regs.V[reg] = random and byte
    }

    private inline fun shl(reg: Register) {
        val value = regs.V[reg]
        regs.VF = value.toUInt().shr(7).overflow8()
        regs.V[reg] = (value * 2u).overflow8()
    }

    private inline fun shr(reg: Register) {
        val value = regs.V[reg]
        regs.VF = value and 1u
        regs.V[reg] = (value / 2u).overflow8()
    }

    private inline fun subn(reg1: Register, reg2: Register) {
        val result = regs.V[reg2] - regs.V[reg1]
        regs.VF = if (regs.V[reg2] > regs.V[reg1]) 1u else 0u
        regs.V[reg1] = result.overflow8()
    }

    private inline fun sub(reg1: Register, reg2: Register) {
        val result = regs.V[reg1] - regs.V[reg2]
        regs.VF = if (regs.V[reg1] > regs.V[reg2]) 1u else 0u
        regs.V[reg1] = result.overflow8()
    }

    private inline fun add(reg1: Register, reg2: Register) {
        val result = regs.V[reg1] + regs.V[reg2]
        regs.VF = result.wouldOverflow8()
        regs.V[reg1] = result.overflow8()
    }

    private inline fun add(reg: Register, byte: Word8) {
        regs.V[reg] = (regs.V[reg] + byte).overflow8()
    }

    private inline fun bitOp(crossinline op: (Word8, Word8) -> Word8, reg1: Register, reg2: Register) {
        regs.V[reg1] = op(regs.V[reg1], regs.V[reg2])
    }

    private inline fun ld(reg: Register, byte: Word8) {
        regs.V[reg] = byte
    }

    private inline fun ldi(addr: Address) {
        regs.I = addr
    }

    private inline fun lddt(reg: Register) {
        regs.DT = regs.V[reg]
    }

    private inline fun ldst(reg: Register) {
        regs.ST = regs.V[reg]
    }

    private inline fun sne(reg: Register, byte: Word8) {
        if (regs.V[reg] != byte) {
            regs.PC = (regs.PC + 2u).overflow16()
        }
    }

    private inline fun se(reg: Register, byte: Word8) {
        if (regs.V[reg] == byte) {
            regs.PC = (regs.PC + 2u).overflow16()
        }
    }

    private inline fun call(addr: Address) {
        stack[regs.SP] = regs.PC
        regs.SP = (regs.SP + 1u).overflow8()
        regs.PC = addr
    }

    private inline fun jp(addr: Address) {
        regs.PC = addr
    }

    private inline fun ret() {
        regs.SP = (regs.SP - 1u).overflow8()
        regs.PC = stack[regs.SP]
    }

    override fun close() {
        display.close()
        buzzer.close()
        keyboard.close()
    }
}

private operator fun Register.rangeTo(other: Register) =
    (reg .. other.reg).map { Register(it.toUByte()) }

private operator fun Heap.get(addr: Address): Word8 = get(addr.toInt())
private operator fun Heap.set(addr: Address, x: Word8) = set(addr.toInt(), x)

private operator fun DataRegisters.get(reg: Register): UByte = get(reg.reg.toInt())
private operator fun DataRegisters.set(reg: Register, x: Word8) = set(reg.reg.toInt(), x)

private operator fun Stack.get(sp: Word8): UShort = get(sp.toInt())
private operator fun Stack.set(sp: Word8, x: Address) = set(sp.toInt(), x)

private fun UInt.wouldOverflow8(): Word8 = if (this in 0u .. 0xFFu) 0u else 1u
private fun UInt.overflow8(): Word8 = toUByte()
private fun UInt.overflow16(): Word16 = toUShort()
private fun UInt.toAddr(): Address = toUShort()

private val Instruction.addr: Address
    get() = this and 0x0FFFu

private fun Word16.shl(i: Int): Word16 = this.toUInt().shl(i).toUShort()
private fun Word16.shr(i: Int): Word16 = this.toUInt().shr(i).toUShort()

private val Instruction.x: Register
    get() = Register((this and 0x0F00u).shr(8).toUByte())

private val Instruction.y: Register
    get() = Register((this and 0x00F0u).shr(4).toUByte())

private val Instruction.n: Word4
    get() = (this and 0x000Fu).toUByte()

private val Instruction.byte: Word8
    get() = this.toUByte()
