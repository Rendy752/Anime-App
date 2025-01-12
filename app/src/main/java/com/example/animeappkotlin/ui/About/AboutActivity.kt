package com.example.animeappkotlin.ui.About

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.animeappkotlin.databinding.FragmentAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: FragmentAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}