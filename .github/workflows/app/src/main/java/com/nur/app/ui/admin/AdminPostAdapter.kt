package com.nur.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nur.app.data.model.Post
import com.nur.app.databinding.ItemAdminPostBinding
import com.nur.app.util.hide
import com.nur.app.util.loadUrl
import com.nur.app.util.show

class AdminPostAdapter(
    private val onEdit:          (Post) -> Unit,
    private val onDelete:        (Post) -> Unit,
    private val onTogglePublish: (Post) -> Unit
) : ListAdapter<Post, AdminPostAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAdminPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemAdminPostBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(post: Post) {
            b.tvAdminPostTitle.text = post.title
            b.tvAdminPostType.text  = "${post.type.emoji} ${post.type.label}"
            b.tvAdminPostDate.text  = android.text.format.DateFormat.format("MMM dd, yyyy", post.createdAt.toDate()).toString()
            b.tvStatus.text = if (post.isPublished) "● Published" else "◌ Scheduled / Draft"
            b.tvStatus.setTextColor(if (post.isPublished) 0xFF4CAF50.toInt() else 0xFFFFA726.toInt())
            if (!post.imageUrl.isNullOrEmpty()) { b.ivAdminThumb.show(); b.ivAdminThumb.loadUrl(post.imageUrl) }
            else b.ivAdminThumb.hide()
            b.btnAdminEdit.setOnClickListener   { onEdit(post) }
            b.btnAdminDelete.setOnClickListener { onDelete(post) }
            b.btnAdminPublish.setOnClickListener { onTogglePublish(post) }
            b.btnAdminPublish.text      = if (post.isPublished) "Published" else "Publish Now"
            b.btnAdminPublish.isEnabled = !post.isPublished
        }
    }

    class Diff : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(a: Post, b: Post) = a.id == b.id
        override fun areContentsTheSame(a: Post, b: Post) = a == b
    }
}
