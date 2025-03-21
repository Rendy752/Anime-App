package com.example.animeapp.utils

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.animeapp.R
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.CommonIdentity
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.google.gson.Gson

object Navigation {
    fun navigateToAnimeWatch(
        fragment: Fragment,
        actionId: Int,
        animeDetail: AnimeDetail,
        episodeId: String,
        episodes: List<Episode>,
        defaultEpisode: EpisodeDetailComplement
    ) {
        val bundle = Bundle().apply {
            putParcelable("animeDetail", animeDetail)
            putString("episodeId", episodeId)
            putParcelableArrayList("episodes", ArrayList(episodes))
            putParcelable("defaultEpisode", defaultEpisode)
        }

        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_left)
            .setExitAnim(R.anim.slide_out_right)
            .setPopEnterAnim(R.anim.slide_in_right)
            .setPopExitAnim(R.anim.slide_out_left)
            .setPopUpTo(R.id.animeDetailFragment, false)
            .build()

        fragment.findNavController().navigate(
            actionId,
            bundle,
            navOptions
        )
    }

    fun navigateWithFilter(
        navController: NavController,
        data: CommonIdentity?,
        genreFilter: Boolean = false
    ) {
        val gson = Gson()
        val commonIdentityString = data?.let { Uri.encode(gson.toJson(it)) }
        val genrePart = if (genreFilter) commonIdentityString ?: "null" else "null"
        val producerPart = if (!genreFilter) commonIdentityString ?: "null" else "null"

        navController.navigate("search/${genrePart}/${producerPart}")
    }
}