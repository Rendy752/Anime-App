package com.example.animeapp.models

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "anime_detail",
    primaryKeys = ["mal_id"]
)

@Parcelize
@Serializable
data class AnimeDetail(
    val mal_id: Int,
    val url: String,
    val images: Images,
    val trailer: Trailer,
    val approved: Boolean,
    val titles: List<Title>,
    val title: String,
    val title_english: String?,
    val title_japanese: String?,
    val title_synonyms: List<String>?,
    val type: String?,
    val source: String,
    val episodes: Int,
    val status: String,
    val airing: Boolean,
    val aired: Aired,
    val duration: String,
    val rating: String?,
    val score: Double?,
    val scored_by: Int?,
    val rank: Int?,
    val popularity: Int,
    val members: Int,
    val favorites: Int,
    val synopsis: String?,
    val background: String?,
    val season: String?,
    val year: Int?,
    val broadcast: Broadcast,
    val producers: List<CommonIdentity>?,
    val licensors: List<CommonIdentity>?,
    val studios: List<CommonIdentity>?,
    val genres: List<CommonIdentity>?,
    val explicit_genres: List<CommonIdentity>?,
    val themes: List<CommonIdentity>?,
    val demographics: List<CommonIdentity>?,
    val relations: List<Relation>?,
    val theme: Theme?,
    val external: List<NameAndUrl>?,
    val streaming: List<NameAndUrl>?
) : Parcelable

