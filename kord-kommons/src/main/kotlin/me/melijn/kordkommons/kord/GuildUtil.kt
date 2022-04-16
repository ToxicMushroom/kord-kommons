package me.melijn.kordkommons.kord

import dev.kord.core.entity.Guild
import dev.kord.rest.Image

/**
 * @return png url or gif url based on icon hash (see https://discord.com/developers/docs/reference#image-formatting)))))
 */
fun Guild.iconUrl(size: Image.Size? = Image.Size.Size2048): String? {
    val hash = data.icon ?: return null
    val format = hashToImageFormat(hash)
    return getIconUrl(format) + size?.let { "?size=${size.maxRes}" }
}

/**
 * @return png url or gif url based on banner hash (see https://discord.com/developers/docs/reference#image-formatting)))))
 */
fun Guild.bannerUrl(size: Image.Size? = Image.Size.Size2048): String? {
    val hash = data.banner ?: return null
    val format = hashToImageFormat(hash)
    return getBannerUrl(format) + size?.let { "?size=${size.maxRes}" }
}

/**
 * @return png url or gif url based on icon hash (see https://discord.com/developers/docs/reference#image-formatting)))))
 */
fun Guild.splashUrl(size: Image.Size? = Image.Size.Size2048): String? {
    val hash = data.splash.value ?: return null
    val format = hashToImageFormat(hash)
    return getSplashUrl(format) + size?.let { "?size=${size.maxRes}" }
}

private fun hashToImageFormat(hash: String) = if (hash.startsWith("a_")) Image.Format.GIF else Image.Format.PNG