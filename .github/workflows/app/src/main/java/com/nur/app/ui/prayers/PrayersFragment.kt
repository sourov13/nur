package com.nur.app.ui.prayers

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nur.app.databinding.FragmentFeedBinding
import com.nur.app.ui.FeedAdapter
import com.nur.app.ui.PostsViewModel
import com.nur.app.util.hide
import com.nur.app.util.show
import kotlinx.coroutines.launch

class PrayersFragment : Fragment() {
    private var _b: FragmentFeedBinding? = null
    private val b get() = _b!!
    private val vm: PostsViewModel by activityViewModels()
    private lateinit var adapter: FeedAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentFeedBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.tvFeedLabel.text = "Daily Prayers"
        b.tvFeedSubtitle.text = "A new prayer posted for you each day"
        adapter = FeedAdapter(
            onLike = { vm.toggleLike(it.id) },
            onBookmark = { vm.toggleBookmark(it.id) },
            onShare = { post ->
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${post.title}\n\n${post.body}\n\n— Nūr App")
                }, "Share Prayer"))
            }
        )
        b.recyclerFeed.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerFeed.adapter = adapter
        b.swipeRefresh.setOnRefreshListener { b.swipeRefresh.isRefreshing = false }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.prayers.collect { posts -> _b ?: return@collect; adapter.submitList(posts); if (posts.isEmpty()) b.tvEmpty.show() else b.tvEmpty.hide() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.currentUser.collect { user -> _b ?: return@collect; adapter.currentUser = user; adapter.notifyDataSetChanged() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.error.collect { err -> _b ?: return@collect; err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show(); vm.clearError() } }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
