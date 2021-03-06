package com.ubertob.pesticide.examples.calculator

import com.ubertob.pesticide.core.*
import java.util.concurrent.atomic.AtomicInteger


fun allCalculatorInterpreters() = setOf(
    DomainOnlyCalculator(),
    FakeHttpCalculator()
)

interface CalculatorInterpreter : DomainInterpreter<DdtProtocol> {
    fun addNumber(num: Int)
    fun subtractNumber(num: Int)

    fun getTotal(): Int
    fun startWithNumber(num: Int): CalculatorInterpreter
}


class DomainOnlyCalculator : CalculatorInterpreter {

    var tot = 0

    override fun addNumber(num: Int) {
        tot += num
    }

    override fun subtractNumber(num: Int) {
        tot -= num
    }

    override fun getTotal(): Int = tot

    override fun startWithNumber(num: Int): CalculatorInterpreter {
        tot = num
        return this
    }

    override val protocol = DomainOnly

    override fun prepare(): DomainSetUp =
        Ready

}


class FakeHttpCalculator : CalculatorInterpreter {

    val tot = AtomicInteger(0)

    override fun addNumber(num: Int) {
        tot.getAndUpdate { it + num }
    }

    override fun subtractNumber(num: Int) {
        TODO("subtractNumber not implemented in HTTP yet")
    }

    override fun getTotal(): Int = tot.get()
    override fun startWithNumber(num: Int): CalculatorInterpreter {
        tot.set(num)
        return this
    }

    override val protocol = Http("fake")

    override fun prepare(): DomainSetUp =
        Ready

}

