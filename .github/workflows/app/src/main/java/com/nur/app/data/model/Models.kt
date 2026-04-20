package com.nur.app.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    val id: String = "",
    val type: PostType = PostType.PRAYER,
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val likesCount: Int = 0,
    val bookmarksCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val scheduledAt: Timestamp? = null,
    val isPublished: Boolean = true,
    val notificationSent: Boolean = false
) : Parcelable {
    constructor() : this(id = "")
}

enum class PostType(val label: String, val emoji: String) {
    PRAYER("Prayer", "🤲"),
    REFLECTION("Reflection", "🌙"),
    GUIDANCE("Guidance", "💞");
    companion object {
        fun fromString(value: String) = values().firstOrNull { it.name == value } ?: PRAYER
    }
}

@Parcelize
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isAdmin: Boolean = false,
    val fcmToken: String? = null,
    val bookmarkedPostIds: List<String> = emptyList(),
    val likedPostIds: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
) : Parcelable {
    constructor() : this(uid = "")
}

data class NurNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val postId: String? = null,
    val postType: String? = null,
    val sentAt: Timestamp = Timestamp.now()
) {
    constructor() : this(id = "")
}
