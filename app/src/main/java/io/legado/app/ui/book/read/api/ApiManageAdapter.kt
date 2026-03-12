package io.legado.app.ui.book.read.api

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.databinding.ItemApiConfigBinding

/**
 * API配置列表适配器
 */
class ApiManageAdapter(
    private val callBack: CallBack
) : ListAdapter<ApiConfig, ApiManageAdapter.ViewHolder>(DiffCallback) {

    interface CallBack {
        fun onEdit(config: ApiConfig)
        fun onDelete(config: ApiConfig)
        fun onSetDefault(config: ApiConfig)
    }

    private object DiffCallback : DiffUtil.ItemCallback<ApiConfig>() {
        override fun areItemsTheSame(oldItem: ApiConfig, newItem: ApiConfig): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ApiConfig, newItem: ApiConfig): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(private val binding: ItemApiConfigBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ApiConfig) {
            binding.apply {
                tvName.text = item.name
                tvModel.text = item.apiModel

                // 默认标记
                if (item.isDefault) {
                    tvDefault.visibility = android.view.View.VISIBLE
                    btnSetDefault.visibility = android.view.View.GONE
                } else {
                    tvDefault.visibility = android.view.View.GONE
                    btnSetDefault.visibility = android.view.View.VISIBLE
                }

                // 点击整个卡片进行编辑
                root.setOnClickListener {
                    callBack.onEdit(item)
                }

                // 设置默认
                btnSetDefault.setOnClickListener {
                    callBack.onSetDefault(item)
                }

                // 编辑
                btnEdit.setOnClickListener {
                    callBack.onEdit(item)
                }

                // 删除
                btnDelete.setOnClickListener {
                    callBack.onDelete(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApiConfigBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
