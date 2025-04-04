# AnimeApp

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

AnimeApp is a comprehensive, anime-themed Android application using Kotlin and Jetpack Compose designed to provide users with a seamless and immersive experience for discovering, exploring, and enjoying their favorite anime. Leveraging a robust architecture and efficient data management, this app aims to be the go-to resource for anime enthusiasts.

## Key Features

* **Extensive Anime Database:**
    * Access a vast and up-to-date catalog of anime titles, including detailed information such as genres, synopsis, release dates, ratings, and character profiles.
    * Regularly updated data fetched from **Jikan API**, ensuring users have access to the latest releases and information.
* **Personalized Recommendations:**
    * Intelligent recommendation engine that suggests **two** similar anime based on user preferences and trending titles, with a **refresh button**.
      <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/a3be233e-6aa7-42f9-9695-7a2c5055ee7c" width="300" alt="Recommendations Anime Skeleton Loading">
          </td>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/38629370-f6f5-4dce-bd43-0fdbae9640b8" width="300" alt="Recommendations Anime Portrait">
          </td>
           <td align="center">
            <img src="https://github.com/user-attachments/assets/6691f185-ad85-4276-8054-81222a56ac80" width="700" alt="Recommendations Anime Landscape">
          </td>
        </tr>
      </table>
* **Advanced Filtering and Search:**
    * Efficient search functionality for quickly finding specific anime titles.
      <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/25f464cf-1468-428e-8806-e043af125a00" width="300" alt="Search Anime Skeleton Loading">
          </td>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/8d3087bb-3875-4a0d-b61a-679605f62f71" width="300" alt="Search Anime Portrait">
          </td>
           <td align="center">
            <img src="https://github.com/user-attachments/assets/d4b1a600-5366-41c2-8788-7df6d6bcb360" width="700" alt="Search Anime Landscape">
          </td>
        </tr>
      </table>
    * Powerful filtering options to refine searches by **keyword, limit, pagination, genre, producers, and more via a bottom sheet interface.**
      <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/3cc31305-610d-4f5c-9ccd-b3fef8c49c53" width="300" alt="Anime Filter Bottom Sheet">
          </td>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/7175479a-674b-4ee6-9ba3-b37fc731b157" width="300" alt="Genre Filter Bottom Sheet">
          </td>
           <td align="center">
            <img src="https://github.com/user-attachments/assets/a2492278-e3e6-457d-aa80-b1b074713fb0" width="300" alt="Producer Filter Bottom Sheet">
          </td>
        </tr>
      </table>
* **Detailed Anime Information:**
    * Data stored in **Room Persistence Library**.
    * Comprehensive anime details pages, including high-resolution cover art, character descriptions, and episode lists with search filter.
      <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/46409b76-bdd2-4917-8e2e-13cdde12ea5e" width="300" alt="Detail Anime Skeleton Loading">
          </td>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/bf8e664d-aba9-4b2d-8b5b-8d06fb5d4fcd" width="300" alt="Detail Anime First Half">
          </td>
           <td align="center">
            <img src="https://github.com/user-attachments/assets/bdc59915-7b41-4a76-8216-90a6b4fd9ab4" width="300" alt="Detail Anime Second Half">
          </td>
           <td align="center">
            <img src="https://github.com/user-attachments/assets/b47c9e8e-d827-4b78-9fa9-f53f1baaa716" width="700" alt="Detail Anime Landscape">
          </td>
        </tr>
      </table>
* **Episode Streaming:**
    * Integrated episode streaming capabilities, allowing users to watch anime directly within the app, using **Aniwatch API**.
      <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/a495fc53-d7b1-4a4d-b6ab-19f852c5980b" width="300" alt="Streaming Anime Skeleton Loading">
          </td>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/3966c51e-dbe0-4884-92ee-b39bd4761831" width="300" alt="Streaming Anime Portrait">
          </td>
           <td align="center">
            <img src="https://github.com/user-attachments/assets/6166cbce-370e-458f-a254-3bbdfb7f23c5" width="700" alt="Streaming Anime Landscape">
          </td>
        </tr>
      </table>
    * Store episode streaming data, server and source in **Room Persistence Library**.
    * Support **Picture in Picture (PiP)** mode.
      <table>
        <tr>
           <td align="center">
            <img src="https://github.com/user-attachments/assets/8d65fc55-6f77-43eb-800b-b531f2e551fe" width="700" alt="Streaming Anime PiP">
          </td>
        </tr>
      </table>
* **Offline Data Management:**
    * Implementation of **Room Persistence Library** for local data storage, significantly reducing API requests and improving app performance.
    * Caching of frequently accessed anime data, enabling offline browsing and faster loading times.
    * User saved lists, and watch histories stored locally.
* **User-Friendly Interface:**
    * Intuitive and visually appealing interface, designed for optimal user experience on Android devices.
    * Clean and organized layout for easy navigation and content discovery.
    * **Dark mode support.**
    * **Loading Skeleton** for placeholder UI.

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
* **Image Loading:** Coil for efficient image loading and caching.
* **Streaming:** ExoPlayer for video playback.
* **Network inspection:** Chucker Interceptor.
* **Retrofit2:** for api interaction.

## Next Update

I'm actively working on enhancing AnimeApp with the following features and improvements:

* **Recent Watched Episode Tracking:** Easily pick up where you left off with a dedicated section for recently watched episodes.
* **Homepage for Recent Releases:** Stay up-to-date with the latest anime releases directly from the app's homepage.
* **Multiplatform Conversion:** We're exploring the possibility of expanding AnimeApp to other platforms.
