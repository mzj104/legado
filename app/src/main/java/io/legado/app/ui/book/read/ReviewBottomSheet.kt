package io.legado.app.ui.book.read

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.legado.app.help.QdCat
import io.legado.app.help.QdCat.get_review
import io.legado.app.help.ReviewThread
import io.legado.app.help.Simicatalog
import io.legado.app.help.book.BookContent
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.config.AppConfig.isNightTheme
import io.legado.app.model.ReadBook
import io.legado.app.data.appDb
import io.legado.app.ui.book.read.page.ContentTextView
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.ReaderBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


object CommentOffsetManager {
    var offset = 0

    fun reset() {
        offset = 0
    }

    fun forward() {
        offset++
    }

    fun backward() {
        offset--
    }
}


class ReviewBottomSheet(
    private val chapterIndex: Int,
    private val paragraphIndex: Int
) : BottomSheetDialogFragment() {

    private var recyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    var onOffsetChange: ((Int) -> Unit)? = null

    private fun dp(v: Int): Int =
        (v * requireContext().resources.displayMetrics.density).toInt()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val root = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (requireContext().resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        root.layoutParams = params


// ============= 1. 创建顶部固定标题栏 =============
        val headerHeight = dp(55)

        val header = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                headerHeight
            )
            setBackgroundColor(
                if (isNightTheme) 0xFF1C1C1C.toInt()
                else 0xFFF5F5F5.toInt()
            )
        }

// ------ 左按钮：向前一段 ------
        val btnPrev = AppCompatButton(requireContext()).apply {
            text = "← 向前"
            textSize = 14f
            setPadding(20, 10, 20, 10)
            setBackgroundColor(0x00000000)
            if (isNightTheme) setTextColor(0xFFCCCCCC.toInt())
            else setTextColor(0xFF444444.toInt())

            setOnClickListener {
                CommentOffsetManager.backward()
                ReaderBridge.readView?.upContent(0, true)
            }
        }

// ------ 中间区域（标题 + 相似度按钮）------
        val middleContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = AppCompatTextView(requireContext()).apply {
            text = ""
            textSize = 0f
        }

        val btnSimilarity = AppCompatButton(requireContext()).apply {
            text = "自动匹配"
            textSize = 14f
            setPadding(20, 10, 20, 10)
            setBackgroundColor(0x00000000)
            if (isNightTheme) setTextColor(0xFFCCCCCC.toInt())
            else setTextColor(0xFF000000.toInt())

            setOnClickListener {
                calculateSimilarity()
            }
        }

        middleContainer.addView(titleView)
        middleContainer.addView(btnSimilarity, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ))

// ------ 右按钮：向后一段 ------
        val btnNext = AppCompatButton(requireContext()).apply {
            text = "向后 →"
            textSize = 14f
            setPadding(20, 10, 20, 10)
            setBackgroundColor(0x00000000)
            if (isNightTheme) setTextColor(0xFFCCCCCC.toInt())
            else setTextColor(0xFF444444.toInt())

            setOnClickListener {
                CommentOffsetManager.forward()
                ReaderBridge.readView?.upContent(0, true)
            }
        }

// ====== 将控件加入 header ======
        header.addView(btnPrev, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.START or Gravity.CENTER_VERTICAL
        ))

        header.addView(middleContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        header.addView(btnNext, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.END or Gravity.CENTER_VERTICAL
        ))

// ====== 加到 root ======
        root.addView(header)


// ============= 3. RecyclerView（放在 Header 下方） =============
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            visibility = View.GONE
        }

        val rvParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

// ⭐ 关键：与 header 高度保持一致
        rvParams.topMargin = headerHeight

        recyclerView!!.layoutParams = rvParams
        root.addView(recyclerView)


