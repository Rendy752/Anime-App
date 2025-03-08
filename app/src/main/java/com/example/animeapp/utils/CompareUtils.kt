package com.example.animeapp.utils

object CompareUtils {
    inline fun <reified T : Any> areDataClassesEqual(obj1: T?, obj2: T?): Boolean {
        if (obj1 == null && obj2 == null) {
            return true
        }
        if (obj1 == null || obj2 == null) {
            return false
        }

        if (obj1::class != T::class || obj2::class != T::class) {
            return false
        }

        return obj1 == obj2
    }
}