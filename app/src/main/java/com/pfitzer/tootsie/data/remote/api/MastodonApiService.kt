package com.pfitzer.tootsie.data.remote.api

import com.pfitzer.tootsie.data.remote.dto.AccountDto
import com.pfitzer.tootsie.data.remote.dto.InstanceDto
import com.pfitzer.tootsie.data.remote.dto.MediaAttachmentDto
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API service for Mastodon API v1 endpoints.
 * Requires authentication (Bearer token in Authorization header).
 */
interface MastodonApiService {

    // ========== Account Endpoints ==========

    /**
     * Verify account credentials.
     * Returns the authenticated user's account information.
     */
    @GET("api/v1/accounts/verify_credentials")
    suspend fun verifyCredentials(): AccountDto

    /**
     * Get account information by ID.
     *
     * @param id Account ID
     */
    @GET("api/v1/accounts/{id}")
    suspend fun getAccount(@Path("id") id: String): AccountDto

    /**
     * Update account profile.
     * Only includes display_name and note for now; can be extended.
     */
    @FormUrlEncoded
    @POST("api/v1/accounts/update_credentials")
    suspend fun updateCredentials(
        @Field("display_name") displayName: String? = null,
        @Field("note") note: String? = null
    ): AccountDto

    /**
     * Get statuses posted by an account.
     *
     * @param id Account ID
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results (default 20, max 40)
     * @param excludeReplies Skip statuses that are replies to other statuses
     * @param excludeReblogs Skip statuses that are reblogs of other statuses
     * @param onlyMedia Only return statuses that have media attachments
     * @param pinned Only return statuses that are pinned
     */
    @GET("api/v1/accounts/{id}/statuses")
    suspend fun getAccountStatuses(
        @Path("id") id: String,
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("exclude_replies") excludeReplies: Boolean = false,
        @Query("exclude_reblogs") excludeReblogs: Boolean = false,
        @Query("only_media") onlyMedia: Boolean = false,
        @Query("pinned") pinned: Boolean = false
    ): List<StatusDto>

    /**
     * Get accounts following the given account.
     *
     * @param id Account ID
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param limit Maximum number of results
     */
    @GET("api/v1/accounts/{id}/followers")
    suspend fun getFollowers(
        @Path("id") id: String,
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("limit") limit: Int = 40
    ): List<AccountDto>

    /**
     * Get accounts that the given account is following.
     *
     * @param id Account ID
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param limit Maximum number of results
     */
    @GET("api/v1/accounts/{id}/following")
    suspend fun getFollowing(
        @Path("id") id: String,
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("limit") limit: Int = 40
    ): List<AccountDto>

    // ========== Relationship Endpoints ==========

    /**
     * Get relationship with a single account.
     *
     * @param id Account IDs (can be multiple, separated by comma in query)
     */
    @GET("api/v1/accounts/relationships")
    suspend fun getRelationships(
        @Query("id[]") id: List<String>
    ): List<com.pfitzer.tootsie.data.remote.dto.RelationshipDto>

    /**
     * Follow an account.
     *
     * @param id Account ID to follow
     * @param notify Receive notifications when this account posts a status
     * @param reblogs Show reblogs from this account
     */
    @FormUrlEncoded
    @POST("api/v1/accounts/{id}/follow")
    suspend fun followAccount(
        @Path("id") id: String,
        @Field("notify") notify: Boolean = false,
        @Field("reblogs") reblogs: Boolean = true
    ): com.pfitzer.tootsie.data.remote.dto.RelationshipDto

    /**
     * Unfollow an account.
     *
     * @param id Account ID to unfollow
     */
    @POST("api/v1/accounts/{id}/unfollow")
    suspend fun unfollowAccount(
        @Path("id") id: String
    ): com.pfitzer.tootsie.data.remote.dto.RelationshipDto

    // ========== Timeline Endpoints ==========

    /**
     * Get home timeline (posts from followed accounts).
     *
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results (default 20, max 40)
     */
    @GET("api/v1/timelines/home")
    suspend fun getHomeTimeline(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20
    ): List<StatusDto>

    /**
     * Get public timeline (all public posts from this instance).
     *
     * @param local Only show posts from this instance
     * @param remote Only show posts from remote instances
     * @param onlyMedia Only show posts with media
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results
     */
    @GET("api/v1/timelines/public")
    suspend fun getPublicTimeline(
        @Query("local") local: Boolean = false,
        @Query("remote") remote: Boolean = false,
        @Query("only_media") onlyMedia: Boolean = false,
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20
    ): List<StatusDto>