// 加载评论（传入 titleView）
        loadReviews(titleView)


        return root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = super.onCreateDialog(savedInstanceState).apply {

        setOnShowListener {
            val sheet = (this as com.google.android.material.bottomsheet.BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            sheet?.setBackgroundColor(
                if (isNightTheme) 0xFF1C1C1C.toInt() else 0xFFF5F5F5.toInt()
            )
        }
    }

    private fun loadReviews(titleView: TextView) {

        CoroutineScope(Dispatchers.IO).launch {
            val qdId = Simicatalog.get_qdchapter_id("", chapterIndex)
            val rawArr = get_review(QdCat.nowqdbookid, qdId, paragraphIndex - 1 + CommentOffsetManager.offset)

            val threads = MutableList(rawArr.length()) { i ->
                ReviewThread(rawArr.getJSONObject(i))
            }.sortedByDescending { it.root.optInt("likeCount") }
                .toMutableList()

            withContext(Dispatchers.Main) {
                // ⭐ 更新评论数量
                titleView.text = ""

                progressBar?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                recyclerView?.adapter = ReviewAdapter(threads)
            }
        }
    }

    /**
     * 发送数据到API进行匹配分析
     */
    private fun calculateSimilarity() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取适配器中的评论数据
                val adapter = recyclerView?.adapter as? ReviewAdapter
                if (adapter == null) {
                    Log.e("MATCH_API", "无法获取评论适配器")
                    return@launch
                }

                // 使用反射获取评论列表（因为 threads 是私有的）
                val threadsField = adapter.javaClass.getDeclaredField("threads")
                threadsField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val threads = threadsField.get(adapter) as? List<ReviewThread>
                if (threads == null || threads.isEmpty()) {
                    Log.e("MATCH_API", "评论列表为空")
                    return@launch
                }

                // 收集所有评论内容
                val allReviewContent = StringBuilder()
                for (thread in threads) {
                    val content = thread.root.optString("content", "")
                    if (content.isNotEmpty()) {
                        allReviewContent.append(content).append("\n")
                    }
                    // 也包含子评论
                    for (reply in thread.replies) {
                        val replyContent = reply.optString("content", "")
                        if (replyContent.isNotEmpty()) {
                            allReviewContent.append(replyContent).append("\n")
                        }
                    }
                }

                // 截断评论内容，最多前1000字
                val reviewSummary = allReviewContent.toString().trim()
                val truncatedReview = if (reviewSummary.length > 1000) {
                    reviewSummary.take(1000) + "...(已截断)"
                } else {
                    reviewSummary
                }

                // 获取当前书籍和章节
                val book = ReadBook.book
                if (book == null) {
                    Log.e("MATCH_API", "无法获取当前书籍")
                    return@launch
                }

                val curChapterIndex = ReadBook.durChapterIndex
                val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, curChapterIndex)
                if (chapter == null) {
                    Log.e("MATCH_API", "无法获取当前章节")
                    return@launch
                }

                // 获取原始章节内容
                val rawContent = BookHelp.getContent(book, chapter)
                if (rawContent == null) {
                    Log.e("MATCH_API", "无法获取章节原始内容")
                    return@launch
                }

                // 使用 ContentProcessor 处理内容，获取原始自然段
                val contentProcessor = ContentProcessor.get(book.name, book.origin)
                val bookContent: BookContent = contentProcessor.getContent(
                    book = book,
                    chapter = chapter,
                    content = rawContent,
                    includeTitle = false,
                    useReplace = false,
                    chineseConvert = false,
                    reSegment = false
                )

                val allParagraphs = bookContent.textList
                if (allParagraphs.isEmpty()) {
                    Log.e("MATCH_API", "章节处理后段落数量为空")
                    return@launch
                }

                // 获取附近5页的段落（基于 paragraphIndex）
                val currentParaIndex = maxOf(0, paragraphIndex - 1)
                val rangeStart = maxOf(0, currentParaIndex - 6)
                val rangeEnd = minOf(allParagraphs.size, currentParaIndex + 7)

                if (rangeEnd <= rangeStart) {
                    Log.e("MATCH_API", "段落范围无效: start=$rangeStart, end=$rangeEnd")
                    return@launch
                }

                val nearbyParagraphs = allParagraphs.subList(rangeStart, rangeEnd)

                // 构建请求数据
                val paragraphsText = StringBuilder()
                paragraphsText.append("评论：\n")
                paragraphsText.append(truncatedReview).append("\n\n")
                paragraphsText.append("候选段落：\n")
                for ((index, paragraphText) in nearbyParagraphs.withIndex()) {
                    val paraNum = rangeStart + index + 1
                    paragraphsText.append("段落 #$paraNum: ").append(paragraphText).append("\n")
                }

                Log.d("MATCH_API", "==================== 发送API请求 ====================")
                Log.d("MATCH_API", "评论按钮对应段落序号: $paragraphIndex")
                Log.d("MATCH_API", "=====================================================")

                // 发送API请求
                val result = sendMatchRequest(paragraphsText.toString())
                Log.d("MATCH_API", "==================== API响应 ====================")
                Log.d("MATCH_API", result)

                // 如果是错误消息，不继续处理
                if (result.startsWith("请求失败") || result.startsWith("网络请求异常") || result.startsWith("API密钥未配置")) {
                    Log.d("MATCH_API", "API请求失败，不进行段落匹配")
                    return@launch
                }

                // 解析API返回的段落序号
                val matchedParaNum = try {
                    val numPattern = Regex("""#?(\d+)""")
                    val matchResult = numPattern.find(result)
                    val numStr = matchResult?.groupValues?.get(1)
                    if (numStr.isNullOrEmpty()) -1 else numStr.toIntOrNull() ?: -1
                } catch (e: Exception) {
                    -1
                }

                if (matchedParaNum > 0) {
                    Log.d("MATCH_API", "匹配段落序号: $matchedParaNum")
                    val diff = matchedParaNum - paragraphIndex
                    Log.d("MATCH_API", "差值: $diff")

                    if (kotlin.math.abs(diff) < 5) {
                        // get_review 使用的是 paragraphIndex - 1 + offset
                        // 所以 offset = (matchedParaNum - 1) - (paragraphIndex - 1) = matchedParaNum - paragraphIndex
                        CommentOffsetManager.offset += -diff - 1
                        Log.d("MATCH_API", "设置偏移: ${CommentOffsetManager.offset}")
                        // 刷新页面
                        ReaderBridge.readView?.upContent(0, true)
                    } else {
                        Log.d("MATCH_API", "差值过大，不设置偏移")
                    }

                    withContext(Dispatchers.Main) {
                        if (diff == -1) {
                            Toast.makeText(requireContext(), "当前匹配正确", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "已完成匹配", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.d("MATCH_API", "无法解析段落序号")
                }

                Log.d("MATCH_API", "=====================================================")

            } catch (e: Exception) {
                Log.e("MATCH_API", "处理匹配请求时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 发送匹配请求到API
     */
    private suspend fun sendMatchRequest(data: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val messagesJson = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", """
                    任务：匹配正文段落和评论，找出和评论最相关的正文。
                    只输出最匹配的段落序号。
                    禁止输出解释、分析、推理过程、额外文字。
                """.trimIndent())
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", data)
            })
        }

        // 从配置获取API密钥和模型名称
        val apiKey = ApiConfigDialog.getApiKey(requireContext())
        val apiModel = ApiConfigDialog.getApiModel(requireContext())

        if (apiKey.isEmpty()) {
            return "API密钥未配置，请在菜单中选择\"添加API\"进行配置"
        }

        val requestBodyJson = JSONObject().apply {
            put("model", apiModel)
            put("messages", messagesJson)
            put("enable_thinking", false)
        }

        val body = requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 401) {
                    // 401错误，弹窗提示用户检查API配置
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("API认证失败")
                            .setMessage("请求失败: 401\n\n请检查API Key是否正确配置")
                            .setPositiveButton("去设置") { _, _ ->
                                ApiConfigDialog.show(requireContext())
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
                return "请求失败: ${response.code}"
            }
            val responseBody = response.body?.string() ?: return "响应为空"
            val jsonResponse = JSONObject(responseBody)
            return jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            return "网络请求异常: ${e.message}"
        }
    }

}
