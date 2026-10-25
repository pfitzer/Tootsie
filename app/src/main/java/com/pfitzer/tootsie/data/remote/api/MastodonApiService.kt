package com.pfitzer.tootsie.data.remote.api

import com.pfitzer.tootsie.data.remote.dto.AccountDto
import com.pfitzer.tootsie.data.remote.interceptors.HEADER_INSTANCE
import com.pfitzer.tootsie.data.remote.dto.AnnouncementDto
import com.pfitzer.tootsie.data.remote.dto.FilterDto
import com.pfitzer.tootsie.data.remote.dto.ReportDto
import com.pfitzer.tootsie.data.remote.dto.FilterKeywordDto
import com.pfitzer.tootsie.data.remote.dto.FollowedTagDto
import com.pfitzer.tootsie.data.remote.dto.InstanceDto
import com.pfitzer.tootsie.data.remote.dto.ListDto
import com.pfitzer.tootsie.data.remote.dto.MediaAttachmentDto
import com.pfitzer.tootsie.data.remote.dto.SearchResultsDto
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import com.pfitzer.tootsie.data.remote.dto.TagDto
import com.pfitzer.tootsie.data.remote.dto.TrendingLinkDto
import com.pfitzer.tootsie.data.remote.dto.TrendingTagDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
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

    // ========== Mute/Block Endpoints ==========

    /**
     * Mute an account.
     * Hides the account's posts from timelines and notifications.
     *
     * @param id Account ID to mute
     * @param notifications Whether to mute notifications (default true)
     * @param duration Duration of mute in seconds (0 = indefinite)
     */
    @FormUrlEncoded
    @POST("api/v1/accounts/{id}/mute")
    suspend fun muteAccount(
        @Path("id") id: String,
        @Field("notifications") notifications: Boolean = true,
        @Field("duration") duration: Int = 0
    ): com.pfitzer.tootsie.data.remote.dto.RelationshipDto

    /**
     * Unmute an account.
     *
     * @param id Account ID to unmute
     */
    @POST("api/v1/accounts/{id}/unmute")
    suspend fun unmuteAccount(
        @Path("id") id: String
    ): com.pfitzer.tootsie.data.remote.dto.RelationshipDto

    /**
     * Block an account.
     * Prevents the account from following you and viewing your content.
     *
     * @param id Account ID to block
     */
    @POST("api/v1/accounts/{id}/block")
    suspend fun blockAccount(
        @Path("id") id: String
    ): com.pfitzer.tootsie.data.remote.dto.RelationshipDto

    /**
     * Unblock an account.
     *
     * @param id Account ID to unblock
     */
    @POST("api/v1/accounts/{id}/unblock")
    suspend fun unblockAccount(
        @Path("id") id: String
    ): com.pfitzer.tootsie.data.remote.dto.RelationshipDto

    /**
     * Get muted accounts.
     * Returns Response to access Link header for pagination.
     *
     * @param maxId Return results older than this ID (internal relationship ID from Link header)
     * @param sinceId Return results newer than this ID
     * @param limit Maximum number of results (default 40)
     */
    @GET("api/v1/mutes")
    suspend fun getMutedAccounts(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("limit") limit: Int = 40
    ): retrofit2.Response<List<AccountDto>>

    /**
     * Get blocked accounts.
     * Returns Response to access Link header for pagination.
     *
     * @param maxId Return results older than this ID (internal relationship ID from Link header)
     * @param sinceId Return results newer than this ID
     * @param limit Maximum number of results (default 40)
     */
    @GET("api/v1/blocks")
    suspend fun getBlockedAccounts(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("limit") limit: Int = 40
    ): retrofit2.Response<List<AccountDto>>

    // ========== Domain Block Endpoints ==========

    /**
     * Get blocked domains.
     * Returns Response to access Link header for pagination.
     *
     * @param maxId Return results older than this ID (internal block ID from Link header)
     * @param limit Maximum number of results (default 100, max 200)
     */
    @GET("api/v1/domain_blocks")
    suspend fun getBlockedDomains(
        @Query("max_id") maxId: String? = null,
        @Query("limit") limit: Int = 100
    ): retrofit2.Response<List<String>>

    /**
     * Block a domain: hides all its posts and notifications, and removes followers from it.
     *
     * @param domain Domain to block, e.g. "example.social"
     */
    @FormUrlEncoded
    @POST("api/v1/domain_blocks")
    suspend fun blockDomain(
        @Field("domain") domain: String
    )

    /**
     * Unblock a domain.
     *
     * @param domain Domain to unblock
     */
    // Query parameter instead of a form body: Retrofit rejects @FormUrlEncoded on DELETE
    @DELETE("api/v1/domain_blocks")
    suspend fun unblockDomain(
        @Query("domain") domain: String
    )

    // ========== Follow Requests Endpoints ==========

    /**
     * Get pending follow requests.
     *
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param limit Maximum number of results (default 40)
     */
    @GET("api/v1/follow_requests")
    suspend fun getFollowRequests(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("limit") limit: Int = 40
    ): List<com.pfitzer.tootsie.data.remote.dto.AccountDto>

    /**
     * Authorize a follow request.
     *
     * @param id Account ID to authorize
     */
    @POST("api/v1/follow_requests/{id}/authorize")
    suspend fun authorizeFollowRequest(
        @Path("id") id: String
    ): com.pfitzer.tootsie.data.remote.dto.RelationshipDto

    /**
     * Reject a follow request.
     *
     * @param id Account ID to reject
     */
    @POST("api/v1/follow_requests/{id}/reject")
    suspend fun rejectFollowRequest(
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

    /**
     * Get hashtag timeline (public posts containing a specific hashtag).
     *
     * @param hashtag The hashtag name (without the # symbol)
     * @param local Only show posts from this instance
     * @param onlyMedia Only show posts with media
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results (default 20, max 40)
     */
    @GET("api/v1/timelines/tag/{hashtag}")
    suspend fun getHashtagTimeline(
        @Path("hashtag") hashtag: String,
        @Query("local") local: Boolean = false,
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
        @Field("language") language: String? = null,
        @Field("poll[options][]") pollOptions: List<String>? = null,
        @Field("poll[expires_in]") pollExpiresIn: Int? = null,
        @Field("poll[multiple]") pollMultiple: Boolean? = null,
        @Field("poll[hide_totals]") pollHideTotals: Boolean? = null,
        @Field("quoted_status_id") quotedStatusId: String? = null
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
     * Pin a status to profile.
     * Only works for own statuses. Max 5 pinned statuses typically.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/pin")
    suspend fun pinStatus(@Path("id") id: String): StatusDto

    /**
     * Unpin a status from profile.
     *
     * @param id Status ID
     */
    @POST("api/v1/statuses/{id}/unpin")
    suspend fun unpinStatus(@Path("id") id: String): StatusDto

    /**
     * Get conversation context for a status.
     * Returns ancestors (posts before) and descendants (replies after).
     *
     * @param id Status ID
     */
    @GET("api/v1/statuses/{id}/context")
    suspend fun getContext(
        @Path("id") id: String
    ): retrofit2.Response<com.pfitzer.tootsie.data.remote.dto.ContextDto>

    /**
     * Get the state of a server background job announced via the Mastodon-Async-Refresh
     * header (experimental API, Mastodon >= 4.4).
     */
    @GET("api/v1_alpha/async_refreshes/{id}")
    suspend fun getAsyncRefresh(
        @Path("id") id: String
    ): com.pfitzer.tootsie.data.remote.dto.AsyncRefreshResponseDto

    // ========== Bookmarks & Favorites Endpoints ==========

    /**
     * Get bookmarked statuses.
     * Returns statuses the authenticated user has bookmarked.
     *
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results (default 20, max 40)
     */
    @GET("api/v1/bookmarks")
    suspend fun getBookmarks(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20
    ): List<StatusDto>

    /**
     * Get favorited statuses.
     * Returns statuses the authenticated user has favorited.
     *
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results (default 20, max 40)
     */
    @GET("api/v1/favourites")
    suspend fun getFavorites(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20
    ): List<StatusDto>

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
        @Query("account_id") accountId: String? = null,
        @Header("Authorization") authorization: String? = null,
        @Header(HEADER_INSTANCE) instance: String? = null
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

    // ========== Search Endpoints ==========

    /**
     * Search for accounts, statuses, and hashtags.
     * Uses Mastodon API v2 for improved search results.
     *
     * @param query Search query string
     * @param type Type of results to return (accounts, statuses, hashtags) or null for all
     * @param resolve Attempt WebFinger lookup (for remote accounts)
     * @param following Only search among accounts the user follows
     * @param accountId Only search for statuses from this account ID
     * @param maxId Return results older than this ID
     * @param minId Return results newer than this ID
     * @param limit Maximum number of results per category (default 20, max 40)
     * @param offset Skip this many results (for pagination)
     */
    @GET("api/v2/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("resolve") resolve: Boolean = false,
        @Query("following") following: Boolean = false,
        @Query("account_id") accountId: String? = null,
        @Query("max_id") maxId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SearchResultsDto

    // ========== Trending Endpoints ==========

    /**
     * Get trending hashtags.
     * Returns tags that are being used more frequently within the past week.
     *
     * @param limit Maximum number of results to return (default 10, max 20)
     * @param offset Skip the first n results (for pagination)
     */
    @GET("api/v1/trends/tags")
    suspend fun getTrendingTags(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): List<TrendingTagDto>

    /**
     * Get trending statuses/posts.
     * Returns statuses that have been interacted with more than others.
     *
     * @param limit Maximum number of results to return (default 20, max 40)
     * @param offset Skip the first n results (for pagination)
     */
    @GET("api/v1/trends/statuses")
    suspend fun getTrendingStatuses(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): List<StatusDto>

    /**
     * Get trending links/articles.
     * Returns links that have been shared more than others.
     *
     * @param limit Maximum number of results to return (default 10, max 20)
     * @param offset Skip the first n results (for pagination)
     */
    @GET("api/v1/trends/links")
    suspend fun getTrendingLinks(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): List<TrendingLinkDto>

    // ========== Lists Endpoints ==========

    /**
     * Get all lists owned by the authenticated user.
     */
    @GET("api/v1/lists")
    suspend fun getLists(): List<ListDto>

    /**
     * Get a single list by ID.
     *
     * @param id List ID
     */
    @GET("api/v1/lists/{id}")
    suspend fun getList(
        @Path("id") id: String
    ): ListDto

    /**
     * Create a new list.
     *
     * @param title Title of the list
     * @param repliesPolicy Replies policy: "followed", "list", or "none"
     */
    @FormUrlEncoded
    @POST("api/v1/lists")
    suspend fun createList(
        @Field("title") title: String,
        @Field("replies_policy") repliesPolicy: String = "list"
    ): ListDto

    /**
     * Update a list.
     *
     * @param id List ID
     * @param title New title
     * @param repliesPolicy New replies policy
     */
    @FormUrlEncoded
    @POST("api/v1/lists/{id}")
    suspend fun updateList(
        @Path("id") id: String,
        @Field("title") title: String? = null,
        @Field("replies_policy") repliesPolicy: String? = null
    ): ListDto

    /**
     * Delete a list.
     *
     * @param id List ID
     */
    @DELETE("api/v1/lists/{id}")
    suspend fun deleteList(
        @Path("id") id: String
    )

    /**
     * Get accounts in a list.
     *
     * @param id List ID
     * @param limit Maximum number of accounts (default 40)
     */
    @GET("api/v1/lists/{id}/accounts")
    suspend fun getListAccounts(
        @Path("id") id: String,
        @Query("limit") limit: Int = 40
    ): List<AccountDto>

    /**
     * Add accounts to a list.
     *
     * @param id List ID
     * @param accountIds Array of account IDs to add
     */
    @FormUrlEncoded
    @POST("api/v1/lists/{id}/accounts")
    suspend fun addAccountsToList(
        @Path("id") id: String,
        @Field("account_ids[]") accountIds: List<String>
    )

    /**
     * Remove accounts from a list.
     *
     * @param id List ID
     * @param accountIds Array of account IDs to remove
     */
    // Query parameters instead of a form body: Retrofit rejects @FormUrlEncoded on DELETE,
    // and Mastodon reads account_ids[] from the query string as well
    @DELETE("api/v1/lists/{id}/accounts")
    suspend fun removeAccountsFromList(
        @Path("id") id: String,
        @Query("account_ids[]") accountIds: List<String>
    )

    /**
     * Get timeline for a specific list.
     *
     * @param id List ID
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     * @param limit Maximum number of results (default 20, max 40)
     */
    @GET("api/v1/timelines/list/{id}")
    suspend fun getListTimeline(
        @Path("id") id: String,
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20
    ): List<StatusDto>

    // ========== Followed Tags Endpoints ==========

    /**
     * Get all hashtags followed by the authenticated user.
     *
     * @param limit Maximum number of results (default 100, max 200)
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param minId Return results immediately newer than this ID
     */
    @GET("api/v1/followed_tags")
    suspend fun getFollowedTags(
        @Query("limit") limit: Int = 100,
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null
    ): retrofit2.Response<List<FollowedTagDto>>

    /**
     * Follow a hashtag.
     *
     * @param id Tag name (hashtag without #)
     */
    @POST("api/v1/tags/{id}/follow")
    suspend fun followTag(
        @Path("id") id: String
    ): FollowedTagDto

    /**
     * Unfollow a hashtag.
     *
     * @param id Tag name (hashtag without #)
     */
    @POST("api/v1/tags/{id}/unfollow")
    suspend fun unfollowTag(
        @Path("id") id: String
    ): FollowedTagDto

    // ========== Filters Endpoints (v2) ==========

    /**
     * Get all filters for the authenticated user.
     */
    @GET("api/v2/filters")
    suspend fun getFilters(): List<FilterDto>

    /**
     * Get a specific filter by ID.
     *
     * @param id Filter ID
     */
    @GET("api/v2/filters/{id}")
    suspend fun getFilter(
        @Path("id") id: String
    ): FilterDto

    /**
     * Create a new filter.
     *
     * @param title Title of the filter
     * @param context Contexts where filter applies (home, notifications, public, thread, account)
     * @param filterAction Action to take: "warn" or "hide"
     * @param expiresIn Expiration time in seconds, or null for never
     */
    @FormUrlEncoded
    @POST("api/v2/filters")
    suspend fun createFilter(
        @Field("title") title: String,
        @Field("context[]") context: List<String>,
        @Field("filter_action") filterAction: String,
        @Field("expires_in") expiresIn: Int? = null
    ): FilterDto

    /**
     * Update an existing filter.
     *
     * @param id Filter ID
     * @param title New title (optional)
     * @param context New contexts (optional)
     * @param filterAction New action (optional)
     * @param expiresIn New expiration in seconds (optional)
     */
    @FormUrlEncoded
    @PUT("api/v2/filters/{id}")
    suspend fun updateFilter(
        @Path("id") id: String,
        @Field("title") title: String? = null,
        @Field("context[]") context: List<String>? = null,
        @Field("filter_action") filterAction: String? = null,
        // An empty value removes the expiry
        @Field("expires_in") expiresIn: String? = null
    ): FilterDto

    /**
     * Delete a filter.
     *
     * @param id Filter ID
     */
    @DELETE("api/v2/filters/{id}")
    suspend fun deleteFilter(
        @Path("id") id: String
    )

    /**
     * Add a keyword to a filter.
     *
     * @param filterId Filter ID
     * @param keyword The keyword text to match
     * @param wholeWord Whether to match whole words only
     */
    @FormUrlEncoded
    @POST("api/v2/filters/{filter_id}/keywords")
    suspend fun addFilterKeyword(
        @Path("filter_id") filterId: String,
        @Field("keyword") keyword: String,
        @Field("whole_word") wholeWord: Boolean
    ): FilterKeywordDto

    /**
     * Delete a keyword from a filter.
     *
     * @param id Keyword ID
     */
    @DELETE("api/v2/filters/keywords/{id}")
    suspend fun deleteFilterKeyword(
        @Path("id") id: String
    )

    // ========== Push Subscription Endpoints ==========

    /**
     * Subscribe to push notifications.
     * Creates a new Web Push subscription for the authenticated user.
     *
     * @param request Push subscription request with endpoint and alert preferences
     */
    @POST("api/v1/push/subscription")
    suspend fun subscribeToPush(
        @Body request: com.pfitzer.tootsie.data.remote.dto.PushSubscriptionRequestDto,
        @Header("Authorization") authorization: String? = null,
        @Header(HEADER_INSTANCE) instance: String? = null
    ): com.pfitzer.tootsie.data.remote.dto.PushSubscriptionDto

    /**
     * Get current push subscription.
     *
     * @return Current push subscription or error if not subscribed
     */
    @GET("api/v1/push/subscription")
    suspend fun getPushSubscription(
        @Header("Authorization") authorization: String? = null,
        @Header(HEADER_INSTANCE) instance: String? = null
    ): com.pfitzer.tootsie.data.remote.dto.PushSubscriptionDto

    /**
     * Update push subscription alerts.
     *
     * @param request Updated push subscription request
     */
    @PUT("api/v1/push/subscription")
    suspend fun updatePushSubscription(
        @Body request: com.pfitzer.tootsie.data.remote.dto.PushSubscriptionRequestDto,
        @Header("Authorization") authorization: String? = null,
        @Header(HEADER_INSTANCE) instance: String? = null
    ): com.pfitzer.tootsie.data.remote.dto.PushSubscriptionDto

    /**
     * Delete push subscription.
     * Unsubscribes from push notifications.
     */
    @DELETE("api/v1/push/subscription")
    suspend fun deletePushSubscription(
        @Header("Authorization") authorization: String? = null,
        @Header(HEADER_INSTANCE) instance: String? = null
    )

    // ========== Custom Emoji Endpoints ==========

    /**
     * Get all custom emojis available on this instance.
     * Returns a list of custom emojis that can be used in statuses.
     */
    @GET("api/v1/polls/{id}")
    suspend fun getPoll(@Path("id") id: String): com.pfitzer.tootsie.data.remote.dto.PollDto

    /**
     * Vote on a poll.
     *
     * @param id Poll ID
     * @param choices List of option indices to vote for
     * @return Updated poll
     */
    @POST("api/v1/polls/{id}/votes")
    @FormUrlEncoded
    suspend fun voteOnPoll(
        @Path("id") id: String,
        @Field("choices[]") choices: List<Int>
    ): com.pfitzer.tootsie.data.remote.dto.PollDto

    @GET("api/v1/custom_emojis")
    suspend fun getCustomEmojis(): List<com.pfitzer.tootsie.data.remote.dto.CustomEmojiDto>

    // --- Conversations ---

    /**
     * Return all conversations the user has participated in.
     */
    @GET("api/v1/conversations")
    suspend fun getConversations(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("min_id") minId: String? = null,
        @Query("limit") limit: Int = 20
    ): List<com.pfitzer.tootsie.data.remote.dto.ConversationDto>

    /**
     * Remove a conversation from the list of conversations.
     */
    @DELETE("api/v1/conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") id: String
    )

    /**
     * Mark a conversation as read.
     */
    @POST("api/v1/conversations/{id}/read")
    suspend fun markConversationAsRead(
        @Path("id") id: String
    ): com.pfitzer.tootsie.data.remote.dto.ConversationDto

    // ========== Announcements Endpoints ==========

    /**
     * Get all active announcements from the server.
     *
     * @param withDismissed Include announcements that have been dismissed by the user
     */
    @GET("api/v1/announcements")
    suspend fun getAnnouncements(
        @Query("with_dismissed") withDismissed: Boolean = true
    ): List<AnnouncementDto>

    /**
     * Dismiss an announcement (mark as read).
     *
     * @param id Announcement ID
     */
    @POST("api/v1/announcements/{id}/dismiss")
    suspend fun dismissAnnouncement(
        @Path("id") id: String
    )

    // ========== Reports Endpoint ==========

    /**
     * Report an account (and optionally specific statuses) to instance moderators.
     *
     * @param accountId ID of the account to report
     * @param statusIds Optional list of status IDs to include in the report
     * @param comment Optional additional comment
     * @param forward Whether to forward to the remote instance
     * @param category Category: "spam", "violation", or "other"
     */
    @FormUrlEncoded
    @POST("api/v1/reports")
    suspend fun report(
        @Field("account_id") accountId: String,
        @Field("status_ids[]") statusIds: List<String>?,
        @Field("comment") comment: String?,
        @Field("forward") forward: Boolean,
        @Field("category") category: String
    ): ReportDto
}
