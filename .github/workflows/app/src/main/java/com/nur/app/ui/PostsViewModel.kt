package com.nur.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.app.data.model.Post
import com.nur.app.data.model.PostType
import com.nur.app.data.model.User
import com.nur.app.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostsViewModel : ViewModel() {

    private val _homeFeed    = MutableStateFlow<List<Post>>(emptyList())
    val homeFeed: StateFlow<List<Post>> = _homeFeed

    private val _prayers     = MutableStateFlow<List<Post>>(emptyList())
    val prayers: StateFlow<List<Post>> = _prayers

    private val _reflections = MutableStateFlow<List<Post>>(emptyList())
    val reflections: StateFlow<List<Post>> = _reflections

    private val _guidance    = MutableStateFlow<List<Post>>(emptyList())
    val guidance: StateFlow<List<Post>> = _guidance

    private val _bookmarks   = MutableStateFlow<List<Post>>(emptyList())
    val bookmarks: StateFlow<List<Post>> = _bookmarks

    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    private val _error        = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult

    init {
        loadCurrentUser()
        viewModelScope.launch { FirebaseRepository.getHomeFeedFlow().collect    { _homeFeed.value    = it } }
        viewModelScope.launch { FirebaseRepository.getPostsFlow(PostType.PRAYER).collect     { _prayers.value     = it } }
        viewModelScope.launch { FirebaseRepository.getPostsFlow(PostType.REFLECTION).collect { _reflections.value = it } }
        viewModelScope.launch { FirebaseRepository.getPostsFlow(PostType.GUIDANCE).collect   { _guidance.value    = it } }
    }

    private fun loadCurrentUser() = viewModelScope.launch {
        _currentUser.value = FirebaseRepository.getCurrentUser() ?: User()
    }

    fun loadBookmarks() = viewModelScope.launch {
        _bookmarks.value = FirebaseRepository.getBookmarkedPosts()
    }

    fun toggleLike(postId: String) = viewModelScope.launch {
        FirebaseRepository.toggleLike(postId).onFailure { _error.value = it.localizedMessage }
        loadCurrentUser()
    }

    fun toggleBookmark(postId: String) = viewModelScope.launch {
        FirebaseRepository.toggleBookmark(postId)
            .onSuccess { _actionResult.value = "Bookmark updated" }
            .onFailure { _error.value = it.localizedMessage }
        loadCurrentUser()
    }

    fun clearError()        { _error.value = null }
    fun clearActionResult() { _actionResult.value = null }
}
