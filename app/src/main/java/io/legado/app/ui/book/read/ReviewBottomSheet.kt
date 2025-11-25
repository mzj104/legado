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
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.legado.app.help.QdCat
import io.legado.app.help.QdCat.get_review
import io.legado.app.help.ReviewThread
import io.legado.app.help.Simicatalog
import io.legado.app.help.config.AppConfig.isNightTheme
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.ContentTextView
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.ReaderBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray


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

// ------ 中间标题 ------
        val titleView = AppCompatTextView(requireContext()).apply {
            text = "全部评论"
            textSize = 18f
            gravity = Gravity.CENTER
            if (isNightTheme) setTextColor(0xFFCCCCCC.toInt())
            else setTextColor(0xFF333333.toInt())
        }

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

        header.addView(titleView, FrameLayout.LayoutParams(
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
            }

            withContext(Dispatchers.Main) {
                // ⭐ 更新评论数量
                titleView.text = "全部（${threads.size}）"

                progressBar?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                recyclerView?.adapter = ReviewAdapter(threads)
            }
        }
    }



}
