package io.legado.app.help

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    suspend fun fetchCommentCounts(chapterId: String): Map<String, Int> =
        withContext(Dispatchers.IO) {
            // 替换为实际的 bookId 和 chapterId
            val bookId = QdCat.nowqdbookid  // 假设 bookId 固定，你可以根据需要修改
            val token = getCsrfToken()  // 获取 CSRF Token
            val cid = chapterId
            Log.d("COMMENT", "chapterId: $chapterId, cid: $cid")

            // 构建 URL，包含 bookId、chapterId 和 _csrfToken
            val urlString = "https://www.qidian.com/webcommon/chapterreview/reviewsummary?bookId=$bookId&chapterId=$cid&_csrfToken=$token"
            val url = URL(urlString)

            try {
                // 创建连接
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                // 设置请求头
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd")
                connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                connection.setRequestProperty("Connection", "keep-alive")
                connection.setRequestProperty("Cookie", "_csrfToken=$token")
                connection.setRequestProperty("Host", "www.qidian.com")
                connection.setRequestProperty("Sec-Fetch-Dest", "empty")
                connection.setRequestProperty("Sec-Fetch-Mode", "cors")
                connection.setRequestProperty("Sec-Fetch-Site", "same-origin")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0")
                connection.setRequestProperty("X-D", "0")
                connection.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"142\", \"Microsoft Edge\";v=\"142\", \"Not_A Brand\";v=\"99\"")
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
                    val json = JSONObject(body)
                    val commentsData = json.getJSONObject("data")
                    val commentsList = commentsData.getJSONArray("list")

                    val result = mutableMapOf<String, Int>()

                    // 遍历评论数据
                    for (i in 0 until commentsList.length()) {
                        val comment = commentsList.getJSONObject(i)
                        var segmentId = comment.optInt("segmentId", 0)  // 默认值为0
                        if (segmentId == -1)
                            segmentId = 0
                        val reviewNum = comment.getInt("reviewNum")
                        result["chapter_${chapterId}_para_$segmentId"] = reviewNum
                        Log.e("COMMENT", "解析到评论：chapter_${chapterId}_para_$segmentId => $reviewNum")
                    }

                    Log.e("COMMENT", "=== 评论解析完成 ===")
                    Log.e("COMMENT", result.toString())

                    return@withContext result
                } else {
                    Log.e("COMMENT", "HTTP 请求失败，状态码: $responseCode")
                    return@withContext emptyMap()
                }
            } catch (e: Exception) {
                Log.e("COMMENT", "评论获取失败: ${e.message}")
                e.printStackTrace()
                return@withContext emptyMap()
            }
        }

    // 获取 CSRF Token（你可以根据实际需求替换这个方法）
    private fun getCsrfToken(): String {
        // 这里假设获取CSRF Token的方法，你需要根据实际需求获取token
        return "wMKJcrrSW7SvDuZv7w7OaKjf0XtM1C092s9vSuRu"
    }
}