package fe.linksheet.module.resolver.personal

import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import fe.linksheet.testlib.core.BaseUnitTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
internal class PersonalLinkRuleEngineTest : BaseUnitTest {
    private val engine = PersonalLinkRuleEngine()

    @Test
    fun `github links prefer github app and remove tracking parameters`() {
        val result = engine.apply(Uri.parse("https://github.com/openai/codex?utm_source=test&tab=readme"))

        assertThat(result.uri.toString()).isEqualTo("https://github.com/openai/codex?tab=readme")
        assertThat(result.preferredPackageName).isEqualTo(PersonalLinkRuleEngine.GithubPackageName)
        assertThat(result.autoLaunch).isTrue()
    }

    @Test
    fun `youtube links prefer youtube app and remove tracking parameters`() {
        val result = engine.apply(Uri.parse("https://youtu.be/abc123?fbclid=tracking&feature=share"))

        assertThat(result.uri.toString()).isEqualTo("https://youtu.be/abc123?feature=share")
        assertThat(result.preferredPackageName).isEqualTo(PersonalLinkRuleEngine.YoutubePackageName)
        assertThat(result.autoLaunch).isTrue()
    }

    @Test
    fun `non http links are unchanged`() {
        val uri = Uri.parse("mailto:test@example.com?utm_source=test")
        val result = engine.apply(uri)

        assertThat(result.uri).isEqualTo(uri)
        assertThat(result.preferredPackageName).isNull()
        assertThat(result.autoLaunch).isFalse()
    }
}
