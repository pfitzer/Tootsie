package com.pfitzer.tootsie.data.remote.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit

class ListAccountsRequestTest {

    private var captured: Request? = null

    private val api: MastodonApiService = Retrofit.Builder()
        .baseUrl("https://example.social/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    captured = chain.request()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                })
                .build()
        )
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MastodonApiService::class.java)

    @Test
    fun `removeAccountsFromList sends DELETE with account ids as query parameters`() = runTest {
        api.removeAccountsFromList("list-1", listOf("a1", "a2"))

        val request = captured!!
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/lists/list-1/accounts", request.url.encodedPath)
        assertEquals(listOf("a1", "a2"), request.url.queryParameterValues("account_ids[]"))
    }

    @Test
    fun `addAccountsToList still sends a form body`() = runTest {
        api.addAccountsToList("list-1", listOf("a1"))

        val request = captured!!
        assertEquals("POST", request.method)
        assertEquals("application/x-www-form-urlencoded", request.body?.contentType()?.let { "${it.type}/${it.subtype}" })
    }
}
