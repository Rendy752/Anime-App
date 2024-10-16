package com.example.animeapp.db

import androidx.room.TypeConverter
import com.example.animeapp.models.Aired
import com.example.animeapp.models.AnimeHeader
import com.example.animeapp.models.Broadcast
import com.example.animeapp.models.CommonIdentity
import com.example.animeapp.models.Images
import com.example.animeapp.models.NameAndUrl
import com.example.animeapp.models.Relation
import com.example.animeapp.models.Theme
import com.example.animeapp.models.Title
import com.example.animeapp.models.Trailer
import com.example.animeapp.models.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromAnimeHeaderList(value: List<AnimeHeader>): String {
        return value.joinToString(",") { it.mal_id.toString() }
    }

    @TypeConverter
    fun toAnimeHeaderList(value: String): List<AnimeHeader> {
        val malIds = value.split(",")
        return malIds.map { AnimeHeader(it.toInt(), "") }
    }

    @TypeConverter
    fun fromUser(value: User): String {
        return value.username
    }

    @TypeConverter
    fun toUser(value: String): User {
        return User(value, "", "")
    }

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
    fun fromRelationList(relations: List<Relation>): String {
        return Gson().toJson(relations)
    }

    @TypeConverter
    fun toRelationList(relationsJson: String): List<Relation> {
        val type = object : TypeToken<List<Relation>>() {}.type
        return Gson().fromJson(relationsJson, type)
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
    fun fromNameAndUrlList(nameAndUrl: List<NameAndUrl>): String {
        return Gson().toJson(nameAndUrl)
    }

    @TypeConverter
    fun toNameAndUrlList(nameAndUrlJson: String): List<NameAndUrl> {
        val type = object : TypeToken<List<NameAndUrl>>() {}.type
        return Gson().fromJson(nameAndUrlJson, type)
    }


}