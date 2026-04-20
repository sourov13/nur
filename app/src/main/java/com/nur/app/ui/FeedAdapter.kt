package com.nur.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.nur.app.data.model.Post
import com.nur.app.data.model.User
import com.nur.app.databinding.ItemFeedPostBinding
import com.nur.app.util.hide
import com.nur.app.util.loadCircle
import com.nur.app.util.loadUrl
import com.nur.app.util.show

class FeedAdapter(
    private val onLike:     (Post) -> Unit,
    private val onBookmark: (Post) -> Unit,
    private val onShare:    (Post) -> Unit
) : ListAdapter<Post, FeedAdapter.VH>(Diff()) {

    var currentUser: User = User()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFeedPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemFeedPostBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(post: Post) {
            b.ivAvatar.loadCircle(post.authorAvatarUrl)
            b.tvAuthorName.text = post.authorName.ifEmpty { "Nūr" }
            b.tvTime.text = try { TimeAgo.using(post.createdAt.toDate().time) } catch (e: Exception) { "" }
            b.tvTypeBadge.text = "${post.type.emoji} ${post.type.label}"

            // Hero image — show actual uploaded image if available
            if (!post.imageUrl.isNullOrEmpty()) {
                b.ivHeroImage.show()
                b.ivHeroImage.loadUrl(post.imageUrl)
            } else {
                b.ivHeroImage.hide()
            }

            b.tvTitle.text = post.title

            if (post.body.isNotEmpty()) { b.tvBody.show(); b.tvBody.text = post.body }
            else b.tvBody.hide()

            if (post.category.isNotEmpty()) { b.tvCategory.show(); b.tvCategory.text = "# ${post.category}" }
            else b.tvCategory.hide()

            // Like state
            val isLiked = currentUser.likedPostIds.contains(post.id)
            b.btnLike.setImageResource(
                if (isLiked) com.nur.app.R.drawable.ic_heart_filled else com.nur.app.R.drawable.ic_heart
            )
            b.tvLikeCount.text = if (post.likesCount > 0) post.likesCount.toString() else ""

            // Bookmark state
            val isBookmarked = currentUser.bookmarkedPostIds.contains(post.id)
            b.btnBookmark.setImageResource(
                if (isBookmarked) com.nur.app.R.drawable.ic_bookmark_filled else com.nur.app.R.drawable.ic_bookmark_outline
            )

            b.btnLike.setOnClickListener     { onLike(post) }
            b.btnBookmark.setOnClickListener { onBookmark(post) }
            b.btnShare.setOnClickListener    { onShare(post) }

            // Expand/collapse body text
            var expanded = false
            b.tvBody.setOnClickListener {
                expanded = !expanded
                b.tvBody.maxLines = if (expanded) Int.MAX_VALUE else 4
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(a: Post, b: Post) = a.id == b.id
        override fun areContentsTheSame(a: Post, b: Post) = a == b
    }
}
