package com.luminoverse.animevibe.data.local.entities

import androidx.room.TypeConverter
import com.luminoverse.animevibe.models.*
import kotlinx.serialization.json.Json

class AnimeDetailConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromImages(images: Images): String {
        return json.encodeToString(images)
    }

    @TypeConverter
    fun toImages(imagesJson: String): Images {
        return json.decodeFromString(imagesJson)
    }

    @TypeConverter
    fun fromTrailer(trailer: Trailer): String {
        return json.encodeToString(trailer)
    }

    @TypeConverter
    fun toTrailer(trailerJson: String): Trailer {
        return json.decodeFromString(trailerJson)
    }

    @TypeConverter
    fun fromTitlesList(titles: List<Title>): String {
        return json.encodeToString(titles)
    }

    @TypeConverter
    fun toTitlesList(titlesJson: String): List<Title> {
        return json.decodeFromString(titlesJson)
    }

    @TypeConverter
    fun fromStringList(strings: List<String>): String {
        return json.encodeToString(strings)
    }

    @TypeConverter
    fun toStringList(string: String): List<String> {
        return json.decodeFromString(string)
    }

    @TypeConverter
    fun fromAired(aired: Aired): String {
        return json.encodeToString(aired)
    }

    @TypeConverter
    fun toAired(airedJson: String): Aired {
        return json.decodeFromString(airedJson)
    }

    @TypeConverter
    fun fromBroadcast(broadcast: Broadcast): String {
        return json.encodeToString(broadcast)
    }

    @TypeConverter
    fun toBroadcast(broadcastJson: String): Broadcast {
        return json.decodeFromString(broadcastJson)
    }

    @TypeConverter
    fun fromNullableCommonIdentityList(commonIdentities: List<CommonIdentity>?): String? {
        return commonIdentities?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toNullableCommonIdentityList(commonIdentitiesJson: String?): List<CommonIdentity>? {
        return commonIdentitiesJson?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromNullableRelationList(relations: List<Relation>?): String? {
        return relations?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toNullableRelationList(relationsJson: String?): List<Relation>? {
        return relationsJson?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromTheme(theme: Theme?): String? {
        return theme?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toTheme(themeJson: String?): Theme? {
        return themeJson?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromNullableNameAndUrlList(nameAndUrls: List<NameAndUrl?>?): String? {
        return nameAndUrls?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toNameAndUrlList(nameAndUrlJson: String): List<NameAndUrl> {
        return json.decodeFromString(nameAndUrlJson)
    }
}