package fe.linksheet.util

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import fe.linksheet.testlib.core.BaseUnitTest
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class ExportImportUseCaseTest : BaseUnitTest {


//    @org.junit.Test
//    fun test() {
//        val useCase = ExportImportUseCase(repository, Json.Default, Toml.Default)
//        println(useCase.export(false))
//    }
}
