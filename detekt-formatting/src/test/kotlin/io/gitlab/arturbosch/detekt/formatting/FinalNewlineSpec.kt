package io.gitlab.arturbosch.detekt.formatting

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.formatting.wrappers.FinalNewline
import io.gitlab.arturbosch.detekt.test.TestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FinalNewlineSpec {

    @Test
    fun `should report missing new line by default`() {
        val findings = FinalNewline(Config.empty)
            .lint("fun main() = Unit")

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `should not report as new line is present`() {
        val findings = FinalNewline(Config.empty).lint(
            """
                fun main() = Unit

            """
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `should report new line when configured`() {
        val findings = FinalNewline(TestConfig(INSERT_FINAL_NEWLINE_KEY to "false"))
            .lint(
                """
            fun main() = Unit

                """
            )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `should not report when no new line is configured and not present`() {
        val findings = FinalNewline(TestConfig(INSERT_FINAL_NEWLINE_KEY to "false"))
            .lint("fun main() = Unit")

        assertThat(findings).isEmpty()
    }
}

private const val INSERT_FINAL_NEWLINE_KEY = "insertFinalNewLine"
