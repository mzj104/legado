package io.legado.app.help
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object QdCat
{
    private var qdcatalog = JSONArray()
    var nowqdbookid = ""
    private var ready = false
    fun isReady() = ready
    private var qdidmap = ConcurrentHashMap<String, String>()
    private val listeners = mutableListOf<() -> Unit>()
    fun addOnReadyListener(listener: () -> Unit) {
        listeners.add(listener)
        if (ready) {
            listener()
        }
    }
    private fun notifyReady() {
        ready = true
        listeners.forEach { it() }
        listeners.clear()
    }
    fun getQdcatalog(): JSONArray
    {
        return qdcatalog
    }
    fun get_mulu(bookId: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("BESTMATCH", "目录获取开始")
            try {
                val token = "wMKJcrrSW7SvDuZv7w7OaKjf0XtM1C092s9vSuRu"
                val tsfp =
                    "ltvuV0MF2utBvS0Q76jgkUytFDAufT84h0wpEaR0f5thQLErU5mD049/ucj/OHza4cxnvd7DsZoyJTLYCJI3dwNCRpmUcIwV2gXFmoMlj4xFV0FiQMjeUANNdrghvWYUKHhCNxS00jA8eIUd379yilkMsyN1zap3TO14fstJ019E6KDQmI5uDW3HlFWQRzaLbjcMcuqPr6g18L5a5TuP7Q/+L1MiBL9B1EPG1XodWy53tETvfbpdPRmud5j5SqA="

                val url = "https://www.qidian.com/book/$bookId/"
                Log.d("BESTMATCH", "请求 URL: $url")
                // 设置请求头
                val headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    "Accept-Encoding" to "gzip, deflate, br, zstd",
                    "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
                    "Cache-Control" to "max-age=0",
                    "Connection" to "keep-alive",
                    "Cookie" to "_csrfToken=$token; w_tsfp=$tsfp",
                    "Host" to "www.qidian.com",
                    "Sec-Fetch-Dest" to "document",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "none",
                    "Sec-Fetch-User" to "?1",
                    "Upgrade-Insecure-Requests" to "1",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0",
                    "sec-ch-ua" to "\"Chromium\";v=\"142\", \"Microsoft Edge\";v=\"142\", \"Not_A Brand\";v=\"99\"",
                    "sec-ch-ua-mobile" to "?0",
                    "sec-ch-ua-platform" to "\"Windows\""
                )

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                // 设置请求头
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Failed to fetch data from server. Response Code: $responseCode")
                }

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                }

                val htmlContent = response.toString()

                // 使用 Jsoup 解析 HTML
                val doc: Document = Jsoup.parse(htmlContent)
                val chapters = mutableListOf<JSONObject>()

                // 解析章节列表
                val chapterItems = doc.select("li.chapter-item")
                var idx = 0
                for (li in chapterItems) {
                    val rid = li.attr("data-rid")

                    val a = li.selectFirst("a.chapter-name")
                    if (a != null) {
                        val text = a.text().trim() // 章节显示文本
                        var href = a.attr("href")
                        var hrefd = href.split("/")
                        var cid = hrefd[hrefd.size - 2]
                        val titleAttr = a.attr("title")
                        // 将章节信息添加到列表
                        val chapterData = JSONObject()
                        chapterData.put("idx", idx)
                        chapterData.put("data_rid", rid)
                        chapterData.put("text", text)
                        chapterData.put("href", cid)
                        chapterData.put("title_attr", titleAttr)
                        chapters.add(chapterData)
                        idx++
                    }
                }
                // 转换为 JSON 格式并返回
                val resultJson = JSONArray(chapters)
                withContext(Dispatchers.Main) {
                    qdcatalog = resultJson
                    Log.d("BESTMATCH", "目录获取完成，共 ${qdcatalog.length()} 条")
                    notifyReady()
                }
            }catch (e: Exception) {
                Log.e("BESTMATCH", "获取目录失败: ${e.stackTraceToString()}")
            }
        }
    }

    suspend fun get_review(bookId: String, chapterId: String?, sid: Int): JSONArray {
        var ssid = 0
        if (sid == 0)
            ssid = -1
        else
            ssid = sid
        val token = "wMKJcrrSW7SvDuZv7w7OaKjf0XtM1C092s9vSuRu"
        val tsfp =
            "ltvuV0MF2utBvS0Q76jgkUytFDAufT84h0wpEaR0f5thQLErU5mD049/ucj/OHza4cxnvd7DsZoyJTLYCJI3dwNCRpmUcIwV2gXFmoMlj4xFV0FiQMjeUANNdrghvWYUKHhCNxS00jA8eIUd379yilkMsyN1zap3TO14fstJ019E6KDQmI5uDW3HlFWQRzaLbjcMcuqPr6g18L5a5TuP7Q/+L1MiBL9B1EPG1XodWy53tETvfbpdPRmud5j5SqA="
        val cid = chapterId
        Log.d("COMMENT", "chapterId: $chapterId, cid: $cid")

        val urlString =
            "https://www.qidian.com/webcommon/chapterreview/reviewlist?bookId=$bookId&chapterId=$cid&page=1&pageSize=100&segmentId=$ssid&type=2&_csrfToken=$token"
        Log.d("COMMENT", "urlString: $urlString")
        val url = URL(urlString)

        try {
            // 创建连接
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // 设置请求头
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd")
            connection.setRequestProperty(
                "Accept-Language",
                "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6"
            )
            connection.setRequestProperty("Connection", "keep-alive")
            connection.setRequestProperty("Cookie", "_csrfToken=$token; w_tsfp=$tsfp")
            connection.setRequestProperty("Host", "www.qidian.com")
            connection.setRequestProperty("Sec-Fetch-Dest", "empty")
            connection.setRequestProperty("Sec-Fetch-Mode", "cors")
            connection.setRequestProperty("Sec-Fetch-Site", "same-origin")
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0"
            )
            connection.setRequestProperty("X-D", "0")
            connection.setRequestProperty(
                "sec-ch-ua",
                "\"Chromium\";v=\"142\", \"Microsoft Edge\";v=\"142\", \"Not_A Brand\";v=\"99\""
            )
            connection.setRequestProperty("sec-ch-ua-mobile", "?0")
            connection.setRequestProperty("sec-ch-ua-platform", "\"Windows\"")

            // 获取响应
            val responseCode = connection.responseCode
            Log.e("COMMENT", "HTTP 状态码: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应内容
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val body = response.toString()

                Log.e("COMMENT", "服务器返回原始内容 >>>")
                Log.e("COMMENT", body)
                Log.e("COMMENT", "<<< 原始内容结束")

                // 解析 JSON 响应
                val json = JSONObject(body).getJSONObject("data").getJSONArray("list")

                Log.e("COMMENT1", "=== 单个评论完成 ===")
                Log.e("COMMENT1", json.toString())

                return json
            } else {
                Log.e("COMMENT", "HTTP 请求失败，状态码: $responseCode")
                return JSONArray()
            }
        } catch (e: Exception) {
            Log.e("COMMENT", "评论获取失败: ${e.message}")
            e.printStackTrace()
            return JSONArray()
        }
    }

    suspend fun get_sub_review(rootId: String): JSONArray {
        val token = "wMKJcrrSW7SvDuZv7w7OaKjf0XtM1C092s9vSuRu"
        val tsfp = "ltvuV0MF2utBvS0Q76jgkUytFDAufT84h0wpEaR0f5thQLErU5mD049/ucj/OHza4cxnvd7DsZoyJTLYCJI3dwNCRpmUcIwV2gXFmoMlj4xFV0FiQMjeUANNdrghvWYUKHhCNxS00jA8eIUd379yilkMsyN1zap3TO14fstJ019E6KDQmI5uDW3HlFWQRzaLbjcMcuqPr6g18L5a5TuP7Q/+L1MiBL9B1EPG1XodWy53tETvfbpdPRmud5j5SqA="
        val urlString = "https://www.qidian.com/webcommon/chapterreview/quotereviewlist?reviewId=$rootId&page=1&pageSize=100&_csrfToken=$token"
        Log.d("COMMENT", "urlString: $urlString")
        val url = URL(urlString)
        try {
            // 创建连接
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // 设置请求头
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd")
            connection.setRequestProperty(
                "Accept-Language",
                "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6"
            )
            connection.setRequestProperty("Connection", "keep-alive")
            connection.setRequestProperty("Cookie", "_csrfToken=$token; w_tsfp=$tsfp")
            connection.setRequestProperty("Host", "www.qidian.com")
            connection.setRequestProperty("Sec-Fetch-Dest", "empty")
            connection.setRequestProperty("Sec-Fetch-Mode", "cors")
            connection.setRequestProperty("Sec-Fetch-Site", "same-origin")
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0"
            )
            connection.setRequestProperty("X-D", "0")
            connection.setRequestProperty(
                "sec-ch-ua",
                "\"Chromium\";v=\"142\", \"Microsoft Edge\";v=\"142\", \"Not_A Brand\";v=\"99\""
            )
            connection.setRequestProperty("sec-ch-ua-mobile", "?0")
            connection.setRequestProperty("sec-ch-ua-platform", "\"Windows\"")

            // 获取响应
            val responseCode = connection.responseCode
            Log.e("COMMENT", "HTTP 状态码: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应内容
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val body = response.toString()

                Log.e("COMMENT", "服务器返回原始内容 >>>")
                Log.e("COMMENT", body)
                Log.e("COMMENT", "<<< 原始内容结束")

                // 解析 JSON 响应
                val json = JSONObject(body).getJSONObject("data").getJSONArray("list")

                Log.e("COMMENT1", "=== 单个评论完成 ===")
                Log.e("COMMENT1", json.toString())

                return json
            } else {
                Log.e("COMMENT", "HTTP 请求失败，状态码: $responseCode")
                return JSONArray()
            }
        } catch (e: Exception) {
            Log.e("COMMENT", "评论获取失败: ${e.message}")
            e.printStackTrace()
            return JSONArray()
        }
    }

    suspend fun parseResultList(name: String): String? {
        if (qdidmap.containsKey(name)){
            Log.d("MYSEARCH", "🎉 存在记录！title=$name, bid=${qdidmap[name]}")
            return qdidmap[name]
        }
        val token = "eV3kXa9RiAUTFzWfCMiDu0nxGTxfTbDbLptN2BqV"
        val tsfp =
            "ltvuV0MF2utBvS0Q76jgkUytFDAufT84h0wpEaR0f5thQLErU5mD049/ucj/OHza4cxnvd7DsZoyJTLYCJI3dwNCRpmUcIwV2gXFmoMlj4xFV0FiQMjeUANNdrghvWYUKHhCNxS00jA8eIUd379yilkMsyN1zap3TO14fstJ019E6KDQmI5uDW3HlFWQRzaLbjcMcuqPr6g18L5a5TuP7Q/+L1MiBL9B1EPG1XodWy53tETvfbpdPRmud5j5SqA="

        return try {

            val encoded = URLEncoder.encode(name, "UTF-8")
            val urlString = "https://www.qidian.com/so/$encoded.html"

            Log.d("SEARCH", "请求 URL: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // ======== 设置 Headers（完全对齐 get_review）=========
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd")
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
            connection.setRequestProperty("Cache-Control", "max-age=0")
            connection.setRequestProperty("Connection", "keep-alive")
            connection.setRequestProperty("Cookie", "_csrfToken=$token; w_tsfp=$tsfp")
            connection.setRequestProperty("Host", "www.qidian.com")
            connection.setRequestProperty("Sec-Fetch-Dest", "document")
            connection.setRequestProperty("Sec-Fetch-Mode", "navigate")
            connection.setRequestProperty("Sec-Fetch-Site", "none")
            connection.setRequestProperty("Sec-Fetch-User", "?1")
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0")
            connection.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"142\", \"Microsoft Edge\";v=\"142\", \"Not_A Brand\";v=\"99\"")
            connection.setRequestProperty("sec-ch-ua-mobile", "?0")
            connection.setRequestProperty("sec-ch-ua-platform", "\"Windows\"")

            // ========== 读取网页内容 ==========
            val responseCode = connection.responseCode
            Log.d("SEARCH", "HTTP 状态码: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("SEARCH", "请求失败")
                return ""
            }

            val html = connection.inputStream.bufferedReader().use { it.readText() }

            Log.d("SEARCH", "HTML 长度: ${html.length}")

            // ======== Jsoup 解析 HTML ========
            val doc = Jsoup.parse(html)

            val items = doc.select("#result-list .book-img-text ul li.res-book-item")
            Log.d("SEARCH", "匹配到 ${items.size} 项")

            for (li in items) {
                val bid = li.attr("data-bid")

                val cite = li.selectFirst("h3.book-info-title cite.red-kw")
                val title = cite?.text()?.trim()
                    ?: li.selectFirst("h3.book-info-title a")?.attr("title")?.trim()
                    ?: ""

                Log.d("SEARCH", "解析到: $title   bid=$bid")

                if (title == name) {
                    Log.d("SEARCH", "🎉 找到了！title=$title, bid=$bid")
                    qdidmap[name] = bid
                    return bid
                }
            }

            ""
        } catch (e: Exception) {
            Log.e("SEARCH", "搜索获取失败: ${e.message}")
            e.printStackTrace()
            ""
        }
    }

}