package com.ubertob.pesticide

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.stream.Stream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.streams.asStream


typealias DDT = TestFactory

data class Setting<D : DomainUnderTest<*>>(val setUp: DdtStep<D>)

abstract class DomainDrivenTest<D : DomainUnderTest<*>>(val domains: Sequence<D>) {

    fun play(vararg stepsArray: DdtStep<D>): Scenario<D> =
        Scenario(stepsArray.toList())

    fun Scenario<D>.wip(
        dueDate: LocalDate,
        reason: String = "Work In Progress",
        except: Set<KClass<out DdtProtocol>> = emptySet()
    ): Scenario<D> =
        this.copy(wipData = WipData(dueDate, except, reason))

    val timeoutInMillis = 1000


    fun ddtScenario(
        block: D.() -> Scenario<D>
    ): Stream<out DynamicNode> =
        domains.map(dynamicContainerBuilder(block)).asStream()

    private fun dynamicContainerBuilder(
        block: D.() -> Scenario<D>
    ): (D) -> DynamicContainer = { domain ->
        assertTrue(domain.isReady(), "Protocol ${domain.protocol.desc} ready")

        val tests = trapUnexpectedExceptions {
            block(domain).createTests(domain)
        }
        dynamicContainer("running ${domain.description()}", tests.asStream())
    }

    private fun <T : Any> trapUnexpectedExceptions(block: () -> T): T =
        try {
            block()
        } catch (t: Throwable) {
            fail(
                "Unexpected Exception while initializing the tests. Have you forgotten to use executeStep in your steps? ",
                t
            )
        }

    val D.withoutSetting: Setting<D>
        get() = Setting(DdtStep("empty stage") { it })

    fun <D : DomainUnderTest<*>> D.setting(
        block: D.() -> D
    ): Setting<D> = Setting(DdtStep("Preparing", block))


    infix fun Setting<D>.atRise(steps: Scenario<D>): Scenario<D> =
        Scenario(listOf(this.setUp) + steps.steps, steps.wipData) //add source URL

    fun DomainUnderTest<*>.description(): String = "${javaClass.simpleName} - ${protocol.desc}"

}
//
//private fun RootContextBuilder.toMyTestFactory(): Stream<out DynamicNode> = this.buildNode().let {
//    when (it) {
//        is Context<*, *> -> this.toStreamOfDynamicNodes(executor)
//        is Test<*> -> Stream.of(this.toDynamicNode(executor))
//    }
//}
//
//private fun Node<Unit>.toDynamicNode(): DynamicNode = when (this) {
//    is Test<Unit> -> DynamicTest.dynamicTest(name, this.testUri()) {
//        this.invoke(Unit, "ciao")
//    }
//    is Context<Unit, *> -> DynamicContainer.dynamicContainer(
//        name,
//        this.testUri(),
//        this.toStreamOfDynamicNodes(executor)
//    )
//}
//
//private fun <F> Node<F>.testUri(): URI? {
//    val p = Thread.currentThread().stackTrace.last()
//    return p.toSourceReference(File(p.fileName))?.toURI()
//}
//
//private fun SourceReference.toURI(): URI = File(path).toURI().let { fileUri ->
//    URI(
//        fileUri.scheme,
//        fileUri.userInfo,
//        fileUri.host,
//        fileUri.port,
//        "//" + fileUri.path,
//        "line=$lineNumber",
//        fileUri.fragment
//    )
//}
//
//private fun StackTraceElement.toSourceReference(sourceRoot: File): SourceReference? {
//    val fileName = fileName ?: return null
//    val type = Class.forName(className)
//    return SourceReference(
//        sourceRoot.toPath().resolve(type.`package`.name.replace(".", "/")).resolve(fileName).toFile().absolutePath,
//        lineNumber
//    )
//}

class ActorDelegate<D : DomainUnderTest<*>, A : DdtActor<D>>(val actorConstructor: (String) -> A) :
    ReadOnlyProperty<DomainDrivenTest<D>, A> {
    override operator fun getValue(thisRef: DomainDrivenTest<D>, property: KProperty<*>): A =
        actorConstructor(property.name.capitalize())

}