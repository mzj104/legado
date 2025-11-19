package io.legado.app.ui.book.read

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.legado.app.help.QdCat.get_review
import io.legado.app.help.Simicatalog
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        // 外层容器（固定高度）
        val root = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (requireContext().resources.displayMetrics.heightPixels * 0.7).toInt()  // 固定高度：屏幕 55%
        )
        root.layoutParams = params

        // 1) 加载中
        progressBar = ProgressBar(requireContext()).apply {
            val p = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            p.gravity = Gravity.CENTER
            layoutParams = p
        }
        root.addView(progressBar)

        // 2) 评论列表
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            visibility = View.GONE   // 初始不显示
        }
        root.addView(recyclerView)

        // 开始加载评论
        loadReviews()

        return root
    }

    private fun loadReviews() {
        CoroutineScope(Dispatchers.IO).launch {
            val qdId = Simicatalog.get_qdchapter_id("", chapterIndex)
            val reviews = get_review("1035420986", qdId, paragraphIndex - 1)

            withContext(Dispatchers.Main) {
                progressBar?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                recyclerView?.adapter = ReviewAdapter(reviews)
            }
        }
    }
}
