package me.melijn.kordkommons.kord

import dev.kord.core.entity.User
import dev.kord.rest.Image

/**
 * @return the user avatar url or the default discord avatar url as png or gif
 */
fun User.effectiveAvatarUrl(size: Image.Size): String {
    return avatar?.cdnUrl?.toUrl {
        this.format = if (avatar?.animated == true) Image.Format.GIF else Image.Format.PNG
        this.size = size
    } ?: defaultAvatar.cdnUrl.toUrl {
        this.format = Image.Format.PNG
        this.size = size
    }
}