val animeDetailPlaceholder = AnimeDetail(
    mal_id = 1735,
    url = "https://myanimelist.net/anime/1735/Naruto__Shippuuden",
    images = Images(
        jpg = ImageUrl(
            image_url = "https://cdn.myanimelist.net/images/anime/1565/111305.jpg",
            small_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305t.jpg",
            medium_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305m.jpg",
            large_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305l.jpg",
            maximum_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305.jpg"
        ),
        webp = ImageUrl(
            image_url = "https://cdn.myanimelist.net/images/anime/1565/111305.webp",
            small_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305t.webp",
            medium_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305m.webp",
            large_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305l.webp",
            maximum_image_url = "https://cdn.myanimelist.net/images/anime/1565/111305.webp"
        )
    ),
    trailer = Trailer(
        youtube_id = "1dy2zPPrKD0",
        url = "https://www.youtube.com/watch?v=1dy2zPPrKD0",
        embed_url = "https://www.youtube.com/embed/1dy2zPPrKD0?enablejsapi=1&wmode=opaque&autoplay=1",
        images = ImageUrl(
            image_url = "https://img.youtube.com/vi/1dy2zPPrKD0/default.jpg",
            small_image_url = "https://img.youtube.com/vi/1dy2zPPrKD0/sddefault.jpg",
            medium_image_url = "https://img.youtube.com/vi/1dy2zPPrKD0/mqdefault.jpg",
            large_image_url = "https://img.youtube.com/vi/1dy2zPPrKD0/hqdefault.jpg",
            maximum_image_url = "https://img.youtube.com/vi/1dy2zPPrKD0/maxresdefault.jpg"
        )
    ),
    approved = true,
    titles = listOf(
        Title("Default", "Naruto: Shippuuden"),
        Title("Synonym", "Naruto Hurricane Chronicles"),
        Title("Japanese", "-ナルト- 疾風伝"),
        Title("English", "Naruto Shippuden"),
        Title("German", "Naruto Shippuden"),
        Title("Spanish", "Naruto Shippuden"),
        Title("French", "Naruto Shippuden")
    ),
    title = "Naruto: Shippuuden",
    title_english = "Naruto Shippuden",
    title_japanese = "-ナルト- 疾風伝",
    title_synonyms = listOf("Naruto Hurricane Chronicles"),
    type = "TV",
    source = "Manga",
    episodes = 500,
    status = "Finished Airing",
    airing = false,
    aired = Aired(
        from = "2007-02-15T00:00:00+00:00",
        to = "2017-03-23T00:00:00+00:00",
        prop = Prop(DateObject(15, 2, 2007), DateObject(23, 3, 2017)),
        string = "Feb 15, 2007 to Mar 23, 2017"
    ),
    duration = "23 min per ep",
    rating = "PG-13 - Teens 13 or older",
    score = 8.28,
    scored_by = 1730268,
    rank = 303,
    popularity = 16,
    members = 2600581,
    favorites = 115704,
    synopsis = "It has been two and a half years since Naruto Uzumaki left Konohagakure, the Hidden Leaf Village, for intense training following events which fueled his desire to be stronger. Now Akatsuki, the mysterious organization of elite rogue ninja, is closing in on their grand plan which may threaten the safety of the entire shinobi world.\n \nAlthough Naruto is older and sinister events loom on the horizon, he has changed little in personality—still rambunctious and childish—though he is now far more confident and possesses an even greater determination to protect his friends and home. Come whatever may, Naruto will carry on with the fight for what is important to him, even at the expense of his own body, in the continuation of the saga about the boy who wishes to become Hokage.\n\n[Written by MAL Rewrite]",
    background = "",
    season = "winter",
    year = 2007,
    broadcast = Broadcast(
        day = "Thursdays",
        time = "19:30",
        timezone = "Asia/Tokyo",
        string = "Thursdays at 19:30 (JST)"
    ),
    producers = listOf(
        CommonIdentity(
            mal_id = 16,
            type = "anime",
            name = "TV Tokyo",
            url = "https://myanimelist.net/anime/producer/16/TV_Tokyo"
        ),
        CommonIdentity(
            mal_id = 17,
            type = "anime",
            name = "Aniplex",
            url = "https://myanimelist.net/anime/producer/17/Aniplex"
        ),
        CommonIdentity(
            mal_id = 50,
            type = "anime",
            name = "KSS",
            url = "https://myanimelist.net/anime/producer/50/KSS"
        ),
        CommonIdentity(
            mal_id = 211,
            type = "anime",
            name = "Rakuonsha",
            url = "https://myanimelist.net/anime/producer/211/Rakuonsha"
        ),
        CommonIdentity(
            mal_id = 717,
            type = "anime",
            name = "TV Tokyo Music",
            url = "https://myanimelist.net/anime/producer/717/TV_Tokyo_Music"
        ),
        CommonIdentity(
            mal_id = 1365,
            type = "anime",
            name = "Shueisha",
            url = "https://myanimelist.net/anime/producer/1365/Shueisha"
        ),
    ),
    licensors = listOf(
        CommonIdentity(
            mal_id = 119,
            type = "anime",
            name = "VIZ Media",
            url = "https://myanimelist.net/anime/producer/119/VIZ_Media"
        )
    ),
    studios = listOf(
        CommonIdentity(
            mal_id = 1,
            type = "anime",
            name = "Pierrot",
            url = "https://myanimelist.net/anime/producer/1/Pierrot"
        )
    ),
    genres = listOf(
        CommonIdentity(
            mal_id = 1,
            type = "anime",
            name = "Action",
            url = "https://myanimelist.net/anime/genre/1/Action"
        ),
        CommonIdentity(
            mal_id = 2,
            type = "anime",
            name = "Adventure",
            url = "https://myanimelist.net/anime/genre/2/Adventure"
        ),
        CommonIdentity(
            mal_id = 10,
            type = "anime",
            name = "Fantasy",
            url = "https://myanimelist.net/anime/genre/10/Fantasy"
        )
    ),
    explicit_genres = listOf(),
    themes = listOf(
        CommonIdentity(
            mal_id = 17,
            type = "anime",
            name = "Martial Arts",
            url = "https://myanimelist.net/anime/genre/17/Martial_Arts"
        )
    ),
    demographics = listOf(
        CommonIdentity(
            mal_id = 27,
            type = "anime",
            name = "Shounen",
            url = "https://myanimelist.net/anime/genre/27/Shounen"
        )
    ),
    relations = listOf(
        Relation(
            relation = "Prequel",
            entry = listOf(
                CommonIdentity(
                    mal_id = 20,
                    type = "anime",
                    name = "Naruto",
                    url = "https://myanimelist.net/anime/20/Naruto"
                )
            )
        ),
        Relation(
            relation = "Sequel",
            entry = listOf(
                CommonIdentity(
                    mal_id = 28755,
                    type = "anime",
                    name = "Boruto: Naruto the Movie",
                    url = "https://myanimelist.net/anime/28755/Boruto__Naruto_the_Movie"
                ),
                CommonIdentity(
                    mal_id = 34566,
                    type = "anime",
                    name = "Boruto: Naruto Next Generations",
                    url = "https://myanimelist.net/anime/34566/Boruto__Naruto_Next_Generations"
                )
            )
        ),
        Relation(
            relation = "Adaptation",
            entry = listOf(
                CommonIdentity(
                    mal_id = 11,
                    type = "manga",
                    name = "Naruto",
                    url = "https://myanimelist.net/manga/11/Naruto"
                ),
                CommonIdentity(
                    mal_id = 86129,
                    type = "manga",
                    name = "Naruto Hiden Series",
                    url = "https://myanimelist.net/manga/86129/Naruto_Hiden_Series"
                ),
                CommonIdentity(
                    mal_id = 96200,
                    type = "manga",
                    name = "Naruto: Fuu no Sho - Sugao no Shinjitsu...!!",
                    url = "https://myanimelist.net/manga/96200/Naruto__Fuu_no_Sho_-_Sugao_no_Shinjitsu"
                ),
                CommonIdentity(
                    mal_id = 90531,
                    type = "manga",
                    name = "Naruto Shinden Series",
                    url = "https://myanimelist.net/manga/90531/Naruto_Shinden_Series"
                )
            )
        ),
        Relation(
            relation = "Side Story",
            entry = listOf(
                CommonIdentity(
                    mal_id = 2472,
                    type = "anime",
                    name = "Naruto: Shippuuden Movie 1",
                    url = "https://myanimelist.net/anime/2472/Naruto__Shippuuden_Movie_1"
                ),
                CommonIdentity(
                    mal_id = 4437,
                    type = "anime",
                    name = "Naruto: Shippuuden Movie 2 - Kizuna",
                    url = "https://myanimelist.net/anime/4437/Naruto__Shippuuden_Movie_2_-_Kizuna"
                ),
                CommonIdentity(
                    mal_id = 6325,
                    type = "anime",
                    name = "Naruto: Shippuuden Movie 3 - Hi no Ishi wo Tsugu Mono",
                    url = "https://myanimelist.net/anime/6325/Naruto__Shippuuden_Movie_3_-_Hi_no_Ishi_wo_Tsugu_Mono"
                ),
                CommonIdentity(
                    mal_id = 8246,
                    type = "anime",
                    name = "Naruto: Shippuuden Movie 4 - The Lost Tower",
                    url = "https://myanimelist.net/anime/8246/Naruto__Shippuuden_Movie_4_-_The_Lost_Tower"
                ),
                CommonIdentity(
                    mal_id = 10686,
                    type = "anime",
                    name = "Naruto: Honoo no Chuunin Shiken! Naruto vs. Konohamaru!!",
                    url = "https://myanimelist.net/anime/10686/Naruto__Honoo_no_Chuunin_Shiken_Naruto_vs_Konohamaru"
                ),
                CommonIdentity(
                    mal_id = 10589,
                    type = "anime",
                    name = "Naruto: Shippuuden Movie 5 - Blood Prison",
                    url = "https://myanimelist.net/anime/10589/Naruto__Shippuuden_Movie_5_-_Blood_Prison"
                ),
                CommonIdentity(
                    mal_id = 13667,
                    type = "anime",
                    name = "Naruto: Shippuuden Movie 6 - Road to Ninja",
                    url = "https://myanimelist.net/anime/13667/Naruto__Shippuuden_Movie_6_-_Road_to_Ninja"
                )
            )
        )
    ),
    theme = Theme(
        openings = listOf(
            "1: \"Hero's Come Back\" by Nobodyknows+ (eps 1-30)",
            "2: \"distance\" by LONG SHOT PARTY (eps 31-53)",
            "3: \"Blue Bird (ブルーバード)\" by Ikimonogakari (eps 54-77)",
            "4: \"CLOSER\" by Inoue Joe (eps 78-102)",
            "5: \"Hotaru no Hikari (ホタルノヒカリ)\" by Ikimonogakari (eps 103-128)",
            "6: \"Sign\" by FLOW (eps 129-153)",
            "7: \"Toumei Datta Sekai (透明だった世界)\" by Motohiro Hata (eps 152-179)",
            "8: \"Diver\" by NICO Touches the Walls (eps 180-205)",
            "9: \"Lovers (ラヴァーズ)\" by 7!! (eps 206-230)",
            "10: \"newsong\" by tacica (eps 231-256)",
            "11: \"Totsugeki Rock (突撃ロック)\" by THE CRO-MAGNONS (eps 257-281)",
            "12: \"Moshimo\" by Daisuke (eps 282-306)",
            "13: \"Niwaka Ame Nimo Makezu (ニワカ雨ニモ負ケズ)\" by NICO Touches the Walls (eps 307-332)",
            "14: \"Tsuki no Ookisa (月の大きさ)\" by Nogizaka46 (eps 333-356)",
            "15: \"Guren\" by DOES (eps 357-379)",
            "16: \"Silhouette (シルエット)\" by KANA-BOON (eps 380-405)",
            "17: \"Kaze (風)\" by Yamazaru (eps 406-431)",
            "18: \"LINE\" by Sukima Switch (eps 432-458)",
            "19: \"Blood Circulator (ブラッドサーキュレーター)\" by Asian Kung-Fu Generation (eps 459-479)",
            "20: \"Kara no Kokoro (カラノココロ)\" by Anly (eps 480-500)"
        ),
        endings = listOf(
            "1: \"Nagare Boshi ~Shooting Star~ (流れ星〜Shooting Star〜)\" by HOME MADE Kazoku (eps 1-18)",
            "2: \"Michi ~to you all (道 〜to you all)\" by aluto (eps 19-30)",
            "3: \"KIMI MONOGATARI (キミモノガタリ)\" by little by little (eps 31-41)",
            "4: \"Mezamero! Yasei (目覚めろ!野性)\" by MATCHY with QUESTION? (eps 42-53)",
            "5: \"Sunao na Niji (素直な虹)\" by surface (eps 54-63)",
            "6: \"BROKEN YOUTH\" by NICO Touches the Walls (eps 64-77)",
            "7: \"Long Kiss Goodbye\" by HALCALI (eps 78-90)",
            "8: \"BACCHIKOI!!! (バッチコイ!!!)\" by DEV PARADE (eps 91-102)",
            "9: \"Shinkokyuu (深呼吸)\" by SUPER BEAVER (eps 103-115)",
            "10: \"My ANSWER\" by SEAMO (eps 116-128)",
            "11: \"Omae Dattanda (おまえだったんだ)\" by Kishidan (eps 129-141)",
            "12: \"For You\" by AZU (eps 142-153)",
            "13: \"Jitensha (自転車)\" by OreSkaBand (eps 154-166)",
            "14: \"Utakata Hanabi (うたかた花火)\" by supercell (eps 167-179)",
            "15: \"U can do it!\" by DOMINO (eps 180-192)",
            "16: \"Mayonaka no Orchestra (真夜中のオーケストラ)\" by Aqua Timez (eps 193-205)",
            "17: \"FREEDOM\" by HOME MADE Kazoku (eps 206-218)",
            "18: \"Yokubou o Sakebe!!!! (欲望を叫べ!!!!)\" by OKAMOTO'S (eps 219-230)",
            "19: \"Place to Try\" by TOTALFAT (eps 231-242)",
            "20: \"By My Side (バイマイサイド)\" by Hemenway (eps 243-256)",
            "21: \"Cascade (カスケード)\" by UNLIMITS (eps 257-268)",
            "22: \"Kono Koe Karashite (この声枯らしてfeat. CHEHON)\" by AISHA feat. CHEHON (eps 267-281)",
            "23: \"MOTHER\" by MUCC (eps 282-295)",
            "24: \"Sayonara Memory (さよならメモリー)\" by 7!! (eps 296-306)",
            "25: \"I Can Hear\" by DISH// (eps 307-319)",
            "26: \"Yume wo Daite ~Hajimari no Crissroad~ (夢を抱いて〜はじまりのクリスロード〜)\" by Rake (eps 320-332)",
            "27: \"Black Night Town (ブラックナイトタウン)\" by Akihisa Kondou (eps 333-343)",
            "28: \"Niji (虹)\" by Shinkuu Horou (真空ホロウ) (eps 344-356)",
            "29: \"FLAME\" by DISH// (eps 357-366)",
            "30: \"Never Change feat.Lyu:Lyu\" by SHUN (eps 367-379)",
            "31: \"Dame Dame da (だめだめだ)\" by Shiori Tomita (eps 380-393)",
            "32: \"Spinning World\" by Diana Garnet (eps 394-405)",
            "33: \"Kotoba no Iranai Yakusoku (言葉のいらない約束)\" by sana (eps 406-417)",
            "34: \"Niji no Sora (虹の空)\" by FLOW (eps 418-431)",
            "35: \"Trouble Maker (トラブルメイカー)\" by KANIKAPILA (eps 432-443)",
            "36: \"Sonna Kimi, Konna Boku (そんな君、こんな僕)\" by Thinking Dogs (eps 444-454)",
            "37: \"Ao no Lullaby (青のララバイ)\" by Kuroneko Chelsea (eps 455-466)",
            "38: \"Pino to Amélie (ピノとアメリ)\" by Huwie Ishizaki (eps 467-479)",
            "39: \"Tabidachi no Uta (旅立ちの唄)\" by Ayumikurikamaki (eps 480-488)",
            "40: \"Zetsu Zetsu (『絶絶（ぜつぜつ）)\" by Swimy (eps 489-500)"
        )
    ),
    external = listOf(
        NameAndUrl(
            "Official Site",
            "http://www.tv-tokyo.co.jp/anime/naruto/"
        ),
        NameAndUrl(
            "AniDB",
            "https://anidb.net/perl-bin/animedb.pl?show=anime&aid=4880"
        ),
        NameAndUrl(
            "ANN",
            "https://www.animenewsnetwork.com/encyclopedia/anime.php?id=7293"
        ),
        NameAndUrl(
            "Wikipedia",
            "https://en.wikipedia.org/wiki/Naruto#Part_II_2"
        ),
        NameAndUrl(
            "Wikipedia",
            "https://ja.wikipedia.org/wiki/NARUTO_-%E3%83%8A%E3%83%AB%E3%83%88-_%E7%96%BE%E9%A2%A8%E4%BC%9D"
        ),
        NameAndUrl(
            "Syoboi",
            "https://cal.syoboi.jp/tid/1106"
        ),
        NameAndUrl("Bangumi", "https://bangumi.tv/subject/2782")
    ),
    streaming = listOf(
        NameAndUrl("Crunchyroll", "http://www.crunchyroll.com/series-280696"),
        NameAndUrl(
            "Netflix",
            "https://www.netflix.com/"
        )
    )
)


