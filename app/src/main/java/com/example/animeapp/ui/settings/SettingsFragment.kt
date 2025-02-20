package com.example.animeapp.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.animeapp.databinding.FragmentSettingsBinding
import com.example.animeapp.utils.Theme

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTheme()
    }

    private fun setupTheme() {
        val themePrefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkMode = themePrefs.getBoolean("is_dark_mode", false)
        binding.apply {
            switchDarkMode.isChecked = isDarkMode
            val activity = requireActivity() as AppCompatActivity
            Theme.setTheme(activity, isDarkMode)
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                Theme.setTheme(activity, isChecked)
                themePrefs.edit().putBoolean("is_dark_mode", isChecked).apply()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}