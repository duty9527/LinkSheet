package fe.embed.resolve.loader

import fe.embed.resolve.Resource
import fe.embed.resolve.config.ConfigSerializer
import fe.embed.resolve.config.ConfigV1
import java.io.InputStream
import java.net.URL

public interface EmbedResolveConfigLoader {
    public fun load(): Result<ConfigV1?>
}

public object BundledEmbedResolveConfigLoader : EmbedResolveConfigLoader {
    private const val file = "embed_resolve.json"
    private val bundledClass = Resource::class.java
    private val bundledClassPackage by lazy {
        bundledClass.`package`.name.replace(".", "/")
    }

    private val url by lazy {
        bundledClass.getResource(file)
            ?: getSystemResource(bundledClassPackage)
            ?: getSystemResource("fe/embed/resolve")
            ?: getSystemResource()
    }

    public fun getSystemResource(path: String? = null): URL? {
        val filePath = path?.let { "$it/$file" } ?: file
        return ClassLoader.getSystemResource(filePath)
    }

    override fun load(): Result<ConfigV1?> {
        return runCatching {
            println(url)
            url?.openStream()?.let { ConfigSerializer.parseConfig<ConfigV1>(it) }
        }
    }
}

public class StreamEmbedResolveConfigLoader(private val stream: InputStream) : EmbedResolveConfigLoader {
    override fun load(): Result<ConfigV1?> {
        return runCatching {
            ConfigSerializer.parseConfig<ConfigV1>(stream)
        }
    }
}
