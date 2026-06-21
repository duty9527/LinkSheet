package fe.embed.resolve.config

import com.google.gson.GsonBuilder
import com.google.gson.Gson
import fe.gson.typeadapter.RegexTypeAdapter
import java.io.InputStream

public interface Config

public object ConfigSerializer {
    @PublishedApi
    internal val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Regex::class.java, RegexTypeAdapter)
        .create()

    public inline fun <reified T : Config> parseConfig(inputStream: InputStream): T {
        return inputStream.bufferedReader().use { gson.fromJson(it, T::class.java) }
    }
}
