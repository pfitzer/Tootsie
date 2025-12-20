package com.pfitzer.tootsie.data.remote.api

import com.pfitzer.tootsie.data.remote.dto.AccountDto
import com.pfitzer.tootsie.data.remote.dto.InstanceDto
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
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

    // ========== Instance Endpoints ==========

    /**
     * Get instance information.
     * Does not require authentication.
     */
    @GET("api/v1/instance")
    suspend fun getInstance(): InstanceDto
}
