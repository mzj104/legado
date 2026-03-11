package io.legado.app.help

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 段落评论数据类
 */
data class ReviewSegment(
    val isHotSegment: Boolean,      // 是否为热评段落
    val authorReview: JSONObject?,   // 作者评论（可选）
    val containSelf: Boolean,        // 是否包含自己
    val segmentId: Int,              // 段落ID，-1表示整章
    val textCount: Int,              // 文本数量
    val paragraphId: Int,            // 段落序号
    val reviewNum: Int,              // 评论数量
    val isHotComment: Boolean        // 是否为热评
)

/**
 * 章节评论响应数据类
 */
data class ChapterReviewData(
    val enableReview: Int,           // 是否启用评论
    val total: Int,                  // 总评论数
    val list: List<ReviewSegment>    // 段落评论列表
)

object ApiClient {
    /**
     * 获取章节评论数据
     * @param chapterId 章节ID
     * @return ChapterReviewData 包含热评信息的完整评论数据
     */
    suspend fun fetchChapterReviews(chapterId: String): ChapterReviewData? =
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
                    val enableReview = commentsData.optInt("enableReview", 0)
                    val total = commentsData.optInt("total", 0)
                    val commentsList = commentsData.getJSONArray("list")

                    val reviewSegments = mutableListOf<ReviewSegment>()

                    // 遍历评论数据
                    for (i in 0 until commentsList.length()) {
                        val comment = commentsList.getJSONObject(i)
                        val segmentId = comment.optInt("segmentId", 0)

                        val reviewSegment = ReviewSegment(
                            isHotSegment = comment.optBoolean("isHotSegment", false),
                            authorReview = comment.optJSONObject("authorReview"),
                            containSelf = comment.optBoolean("containSelf", false),
                            segmentId = segmentId,
                            textCount = comment.optInt("textCount", 0),
                            paragraphId = comment.optInt("paragraphId", 0),
                            reviewNum = comment.optInt("reviewNum", 0),
                            isHotComment = comment.optBoolean("isHotComment", false)
                        )
                        reviewSegments.add(reviewSegment)

                        Log.e("COMMENT", "解析到评论：segmentId=$segmentId, reviewNum=${reviewSegment.reviewNum}, isHotSegment=${reviewSegment.isHotSegment}")
                    }

                    val result = ChapterReviewData(
                        enableReview = enableReview,
                        total = total,
                        list = reviewSegments
                    )

                    Log.e("COMMENT", "=== 评论解析完成 ===")
                    Log.e("COMMENT", "总评论数: $total, 启用评论: $enableReview, 段落数: ${reviewSegments.size}")

                    return@withContext result
                } else {
                    Log.e("COMMENT", "HTTP 请求失败，状态码: $responseCode")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("COMMENT", "评论获取失败: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }

    /**
     * 获取章节评论数量（旧接口，保持向后兼容）
     * @param chapterId 章节ID
     * @return Map<String, Int> 段落ID到评论数的映射
     */
    suspend fun fetchCommentCounts(chapterId: String): Map<String, Int> {
        val reviewData = fetchChapterReviews(chapterId) ?: return emptyMap()

        val result = mutableMapOf<String, Int>()
        reviewData.list.forEach { segment ->
            val segmentId = if (segment.segmentId == -1) 0 else segment.segmentId
            result["chapter_${chapterId}_para_$segmentId"] = segment.reviewNum
        }

        return result
    }

    // 获取 CSRF Token（你可以根据实际需求替换这个方法）
    private fun getCsrfToken(): String {
        // 这里假设获取CSRF Token的方法，你需要根据实际需求获取token
        return "wMKJcrrSW7SvDuZv7w7OaKjf0XtM1C092s9vSuRu"
    }
}