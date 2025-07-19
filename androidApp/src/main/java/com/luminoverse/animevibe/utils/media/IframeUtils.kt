package com.luminoverse.animevibe.utils.media

import java.net.URLEncoder

object IframeUtils {
    private const val BASE_URL = "https://megaplay.buzz/stream/s-2/"

    /**
     * Constructs the fallback iframe player URL from a full episode ID and language.
     *
     * @param fullEpisodeId The episode ID from your app, e.g., "my-hero-academia-vigilantes-19544?ep=136197".
     * @param language The stream language, either "sub" or "dub".
     * @return The fully constructed URL for the iframe, or null if the ID is invalid.
     */
    fun buildFallbackUrl(fullEpisodeId: String?, language: String?): String? {
        if (fullEpisodeId == null || language == null) return null

        // Extract the numerical ID after "?ep="
        val hianimeEpId = fullEpisodeId.split("?ep=").getOrNull(1)

        return if (hianimeEpId != null) {
            // URL encode to be safe, although IDs are typically just numbers
            val encodedId = URLEncoder.encode(hianimeEpId, "UTF-8")
            "$BASE_URL$encodedId/$language"
        } else {
            null
        }
    }
}