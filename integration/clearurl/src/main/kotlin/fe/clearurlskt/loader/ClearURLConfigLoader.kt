package fe.clearurlskt.loader

import fe.clearurlskt.Resource
import fe.clearurlskt.provider.Provider
import fe.clearurlskt.provider.ProviderSerializer
import java.io.InputStream
import java.net.URL

public interface ClearURLConfigLoader {
    public fun load(): Result<List<Provider>?>
}

public object BundledClearURLConfigLoader : ClearURLConfigLoader {
    private const val FILE = "clearurls.json"
    private val bundledClass = Resource::class.java
    private val bundledClassPackage by lazy {
        bundledClass.`package`.name.replace(".", "/")
    }

    private val url by lazy {
        bundledClass.getResource(FILE)
            ?: getSystemResource(bundledClassPackage)
            ?: getSystemResource("fe/clearurlskt")
            ?: getSystemResource()
    }

    public fun getSystemResource(path: String? = null): URL? {
        val filePath = path?.let { "$it/$FILE" } ?: FILE
        return ClassLoader.getSystemResource(filePath)
    }

    override fun load(): Result<List<Provider>?> {
        return runCatching {
            url?.openStream()?.let { ProviderSerializer.handle(it) }
        }
    }
}

public class StreamClearURLConfigLoader(private val stream: InputStream) : ClearURLConfigLoader {
    override fun load(): Result<List<Provider>?> {
        return runCatching {
            ProviderSerializer.handle(stream)
        }
    }
}