    // ========== Status Endpoints ==========

    /**
     * Get a status by ID.
     *
     * @param id Status ID
     */
    @GET("api/v1/statuses/{id}")
    suspend fun getStatus(@Path("id") id: String): StatusDto

    /**
     * Post a new status.
     *
     * @param status Text content (plain text or with mentions/hashtags)
     * @param inReplyToId ID of status being replied to
     * @param mediaIds Array of media IDs to attach
     * @param sensitive Mark media as sensitive
     * @param spoilerText Content warning text
     * @param visibility Status visibility (public, unlisted, private, direct)
     * @param language ISO 639 language code
     */
    @FormUrlEncoded
    @POST("api/v1/statuses")
    suspend fun postStatus(
        @Field("status") status: String,
        @Field("in_reply_to_id") inReplyToId: String? = null,
        @Field("media_ids[]") mediaIds: List<String>? = null,
        @Field("sensitive") sensitive: Boolean = false,
        @Field("spoiler_text") spoilerText: String? = null,
        @Field("visibility") visibility: String = "public",
        @Field("language") language: String? = null
    ): StatusDto

    /**
     * Delete a status.
     *
     * @param id Status ID to delete
     */
    @DELETE("api/v1/statuses/{id}")
    suspend fun deleteStatus(@Path("id") id: String): StatusDto

    /**
     * Favorite (like) a status.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/favourite")
    suspend fun favoriteStatus(@Path("id") id: String): StatusDto

    /**
     * Unfavorite a status.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/unfavourite")
    suspend fun unfavoriteStatus(@Path("id") id: String): StatusDto

    /**
     * Reblog (boost/share) a status.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/reblog")
    suspend fun reblogStatus(@Path("id") id: String): StatusDto

    /**
     * Unreblog a status.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/unreblog")
    suspend fun unreblogStatus(@Path("id") id: String): StatusDto

    /**
     * Bookmark a status.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/bookmark")
    suspend fun bookmarkStatus(@Path("id") id: String): StatusDto

    /**
     * Unbookmark a status.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/unbookmark")
    suspend fun unbookmarkStatus(@Path("id") id: String): StatusDto

    /**
     * Get conversation context for a status.
     * Returns ancestors (posts before) and descendants (replies after).
     *
     * @param id Status ID
     */
    @GET("api/v1/statuses/{id}/context")
    suspend fun getContext(@Path("id") id: String): com.pfitzer.tootsie.data.remote.dto.ContextDto

    // ========== Media Endpoints ==========

    /**
     * Upload a media file (image, video, audio).
     * Uses Mastodon API v2 which returns an immediate MediaAttachment.
     *
     * @param file The media file to upload (multipart)
     * @param description Alt text description for accessibility (optional)
     * @param focus Focus point for cropping (x,y coordinates from -1 to 1, optional)
     * @return MediaAttachmentDto with the uploaded media information
     */
    @Multipart
    @POST("api/v2/media")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody? = null,
        @Part("focus") focus: RequestBody? = null
    ): MediaAttachmentDto

    // ========== Notification Endpoints ==========

    /**
     * Get notifications for the authenticated user.
     *
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results (default 20, max 40)
     * @param excludeTypes Array of notification types to exclude
     * @param accountId Only show notifications from this account
     */
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("exclude_types[]") excludeTypes: List<String>? = null,
        @Query("account_id") accountId: String? = null
    ): List<com.pfitzer.tootsie.data.remote.dto.NotificationDto>

    /**
     * Get a single notification by ID.
     *
     * @param id Notification ID
     */
    @GET("api/v1/notifications/{id}")
    suspend fun getNotification(@Path("id") id: String): com.pfitzer.tootsie.data.remote.dto.NotificationDto

    /**
     * Dismiss a single notification.
     *
     * @param id Notification ID to dismiss
     */
    @POST("api/v1/notifications/{id}/dismiss")
    suspend fun dismissNotification(@Path("id") id: String)

    /**
     * Clear all notifications.
     */
    @POST("api/v1/notifications/clear")
    suspend fun clearAllNotifications()

    // ========== Instance Endpoints ==========

    /**
     * Get instance information.
     * Does not require authentication.
     */
    @GET("api/v1/instance")
    suspend fun getInstance(): InstanceDto
}
