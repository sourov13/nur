package com.nur.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nur.app.databinding.FragmentHomeBinding
import com.nur.app.ui.FeedAdapter
import com.nur.app.ui.PostsViewModel
import com.nur.app.util.hide
import com.nur.app.util.show
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!
    private val vm: PostsViewModel by activityViewModels()
    private lateinit var adapter: FeedAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        b.tvGreeting.text = when { hour < 12 -> "☀️ Good morning"; hour < 17 -> "🌤 Good afternoon"; else -> "🌙 Good evening" }

        adapter = FeedAdapter(
            onLike     = { vm.toggleLike(it.id) },
            onBookmark = { vm.toggleBookmark(it.id) },
            onShare    = { post ->
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${post.title}\n\n${post.body}\n\n— Nūr App")
                }, "Share"))
            }
        )
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
        b.swipeRefresh.setOnRefreshListener { b.swipeRefresh.isRefreshing = false }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.homeFeed.collect { posts ->
                _b ?: return@collect
                adapter.submitList(posts)
                if (posts.isEmpty()) b.tvEmpty.show() else b.tvEmpty.hide()
                b.swipeRefresh.isRefreshing = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.currentUser.collect { user -> _b ?: return@collect; adapter.currentUser = user; adapter.notifyDataSetChanged() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.error.collect { err -> _b ?: return@collect; err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show(); vm.clearError() } }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.actionResult.collect { msg -> _b ?: return@collect; msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show(); vm.clearActionResult() } }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
