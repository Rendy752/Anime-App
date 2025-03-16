# AnimeApp

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

AnimeApp is a comprehensive, anime-themed Android application designed to provide users with a seamless and immersive experience for discovering, exploring, and enjoying their favorite anime. Leveraging a robust architecture and efficient data management, this app aims to be the go-to resource for anime enthusiasts.

## Key Features

* **Extensive Anime Database:**
    * Access a vast and up-to-date catalog of anime titles, including detailed information such as genres, synopses, release dates, ratings, and character profiles.
    * Regularly updated data fetched from **Jikan API**, ensuring users have access to the latest releases and information.
* **Personalized Recommendations:**
    * Intelligent recommendation engine that suggests **two** similar anime based on user preferences and trending titles, with a **refresh button**.
     <div align="center"><img src="https://github.com/user-attachments/assets/653b6694-e6d2-4241-b1e0-90350f1d7d9a" width="300" alt="Recommendations Anime"></div>
* **Advanced Filtering and Search:**
    * Efficient search functionality for quickly finding specific anime titles.
      <div align="center"><img src="https://github.com/user-attachments/assets/843ba415-33ec-40af-bedc-cd76ea94c80d" width="300" alt="Search Anime"></div>
    * Powerful filtering options to refine searches by **keyword, limit, pagination, genre, producers, and more via a bottom sheet interface.**
      <div align="center"><img src="https://github.com/user-attachments/assets/fc92a5be-7037-434b-9bbb-c486a705eb42" width="300" alt="Genres Filter"></div>
      <div align="center"><img src="https://github.com/user-attachments/assets/8b515692-5487-4f93-8332-1236abca6301" width="300" alt="Producers Filter"></div>
      <div align="center"><img src="https://github.com/user-attachments/assets/2d8df11c-13b5-4a75-88c4-42ea0cf577fb" width="300" alt="Bottom Sheet Filter"></div>
* **Detailed Anime Information:**
    * Data stored in **Room Persistence Library**.
    * Comprehensive anime details pages, including high-resolution cover art, character descriptions, and episode lists.
      <div align="center"><img src="https://github.com/user-attachments/assets/6f567ff3-cc50-4e17-92ca-6293df02eb48" width="300" alt="Detail Anime"></div>
      <div align="center"><img src="https://github.com/user-attachments/assets/b8779ac8-bd68-4bd6-b00e-7b030f0a0dc9" width="300" alt="Episode List"></div>
* **Episode Streaming:**
    * Integrated episode streaming capabilities, allowing users to watch anime directly within the app, using **Aniwatch API**.
      <div align="center"><img src="https://github.com/user-attachments/assets/3dd5fcb2-802b-4dec-83ec-f559d047e1c0" width="300" alt="Episode Streaming"></div>
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
