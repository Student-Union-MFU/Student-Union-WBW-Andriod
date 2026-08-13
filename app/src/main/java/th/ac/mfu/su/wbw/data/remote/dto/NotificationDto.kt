package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /notifications item — a notification delivered to the logged-in
 * participant (audience all + group/school/personal). `readAt` is present only
 * on the authenticated feed.
 */
@Serializable
data class Notification(
    val id: Long,
    val type: String,
    val title: String,
    val body: String? = null,
    val level: String,
    val audience: String,
    @SerialName("audience_id") val audienceId: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
)

/**
 * GET /notifications/public item — public announcements (audience='all'),
 * readable without logging in. No recipient/read data.
 */
@Serializable
data class NotificationPublic(
    val id: Long,
    val type: String,
    val title: String,
    val body: String? = null,
    val level: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("creator_name") val creatorName: String? = null,
)
