package io.legado.app.ui.book.read
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import org.json.JSONArray
import org.json.JSONObject

class ReviewAdapter(private val arr: JSONArray) :
    RecyclerView.Adapter<ReviewAdapter.ReviewVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewVH(v)
    }

    override fun getItemCount(): Int = arr.length()

    override fun onBindViewHolder(holder: ReviewVH, position: Int) {
        holder.bind(arr.getJSONObject(position))
    }

    class ReviewVH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvUser = view.findViewById<TextView>(R.id.tvUser)
        private val tvContent = view.findViewById<TextView>(R.id.tvContent)
        private val tvTime = view.findViewById<TextView>(R.id.tvTime)
        private val imgAvatar = view.findViewById<ImageView>(R.id.imgAvatar)

        fun bind(o: JSONObject) {
            val nickname = o.optString("nickName")
            val content = o.optString("content")
            val time = o.optString("createTime")
            val ip = o.optString("ipAddress")
            val avatar = o.optString("avatar")

            tvUser.text = nickname
            tvContent.text = content
            tvTime.text = "$time · $ip"

            if (avatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(avatar)
                    .apply(
                        RequestOptions()
                    )
                    .into(imgAvatar)
        }
    }
}
}