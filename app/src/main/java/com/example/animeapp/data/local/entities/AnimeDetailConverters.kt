package com.example.animeappkotlin.data.local.entities

import android.provider.MediaStore.Audio.Genres
import androidx.room.TypeConverter
import com.example.animeappkotlin.models.Aired
import com.example.animeappkotlin.models.Broadcast
import com.example.animeappkotlin.models.CommonIdentity
import com.example.animeappkotlin.models.Images
import com.example.animeappkotlin.models.NameAndUrl
import com.example.animeappkotlin.models.Relation
import com.example.animeappkotlin.models.Theme
import com.example.animeappkotlin.models.Title
import com.example.animeappkotlin.models.Trailer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AnimeDetailConverters {
    @TypeConverter
    fun fromImages(images: Images): String {
        return Gson().toJson(images)
    }

    @TypeConverter
    fun toImages(imagesJson: String): Images {
        return Gson().fromJson(imagesJson, Images::class.java)
    }

    @TypeConverter
    fun fromTrailer(trailer: Trailer): String {
        return Gson().toJson(trailer)
    }

    @TypeConverter
    fun toTrailer(trailerJson: String): Trailer {
        return Gson().fromJson(trailerJson, Trailer::class.java)
    }

    @TypeConverter
    fun fromTitlesList(titles: List<Title>): String {
        return Gson().toJson(titles)
    }

    @TypeConverter
    fun toTitlesList(titlesJson: String): List<Title> {
        val type = object : TypeToken<List<Title>>() {}.type
        return Gson().fromJson(titlesJson, type)
    }

    @TypeConverter
    fun fromStringArray(strings: Array<String>): String {
        return strings.joinToString(",")
    }

    @TypeConverter
    fun toStringArray(string: String): Array<String> {
        return string.split(",").toTypedArray()
    }

    @TypeConverter
    fun fromAired(aired: Aired): String {
        return Gson().toJson(aired)
    }

    @TypeConverter
    fun toAired(airedJson: String): Aired {
        return Gson().fromJson(airedJson, Aired::class.java)
    }

    @TypeConverter
    fun fromBroadcast(broadcast: Broadcast): String {
        return Gson().toJson(broadcast)
    }

    @TypeConverter
    fun toBroadcast(broadcastJson: String): Broadcast {
        return Gson().fromJson(broadcastJson, Broadcast::class.java)
    }

    @TypeConverter
    fun fromCommonIdentityList(commonIdentities: List<CommonIdentity>): String {
        return Gson().toJson(commonIdentities)
    }

    @TypeConverter
    fun toCommonIdentityList(commonIdentitiesJson: String): List<CommonIdentity> {
        val type = object : TypeToken<List<CommonIdentity>>() {}.type
        return Gson().fromJson(commonIdentitiesJson, type)
    }

    @TypeConverter
    fun fromNullableCommonIdentityList(commonIdentities: List<CommonIdentity>?): String {
        return Gson().toJson(commonIdentities)
    }

    @TypeConverter
    fun toNullableCommonIdentityList(commonIdentitiesJson: String?): List<CommonIdentity>? {
        return commonIdentitiesJson?.let {
            Gson().fromJson(it, object : TypeToken<List<CommonIdentity>>() {}.type)
        }
    }

    @TypeConverter
    fun fromNullableRelationList(relations: List<Relation>?): String {
        return Gson().toJson(relations)
    }

    @TypeConverter
    fun toNullableRelationList(relationsJson: String?): List<Relation>? {
        return relationsJson?.let {
            Gson().fromJson(it, object : TypeToken<List<Relation>>() {}.type)
        }
    }

    @TypeConverter
    fun fromTheme(theme: Theme): String {
        return Gson().toJson(theme)
    }

    @TypeConverter
    fun toTheme(themeJson: String): Theme {
        return Gson().fromJson(themeJson, Theme::class.java)
    }

    @TypeConverter
    fun fromNullableNameAndUrlList(nameAndUrls: List<NameAndUrl?>?): String? {
        return Gson().toJson(nameAndUrls)
    }

    @TypeConverter
    fun toNullableNameAndUrlList(nameAndUrlsJson: String?): List<NameAndUrl?>? {
        return nameAndUrlsJson?.let {
            Gson().fromJson(it, object : TypeToken<List<NameAndUrl?>?>() {}.type)
        }
    }

    @TypeConverter
    fun fromNameAndUrlList(nameAndUrl: List<NameAndUrl>): String {
        return Gson().toJson(nameAndUrl)
    }

    @TypeConverter
    fun toNameAndUrlList(nameAndUrlJson: String): List<NameAndUrl> {
        val type = object : TypeToken<List<NameAndUrl>>() {}.type
        return Gson().fromJson(nameAndUrlJson, type)
    }
}