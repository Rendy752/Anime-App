# AnimeApp

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

AnimeApp is a comprehensive, anime-themed Android application designed to provide users with a seamless and immersive experience for discovering, exploring, and enjoying their favorite anime. Leveraging a robust architecture and efficient data management, this app aims to be the go-to resource for anime enthusiasts.

## Key Features

* **Extensive Anime Database:**
    * Access a vast and up-to-date catalog of anime titles, including detailed information such as genres, synopses, release dates, ratings, and character profiles.
    * Regularly updated data fetched from **Jikan API**, ensuring users have access to the latest releases and information.
* **Personalized Recommendations:**
    * Intelligent recommendation engine that suggests **two** similar anime based on user preferences and trending titles, with a **refresh button**.
* **Advanced Filtering and Search:**
    * Powerful filtering options to refine searches by **keyword, limit, pagination, genre, producers, and more via a bottom sheet interface.**
    * Efficient search functionality for quickly finding specific anime titles.
* **Detailed Anime Information:**
    * Comprehensive anime details pages, including high-resolution cover art, character descriptions, and episode lists.
    * Data stored in **Room Persistence Library**.
* **Episode Streaming:**
    * Integrated episode streaming capabilities, allowing users to watch anime directly within the app, using **Aniwatch API**.
    * Store episode streaming data, server and source in **Room Persistence Library**.
    * Support **Picture in Picture (PiP)** mode.
* **Offline Data Management:**
    * Implementation of **Room Persistence Library** for local data storage, significantly reducing API requests and improving app performance.
    * Caching of frequently accessed anime data, enabling offline browsing and faster loading times.
    * User saved lists, and watch histories stored locally.
* **User-Friendly Interface:**
    * Intuitive and visually appealing interface, designed for optimal user experience on Android devices.
    * Clean and organized layout for easy navigation and content discovery.
    * **Dark mode support.**
    * **Shimmer Loading** for placeholder UI.

## Technical Specifications

* **Platform:** Android
* **MinSDK:** 27
* **TargetSDK:** 35
* **Programming Languages:** Kotlin
* **Architecture:** MVVM (Model-View-ViewModel)
* **Data Persistence:** Room Persistence Library
* **Serialization:** for efficient data handling.
* **API Integration:** Utilizing **Jikan API** for anime information and **Aniwatch API** for episode streaming.
* **Asynchronous Operations:** Coroutines for efficient background tasks and API calls.
* **Dependency Injection:** Hilt for dependency management.
* **Image Loading:** Glide for efficient image loading and caching.
* **Streaming:** ExoPlayer for video playback.
* **Network inspection:** Chucker Interceptor.
* **Retrofit2:** for api interaction.
