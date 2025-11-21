package io.legado.app.ui.book.read

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.transition.Transition
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import io.legado.app.R
import io.legado.app.help.QdCat.get_sub_review
import io.legado.app.help.ReviewThread
import io.legado.app.help.config.AppConfig.isNightTheme
import kotlinx.coroutines.*
import org.json.JSONObject

class ReviewAdapter(private val threads: List<ReviewThread>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewVH>() {

    private val expandedSet = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewVH(v)
    }

    override fun getItemCount() = threads.size

    override fun onBindViewHolder(holder: ReviewVH, position: Int) {
        val thread = threads[position]
        val expanded = expandedSet.contains(position)

        holder.bind(thread, expanded)

        holder.tvExpandReply?.setOnClickListener {

            expandedSet.add(position)

            val root = thread.root

            // 需要加载子评论
            if (!thread.repliesLoaded && root.optInt("rootReviewReplyCount") > 0) {

                CoroutineScope(Dispatchers.IO).launch {
                    val rootId = root.optString("reviewId")
                    val replies = get_sub_review(rootId)

                    thread.replies.clear()
                    for (i in 0 until replies.length()) {
                        thread.replies.add(replies.getJSONObject(i))
                    }

                    thread.repliesLoaded = true

                    withContext(Dispatchers.Main) {
                        notifyItemChanged(position)
                    }
                }
            } else {
                notifyItemChanged(position)
            }
        }
    }

    class ReviewVH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvUser = view.findViewById<TextView>(R.id.tvUser)
        private val tvContent = view.findViewById<TextView>(R.id.tvContent)
        private val tvTime = view.findViewById<TextView>(R.id.tvTime)
        private val imgAvatar = view.findViewById<ImageView>(R.id.imgAvatar)

        private val likes = view.findViewById<TextView>(R.id.tvLike)

        val tvExpandReply = view.findViewById<TextView>(R.id.tvExpandReply)
        private val layoutReply = view.findViewById<LinearLayout>(R.id.layoutReply)
        private val tvReplyContent = view.findViewById<TextView>(R.id.tvReplyContent)
        private val line = view.findViewById<View>(R.id.fgline)

        fun setQidianEmojiText(textView: TextView, raw: String) {

            val pattern = Regex("\\[fn=(\\d+)\\]")
            val spannable = SpannableStringBuilder(raw)

            val matches = pattern.findAll(raw).toList()

            for (m in matches) {
                val id = m.groupValues[1]
                val start = m.range.first
                val end = m.range.last + 1

                val url = "https://qdfepccdn.qidian.com/gtimg/app_emoji_new/newface_${id}.png"

                com.bumptech.glide.Glide.with(textView.context)
                    .asBitmap()
                    .load(url)
                    .into(object :
                        com.bumptech.glide.request.target.CustomTarget<Bitmap>() {

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                        ) {

                            val size = (textView.textSize * 1.2f).toInt()
                            val scaled = Bitmap.createScaledBitmap(resource, size, size, true)

                            val span = ImageSpan(textView.context, scaled, ImageSpan.ALIGN_BOTTOM)

                            spannable.setSpan(
                                span,
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            textView.text = spannable
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }

            textView.text = spannable
        }

        fun bind(thread: ReviewThread, expanded: Boolean) {

            itemView.setBackgroundColor(
                if (isNightTheme) 0xFF1C1C1C.toInt() else 0xFFF5F5F5.toInt()
            )
            if (isNightTheme)
                line.setBackgroundColor(0xFF333333.toInt())

            val root = thread.root

            tvUser.text = root.optString("nickName")
            setQidianEmojiText(tvContent, root.optString("content"))
            tvTime.text = root.optString("level") + "楼 · " + root.optString("createTime") + " · " + root.optString("ipAddress")
            likes.text = root.optString("likeCount").toString()

            Glide.with(itemView.context)
                .load(root.optString("avatar"))
                .apply(RequestOptions().circleCrop())
                .into(imgAvatar)

            val replyCount = root.optInt("rootReviewReplyCount")

            if (replyCount > 0) {

                if (!expanded) {
                    tvExpandReply.visibility = View.VISIBLE
                    tvExpandReply.text = "展开回复（${replyCount}）"
                    layoutReply.visibility = View.GONE
                    return
                }

                // 展开状态
                tvExpandReply.visibility = View.GONE
                layoutReply.visibility = View.VISIBLE

                if (!thread.repliesLoaded) {
                    tvReplyContent.text = "加载中…"
                    return
                }

// --- 子评论（最多显示 N 个，一条一条加进去） ---
                layoutReply.removeAllViews()  // 清空旧的子评论

                for (rep in thread.replies) {
                    val child = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_reply, layoutReply, false)

                    child.setBackgroundColor(
                        if (isNightTheme) 0xFF1C1C1C.toInt() else 0xFFF5F5F5.toInt()
                    )

                    val img = child.findViewById<ImageView>(R.id.imgReplyAvatar)
                    val user = child.findViewById<TextView>(R.id.tvReplyUser)
                    val content = child.findViewById<TextView>(R.id.tvReplyContent)
                    val time = child.findViewById<TextView>(R.id.tvReplyTime)
                    val likes = child.findViewById<TextView>(R.id.subtvLike)

                    user.setTextColor(
                        if (isNightTheme) 0xFFDDDDDD.toInt() else 0xFF1C1C1C.toInt()
                    )

                    user.text = rep.optString("nickName")
                    setQidianEmojiText(content, rep.optString("content"))
                    content.text = rep.optString("content")
                    time.text = rep.optString("level") + "楼 · " + rep.optString("createTime") + " · " + rep.optString("ipAddress")
                    likes.text = rep.optString("likeCount")

                    Glide.with(itemView.context)
                        .load(rep.optString("avatar"))
                        .apply(RequestOptions().circleCrop())
                        .into(img)

                    layoutReply.addView(child)
                }

                layoutReply.visibility = View.VISIBLE


            } else {
                tvExpandReply.visibility = View.GONE
                layoutReply.visibility = View.GONE
            }
        }
    }



}
