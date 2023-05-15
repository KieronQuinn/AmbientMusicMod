package com.kieronquinn.app.ambientmusicmod.utils.extensions

import java.security.SecureRandom
import java.util.Base64

fun randomSecureString(length: Int): String {
    val bytes = ByteArray(length)
    SecureRandom().apply {
        nextBytes(bytes)
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}