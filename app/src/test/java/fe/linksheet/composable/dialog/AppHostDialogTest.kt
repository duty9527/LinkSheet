package fe.linksheet.composable.dialog

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import fe.linksheet.testlib.core.BaseUnitTest
import org.junit.Test

internal class AppHostDialogTest : BaseUnitTest {
    @Test
    fun `host input accepts plain hosts`() {
        assertThat(normalizeCustomHost("GitHub.com")).isEqualTo("github.com")
    }

    @Test
    fun `host input accepts urls`() {
        assertThat(normalizeCustomHost("https://github.com/openai/codex?tab=readme")).isEqualTo("github.com")
    }

    @Test
    fun `host input rejects invalid hosts`() {
        assertThat(normalizeCustomHost("not a host")).isNull()
        assertThat(normalizeCustomHost("localhost")).isNull()
    }
}
