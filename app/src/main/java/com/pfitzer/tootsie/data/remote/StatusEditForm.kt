package com.pfitzer.tootsie.data.remote

import com.pfitzer.tootsie.domain.model.NewStatus
import okhttp3.FormBody

/**
 * Form body for editing a status (PUT /api/v1/statuses/:id).
 *
 * Alt texts of attached media can only be changed here, as `media_attributes[]` hashes.
 * Rails builds each hash from consecutive `media_attributes[][…]` fields, so the fields of
 * one attachment must stay together; Retrofit's @Field lists would send all IDs first.
 *
 * @param descriptions Alt text per media ID; attachments without an entry keep theirs
 */
object StatusEditForm {

    fun build(status: NewStatus, descriptions: Map<String, String>): FormBody {
        val form = FormBody.Builder()
            .add("status", status.content)
            // Sent even when empty, so a removed content warning is removed
            .add("spoiler_text", status.spoilerText.orEmpty())
            .add("sensitive", status.sensitive.toString())
        status.language?.let { form.add("language", it) }

        val mediaIds = status.mediaIds.orEmpty()
        mediaIds.forEach { form.add("media_ids[]", it) }
        mediaIds.forEach { id ->
            descriptions[id]?.let { description ->
                form.add("media_attributes[][id]", id)
                form.add("media_attributes[][description]", description)
            }
        }

        status.pollConfig?.let { poll ->
            poll.options.forEach { form.add("poll[options][]", it) }
            form.add("poll[expires_in]", poll.expiresIn.toString())
            form.add("poll[multiple]", poll.multiple.toString())
            form.add("poll[hide_totals]", poll.hideTotals.toString())
        }
        return form.build()
    }
}
