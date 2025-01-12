import com.example.animeappkotlin.utils.Limit

data class AnimeSearchQueryState(
    val query: String = "",
    val page: Int = 1,
    val limit: Int? = Limit.DEFAULT_LIMIT
)