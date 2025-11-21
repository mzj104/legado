package io.legado.app.ui.book.read

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.legado.app.help.QdCat
import io.legado.app.help.QdCat.get_review
import io.legado.app.help.ReviewThread
import io.legado.app.help.Simicatalog
import io.legado.app.help.config.AppConfig.isNightTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class ReviewBottomSheet(
    private val chapterIndex: Int,
    private val paragraphIndex: Int
) : BottomSheetDialogFragment() {

    private var recyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null

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
        val header = androidx.appcompat.widget.AppCompatTextView(requireContext()).apply {
            text = "全部评论"
            textSize = 18f
            setPadding(32, 24, 32, 24)

            if (isNightTheme) {
                setTextColor(0xFFCCCCCC.toInt())   // 对应 #333333 的夜间亮灰
                setBackgroundColor(0xFF1C1C1C.toInt()) // 对应 #F5F5F5 的深灰背景
            }
            else{
                setTextColor(0xFF333333.toInt())
                setBackgroundColor(0xFFF5F5F5.toInt())
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        val headerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        header.layoutParams = headerParams
        root.addView(header)


        // ============= 2. ProgressBar（保持不动） =============
        progressBar = ProgressBar(requireContext()).apply {
            val p = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            p.gravity = Gravity.CENTER
            layoutParams = p
        }
        root.addView(progressBar)


        // ============= 3. RecyclerView（放在 Header 下方） =============
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            visibility = View.GONE
        }

        val rvParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        rvParams.topMargin = dp(55)   // ⭐ 关键：让评论列表从标题栏下方开始
        recyclerView!!.layoutParams = rvParams

        root.addView(recyclerView)


        // 加载评论
        loadReviews(header)

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

    private fun loadReviews(header: TextView) {

        CoroutineScope(Dispatchers.IO).launch {
            val qdId = Simicatalog.get_qdchapter_id("", chapterIndex)
            val rawArr = get_review(QdCat.nowqdbookid, qdId, paragraphIndex - 1)

            val threads = MutableList(rawArr.length()) { i ->
                ReviewThread(rawArr.getJSONObject(i))
            }

            withContext(Dispatchers.Main) {
                header.text = "全部（${threads.size}）"   // ⭐ 更新数量

                progressBar?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                recyclerView?.adapter = ReviewAdapter(threads)
            }
        }
    }


}
