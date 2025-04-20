# AnimeApp

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

AnimeApp is a comprehensive, anime-themed Android application using Kotlin and Jetpack Compose designed to provide users with a seamless and immersive experience for discovering, exploring, and enjoying their favorite anime. Leveraging a robust architecture and efficient data management, this app aims to be the go-to resource for anime enthusiasts.

## Key Features

* **Extensive Anime Database:**
    * Access a vast and up-to-date catalog of anime titles, including detailed information such as genres, synopsis, release dates, ratings, and character profiles.
    * Regularly updated data fetched from **Jikan API** and **Aniwatch API**, ensuring users have access to the latest releases and information.
    * Connectivity Check: The app intelligently manages network connectivity. It proactively checks for internet access before making API requests and provides informative feedback to the user in case of network issues, ensuring a seamless experience even with intermittent connections.
      <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/8bf69e3c-d51d-4588-b545-ba0c4ef098c2" width="300" alt="Connectivity Check">
          </td>
        </tr>
     </table>
* **Home:**
  * Discover the Top 10 Airing Anime, see Today's Anime Schedule with day filters and page navigation, and option to quickly resume watching with a "Continue Watching" suggestion.
    <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/069b9388-9151-4526-b259-60777385aee5" width="300" alt="Home Portrait">
          </td>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/57d282ba-7334-40c9-9f37-c7bb21522ed3" width="300" alt="Home Landscape">
          </td>
        </tr>
      </table>
  * Pull-to-refresh for the latest info.
    <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/86f57ef0-c2ab-479c-a9ac-eed0a4bd0669" width="300" alt="Home Skeleton Loading">
          </td>
        </tr>
     </table>
* **Personalized Recommendations:**
    * Intelligent recommendation engine that suggests **two** similar anime based on user preferences and trending titles.
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
    * Dark mode support for comfortable viewing in low-light environments.
    * Loading Skeletons provide visual feedback during data loading, improving the perceived performance of the app.
    * Idle checking to optimize resource usage and enhance the user experience, the app incorporates an idle detection mechanism.
      <table>
        <tr>
          <td align="center">
            <img src="https://github.com/user-attachments/assets/457b5d24-0f2f-4e35-817a-8a48c05760f1" width="300" alt="Idle Check">
          </td>
        </tr>
     </table>

## CI/CD with GitHub Actions

AnimeApp utilizes GitHub Actions for Continuous Integration and Continuous Delivery (CI/CD). This automated workflow ensures code quality and facilitates smoother development by:

* **Automated Building and Testing:** On every code push to the `main`, `feature/*`, and `enhancement/*` branches, and on pull requests targeting these branches, GitHub Actions automatically checks out the code, sets up the Java environment, and runs unit tests and lint checks.
* **Secret Management:** API keys are securely managed as GitHub Secrets, ensuring they are not exposed in the codebase.
* **Dependency Caching:** Gradle dependencies are cached between builds, significantly speeding up the CI process.
* **APK Building:** Upon successful tests and lint checks, a debug APK of the application is automatically built.

This CI/CD pipeline helps maintain a stable and functional codebase by automatically verifying changes and building the app, reducing the risk of introducing bugs and streamlining the release process.

## Technical Specifications

* **Platform:** Android
* **MinSDK:** 27
* **TargetSDK:** 35
* **Programming Languages:** Kotlin
* **Architecture:** MVVM (Model-View-ViewModel)
* **Data Persistence:** Room Persistence Library
* **Serialization:** for efficient data handling.
* **API Integration:** Utilizing **Jikan API** for anime information and **Aniwatch API** for episode streaming.
* **Asynchronous Operations:** Coroutines for efficient background tasks and smooth API calls.
* **Dependency Injection:** Hilt for streamlined dependency management.
* **Image Loading:** Coil for efficient and performant image loading and caching.
* **Streaming:** ExoPlayer for robust and feature-rich video playback.
* **Network inspection:** Chucker Interceptor for in-app network request debugging.
* **Retrofit2:** for type-safe HTTP client implementation.
* **Shake Detection**: Implemented for easy access to the Chucker network inspection tool in debug builds.

## Next Update

I'm actively working on enhancing AnimeApp with the following features and improvements:

* **Recent Watched Episode Tracking:** Easily pick up where you left off with a dedicated section for recently watched episodes.
* **Multiplatform Conversion:** We're exploring the possibility of expanding AnimeApp to other platforms.
