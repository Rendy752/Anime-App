package com.example.animeappkotlin.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.animeappkotlin.databinding.FragmentSettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}