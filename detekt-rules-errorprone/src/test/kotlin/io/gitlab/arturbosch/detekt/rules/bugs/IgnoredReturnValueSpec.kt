package io.gitlab.arturbosch.detekt.rules.bugs

import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.gitlab.arturbosch.detekt.test.lintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class IgnoredReturnValueSpec(private val env: KotlinCoreEnvironment) {

    @Nested
    inner class `default config with non-annotated return values` {
        private val subject = IgnoredReturnValue()

        @Test
        fun `does not report when a function which returns a value is called and the return is ignored`() {
            val code = """
                fun foo() {
                    listOf("hello")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which returns a value is called before a valid return`() {
            val code = """
                fun foo() : Int {
                    listOf("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which returns a value is called in chain and the return is ignored`() {
            val code = """
                fun foo() {
                    listOf("hello").isEmpty().not()
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which returns a value is called before a semicolon`() {
            val code = """
                fun foo() {
                    listOf("hello");println("foo")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which returns a value is called after a semicolon`() {
            val code = """
                fun foo() {
                    println("foo");listOf("hello")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which returns a value is called between comments`() {
            val code = """
                fun foo() {
                    listOf("hello")//foo
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when an extension function which returns a value is called and the return is ignored`() {
            val code = """
                fun Int.isTheAnswer(): Boolean = this == 42
                fun foo(input: Int) {
                    input.isTheAnswer()
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when the return value is assigned to a pre-existing variable`() {
            val code = """
                package test
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                @Deprecated("Yes")
                fun listA() = listOf("hello")
                
                fun foo() {
                    var x: List<String>
                    x = listA()
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which doesn't return a value is called`() {
            val code = """
                fun noReturnValue() {}

                fun foo() {
                    noReturnValue()
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used in a test statement`() {
            val code = """
                fun returnsBoolean() = true
                
                fun f() {
                    if (returnsBoolean()) {}
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used in a comparison`() {
            val code = """
                fun returnsInt() = 42
                
                fun f() {
                    if (42 == returnsInt()) {}
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used as parameter for another call`() {
            val code = """
                fun returnsInt() = 42
                
                fun f() {
                    println(returnsInt())
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used with named parameters`() {
            val code = """
                fun returnsInt() = 42
                
                fun f() {
                    println(message = returnsInt())
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }
    }

    @Nested
    inner class `default config with annotated return values` {
        private val subject = IgnoredReturnValue()

        @Test
        fun `reports when a function which returns a value is called and the return is ignored`() {
            val code = """
                package annotation
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() {
                    listOfChecked("hello")
                    println("foo")
                }
            """
            val annotationClass = """
                package annotation

                annotation class CheckReturnValue
            """

            val findings = subject.lintWithContext(env, code, annotationClass)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(7, 5)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `reports when a function which returns a value is called before a valid return`() {
            val code = """
                package noreturn
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(9, 5)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `reports when a function which returns a value is called in chain as first statement and the return is ignored`() {
            val code = """
                package noreturn
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello").isEmpty().not()
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which returns a value is called in the middle of a chain and the return is ignored`() {
            val code = """
                package noreturn
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun String.listOfChecked() = listOf(this)
                
                fun foo() : Int {
                    val hello = "world "
                    hello.toUpperCase()
                        .trim()
                        .listOfChecked()
                        .isEmpty()
                        .not()
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `reports when a function which returns a value is called in the end of a chain and the return is ignored`() {
            val code = """
                package noreturn
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun String.listOfChecked() = listOf(this)

                fun foo() : Int {
                    val hello = "world "
                    hello.toUpperCase()
                        .trim()
                        .listOfChecked()
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(12, 10)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `reports when a function which returns a value is called before a semicolon`() {
            val code = """
                package special
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() {
                    listOfChecked("hello");println("foo")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(9, 5)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `reports when a function which returns a value is called after a semicolon`() {
            val code = """
                package special
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    println("foo");listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(9, 20)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `reports when a function which returns a value is called between comments`() {
            val code = """
                package special
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    /* foo */listOfChecked("hello")//foo
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(9, 14)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `reports when an extension function which returns a value is called and the return is ignored`() {
            val code = """
                package specialize
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun Int.isTheAnswer(): Boolean = this == 42
                fun foo(input: Int) : Int {
                    input.isTheAnswer()
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(8, 11)
            assertThat(findings[0]).hasMessage("The call isTheAnswer is returning a value that is ignored.")
        }

        @Test
        fun `does not report when the return value is assigned to a pre-existing variable`() {
            val code = """
                package specialize
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    var x: List<String>
                    x = listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function which doesn't return a value is called`() {
            val code = """
                package specialize
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun noReturnValue() {}

                fun foo() : Int {
                    noReturnValue()
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used in a test statement`() {
            val code = """
                package comparison
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsBoolean() = true
                
                fun f() {
                    if (returnsBoolean()) {}
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used in a comparison`() {
            val code = """
                package comparison
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42
                
                fun f() {
                    if (42 == returnsInt()) {}
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used as parameter for another call`() {
            val code = """
                package parameter
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42
                
                fun f() {
                    println(returnsInt())
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function's return value is used with named parameters`() {
            val code = """
                package parameter
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42
                
                fun f() {
                    println(message = returnsInt())
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function is the last statement in a block and it's used`() {
            val code = """
                package block
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42

                val result = if (true) {
                    1
                } else {
                    returnsInt()
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `report when a function is not the last statement in a 'if' block and 'if' block is used`() {
            val code = """
                package block
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42

                val result = if (true) {
                    1
                } else {
                    returnsInt()
                    2
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not report when a function is the last statement in a block and it's in a chain`() {
            val code = """
                package block

                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42

                fun test() {
                    if (true) {
                        1
                    } else {
                        returnsInt()
                    }.plus(1)
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `report when a function is not the last statement in a block and it's in a chain`() {
            val code = """
                package block

                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42

                fun test() {
                    if (true) {
                        1
                    } else {
                        returnsInt()
                        2
                    }.plus(1)
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
        }

        @Test
        fun `report when a function is the last statement in a block`() {
            val code = """
                package block

                annotation class CheckReturnValue

                @CheckReturnValue
                fun returnsInt() = 42

                fun test() {
                    if (true) {
                        println("hello")
                    } else {
                        returnsInt()
                    }
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not report when a function return value is consumed in a chain that returns a Unit`() {
            val code = """
                package callchain
                
                annotation class CheckReturnValue

                @CheckReturnValue
                fun String.listOfChecked() = listOf(this)
                fun List<String>.print() { println(this) }
                
                fun foo() : Int {
                    val hello = "world "
                    hello.toUpperCase()
                        .trim()
                        .listOfChecked()
                        .print()
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `reports when the containing class of a function has _@CheckReturnValue_`() {
            val code = """
                package foo

                annotation class CheckReturnValue
                
                @CheckReturnValue
                class Assertions {
                    fun listOfChecked(value: String) = listOf(value)
                }
                
                fun main() {
                    Assertions().listOfChecked("hello")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
        }

        @Test
        fun `reports when the containing object of a function has _@CheckReturnValue_`() {
            val code = """
                package foo

                annotation class CheckReturnValue
                
                @CheckReturnValue
                object Assertions {
                    fun listOfChecked(value: String) = listOf(value)
                }

                fun main() {
                    Assertions.listOfChecked("hello")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not report when the containing class of a function has _@CheckReturnValue_ but the function has _@CanIgnoreReturnValue_`() {
            val code = """
                package foo

                annotation class CheckReturnValue
                annotation class CanIgnoreReturnValue
                
                @CheckReturnValue
                class Assertions {
                    @CanIgnoreReturnValue
                    fun listOfChecked(value: String) = listOf(value)
                }
                
                fun main() {
                    Assertions().listOfChecked("hello")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when the containing class of a function has no _@CheckReturnValue_ but the parent class has _@CheckReturnValue_`() {
            val code = """
                package foo
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                class Parent {
                    class Child {
                        fun listOfChecked(value: String) = listOf(value)
                    }
                }
                
                fun main() {
                    Parent.Child().listOfChecked("hello")
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }
    }

    @Nested
    inner class `custom annotation config` {
        val subject = IgnoredReturnValue(
            TestConfig(mapOf("returnValueAnnotations" to listOf("*.CustomReturn")))
        )

        @Test
        fun `reports when a function is annotated with the custom annotation`() {
            val code = """
                package config
                annotation class CustomReturn
                
                @CustomReturn
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(8, 5)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `does not report when a function is annotated with the not included annotation`() {
            val code = """
                package config
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function is not annotated`() {
            val code = """
                package config

                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }
    }

    @Nested
    inner class `restrict to annotated methods config` {
        val subject = IgnoredReturnValue(TestConfig(mapOf("restrictToAnnotatedMethods" to false)))

        @Test
        fun `reports when a function is annotated with a custom annotation`() {
            val code = """
                package config
                
                annotation class CheckReturnValue
                
                @CheckReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(9, 5)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `reports when a function is not annotated`() {
            val code = """
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).hasSize(1)
            assertThat(findings).hasStartSourceLocation(4, 5)
            assertThat(findings[0]).hasMessage("The call listOfChecked is returning a value that is ignored.")
        }

        @Test
        fun `does not report when a function has _@CanIgnoreReturnValue_`() {
            val code = """
                package foo

                annotation class CanIgnoreReturnValue

                @CanIgnoreReturnValue
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val findings = subject.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function has a custom annotation`() {
            val code = """
                package foo

                annotation class CustomIgnoreReturn

                @CustomIgnoreReturn
                fun listOfChecked(value: String) = listOf(value)
                
                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val rule = IgnoredReturnValue(
                TestConfig(
                    mapOf(
                        "ignoreReturnValueAnnotations" to listOf("*.CustomIgnoreReturn"),
                        "restrictToAnnotatedMethods" to false
                    )
                )
            )
            val findings = rule.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not report when a function is in ignoreFunctionCall`() {
            val code = """
                package foo

                fun listOfChecked(value: String) = listOf(value)

                fun foo() : Int {
                    listOfChecked("hello")
                    return 42
                }
            """
            val rule = IgnoredReturnValue(
                TestConfig(
                    mapOf(
                        "ignoreFunctionCall" to listOf("foo.listOfChecked"),
                        "restrictToAnnotatedMethods" to false
                    )
                )
            )
            val findings = rule.compileAndLintWithContext(env, code)
            assertThat(findings).isEmpty()
        }
    }
}
