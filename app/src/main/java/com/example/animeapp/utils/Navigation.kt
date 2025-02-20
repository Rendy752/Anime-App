package com.example.animeapp.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.animeapp.R

object Navigation {
    fun navigateToAnimeDetail(fragment: Fragment, animeId: Int, actionId: Int) {
        val bundle = Bundle().apply {
            putInt("id", animeId)
        }
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .setPopUpTo(R.id.animeDetailFragment, true)
            .build()

        fragment.findNavController().navigate(
            actionId,
            bundle,
            navOptions
        )
    }
}