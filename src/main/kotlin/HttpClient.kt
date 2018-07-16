import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.InputStream

class HttpClient {

    fun get(url: String, cookies: MutableMap<String, String>, vararg data: String) =
            execute(Connection.Method.GET, url, cookies, false, null,
                    null, null, *data)

    fun post(url: String, cookies: MutableMap<String, String>, vararg data: String) =
            execute(Connection.Method.POST, url, cookies, false, null,
                    null, null, *data)


    fun postWithFile(url: String, cookies: MutableMap<String, String>, fileKey: String?,
                     fileName: String?, inputStream: InputStream?, vararg data: String) =
            execute(Connection.Method.POST, url, cookies, false, fileKey, fileName,
                    inputStream, *data)

    fun postIgnoringErrors(url: String, cookies: MutableMap<String, String>,
                           vararg data: String) =
            execute(Connection.Method.POST, url, cookies, true,
                    null, null, null, *data)

    private fun execute(method: Connection.Method, url: String,
                        cookies: MutableMap<String, String>, ignoreErrors: Boolean,
                        fileKey: String?, fileName: String?, inputStream: InputStream?,
                        vararg data: String): Connection.Response {
        var connection = Jsoup.connect(url)
                .timeout(10 * 1000)
                .ignoreContentType(true)
                .ignoreHttpErrors(ignoreErrors)
                .method(method)
                .cookies(cookies)
                .userAgent("Mozilla")
                .data(*data)

        if (fileKey != null) {
            connection = connection.data(fileKey, fileName, inputStream)
        }

        val response = connection.execute()
        cookies.putAll(response.cookies())
        return response
    }
}
