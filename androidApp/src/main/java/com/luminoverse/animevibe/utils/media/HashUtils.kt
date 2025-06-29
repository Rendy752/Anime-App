package com.luminoverse.animevibe.utils.media

import java.security.MessageDigest

fun String.toSha256(): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}