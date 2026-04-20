package com.nur.app.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.nur.app.data.model.NurNotification
import com.nur.app.data.model.Post
import com.nur.app.data.model.PostType
import com.nur.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val postsRef         = db.collection("posts")
    private val usersRef         = db.collection("users")
    private val notificationsRef = db.collection("notifications")

    val currentUserId: String? get() = auth.currentUser?.uid
    val isLoggedIn:    Boolean  get() = auth.currentUser != null

    // ── Auth ──────────────────────────────────────────────────────
    suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        getOrCreateUser(result.user!!.uid, email, result.user?.displayName ?: "")
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid  = result.user!!.uid
        val user = User(uid = uid, email = email, displayName = displayName)
        usersRef.document(uid).set(user).await()
        user
    }

    fun signOut() = auth.signOut()

    private suspend fun getOrCreateUser(uid: String, email: String, displayName: String): User {
        val doc = usersRef.document(uid).get().await()
        return if (doc.exists()) doc.toObject(User::class.java)!!
        else {
            val user = User(uid = uid, email = email, displayName = displayName)
            usersRef.document(uid).set(user).await()
            user
        }
    }

    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return usersRef.document(uid).get().await().toObject(User::class.java)
    }

    suspend fun updateFcmToken(token: String) {
        val uid = currentUserId ?: return
        usersRef.document(uid).update("fcmToken", token).await()
    }

    // ── Posts – live flows ────────────────────────────────────────
    fun getHomeFeedFlow(): Flow<List<Post>> = callbackFlow {
        val listener = postsRef
            .whereEqualTo("isPublished", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull {
                    it.toObject(Post::class.java)?.copy(id = it.id)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getPostsFlow(type: PostType): Flow<List<Post>> = callbackFlow {
        val listener = postsRef
            .whereEqualTo("type", type.name)
            .whereEqualTo("isPublished", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull {
                    it.toObject(Post::class.java)?.copy(id = it.id)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ── Admin CRUD ────────────────────────────────────────────────
    suspend fun createPost(
        type: PostType, title: String, body: String,
        category: String, tags: List<String>,
        imageUri: Uri?, scheduledAt: Timestamp?, authorName: String
    ): Result<Post> = runCatching {
        val uid      = currentUserId ?: throw IllegalStateException("Not authenticated")
        val imageUrl = imageUri?.let { uploadImage(it, "posts") }
        val isPublished = scheduledAt == null || scheduledAt <= Timestamp.now()
        val docRef   = postsRef.document()
        val post = Post(
            id = docRef.id, type = type, title = title, body = body,
            imageUrl = imageUrl, authorId = uid, authorName = authorName,
            category = category, tags = tags,
            scheduledAt = scheduledAt, isPublished = isPublished,
            createdAt = Timestamp.now()
        )
        docRef.set(post).await()
        post
    }

    suspend fun updatePost(
        postId: String, title: String, body: String,
        category: String, tags: List<String>, newImageUri: Uri?
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "title" to title, "body" to body,
            "category" to category, "tags" to tags
        )
        newImageUri?.let { updates["imageUrl"] = uploadImage(it, "posts") }
        postsRef.document(postId).update(updates).await()
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        postsRef.document(postId).delete().await()
    }

    suspend fun publishScheduledPost(postId: String): Result<Unit> = runCatching {
        postsRef.document(postId).update("isPublished", true).await()
    }

    suspend fun getAllPostsAdmin(): List<Post> =
        postsRef.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            .documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }

    // ── Like / Bookmark ───────────────────────────────────────────
    suspend fun toggleLike(postId: String): Result<Unit> = runCatching {
        val uid  = currentUserId ?: throw IllegalStateException("Not authenticated")
        val user = usersRef.document(uid).get().await().toObject(User::class.java) ?: return@runCatching
        val liked = user.likedPostIds.toMutableList()
        if (liked.contains(postId)) {
            liked.remove(postId)
            postsRef.document(postId).update("likesCount", FieldValue.increment(-1)).await()
        } else {
            liked.add(postId)
            postsRef.document(postId).update("likesCount", FieldValue.increment(1)).await()
        }
        usersRef.document(uid).update("likedPostIds", liked).await()
    }

    suspend fun toggleBookmark(postId: String): Result<Unit> = runCatching {
        val uid  = currentUserId ?: throw IllegalStateException("Not authenticated")
        val user = usersRef.document(uid).get().await().toObject(User::class.java) ?: return@runCatching
        val bm   = user.bookmarkedPostIds.toMutableList()
        if (bm.contains(postId)) {
            bm.remove(postId)
            postsRef.document(postId).update("bookmarksCount", FieldValue.increment(-1)).await()
        } else {
            bm.add(postId)
            postsRef.document(postId).update("bookmarksCount", FieldValue.increment(1)).await()
        }
        usersRef.document(uid).update("bookmarkedPostIds", bm).await()
    }

    suspend fun getBookmarkedPosts(): List<Post> {
        val uid  = currentUserId ?: return emptyList()
        val user = usersRef.document(uid).get().await().toObject(User::class.java) ?: return emptyList()
        if (user.bookmarkedPostIds.isEmpty()) return emptyList()
        return postsRef.whereIn("__name__", user.bookmarkedPostIds.take(10)).get().await()
            .documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
    }

    // ── Storage ───────────────────────────────────────────────────
    suspend fun uploadImage(uri: Uri, folder: String): String {
        val ref = storage.reference.child("$folder/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // ── Notifications ─────────────────────────────────────────────
    suspend fun saveNotificationRecord(title: String, body: String, postId: String?): Result<Unit> = runCatching {
        val notif = NurNotification(id = notificationsRef.document().id, title = title, body = body, postId = postId)
        notificationsRef.document(notif.id).set(notif).await()
    }
}
