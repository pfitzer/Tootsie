package com.pfitzer.tootsie.domain.model

/**
 * Translation of a status by the server's translation service.
 *
 * @param pollOptions Translated poll option titles, in the order of the poll
 * @param mediaDescriptions Translated alt texts by media ID
 * @param detectedSourceLanguage Language the status was translated from
 * @param provider Translation service, e.g. "DeepL.com"
 */
data class Translation(
    val content: String,
    val spoilerText: String,
    val pollOptions: List<String> = emptyList(),
    val mediaDescriptions: Map<String, String> = emptyMap(),
    val detectedSourceLanguage: String?,
    val provider: String?
)

/**
 * This status with its translated text, content warning, poll options and alt texts, for
 * display; everything else (IDs, counts, interactions) stays as it is.
 */
fun Status.withTranslation(translation: Translation): Status = copy(
    content = translation.content,
    spoilerText = translation.spoilerText.ifEmpty { spoilerText },
    poll = poll?.let { poll ->
        poll.copy(
            options = poll.options.mapIndexed { index, option ->
                translation.pollOptions.getOrNull(index)?.let { option.copy(title = it) } ?: option
            }
        )
    },
    mediaAttachments = mediaAttachments.map { media ->
        translation.mediaDescriptions[media.id]?.let { media.copy(description = it) } ?: media
    }
)
