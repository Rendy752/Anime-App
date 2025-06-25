# AnimeVibe

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

AnimeVibe is a comprehensive, anime-themed Android application using Kotlin and Jetpack Compose designed to provide users with a seamless and immersive experience for discovering, exploring, and enjoying their favorite anime. Leveraging a robust architecture and efficient data management, this app aims to be the go-to resource for anime enthusiasts.

## Key Features

* **Extensive Anime Database:**
    * Access a vast and up-to-date catalog of anime titles, including detailed information such as genres, synopsis, release dates, ratings, and character profiles.
    * Regularly updated data fetched from **Jikan API** and **Aniwatch API**, ensuring users have access to the latest releases and information.
    * Connectivity Check: The app intelligently manages network connectivity. It proactively checks for internet access before making API requests and provides informative feedback to the user in case of network issues, ensuring a seamless experience even with intermittent connections.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/8bf69e3c-d51d-4588-b545-ba0c4ef098c2" width="300" alt="Connectivity Check">
          </td>
        </tr>
     </table>
* **Home:**
  * Discover the Top 10 Airing Anime, see Today's Anime Schedule with day filters and page navigation, and option to quickly resume watching with a "Continue Watching" suggestion.
    <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/8629e4aa-db25-41e3-850b-39bb9750fab4" width="300" alt="Home Portrait">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/9d4a1fe3-8086-47e9-9c94-27ea8b74cf2b" width="600" alt="Home Landscape">
          </td>
        </tr>
      </table>
  * Pull-to-refresh for the latest info.
    <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/69d7550b-f61b-4427-9be1-cd655458509e" width="300" alt="Home Skeleton Loading">
          </td>
        </tr>
     </table>
* **Personalized Recommendations:**
    * Intelligent recommendation engine that suggests **two** similar anime based on user preferences and trending titles.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/a3be233e-6aa7-42f9-9695-7a2c5055ee7c" width="300" alt="Recommendations Anime Skeleton Loading">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/5f659c66-37f2-432d-a39a-ec4128990c5c" width="300" alt="Recommendations Anime Portrait">
          </td>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/6691f185-ad85-4276-8054-81222a56ac80" width="700" alt="Recommendations Anime Landscape">
          </td>
        </tr>
      </table>
* **Episode History:**
    * Track and manage your anime watch history with a dedicated Episode History screen, storing episode details (e.g., last watched timestamp, progress) in Room Persistence Library for offline access.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/080e8145-5896-4393-ad3a-4a5b26114db4" width="300" alt="Episode History Skeleton Loading">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/538b85ec-e09a-4630-8d8d-c1175fdeafeb" width="300" alt="Episode History Portrait">
          </td>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/9e7928ab-68d9-4253-9696-4b336c0153e5" width="700" alt="Episode History Landscape">
          </td>
        </tr>
      </table>
    * View history grouped by anime, with expandable accordions showing episode details, including thumbnails, progress bars, and watch timestamps.
    * Interactive actions: favorite/unfavorite anime or episodes, delete individual episodes or entire anime history, and navigate to anime details or episode streaming with a single tap.
    * Advanced filtering by search query, favorite status, and sorting options (e.g., newest first, episode title) with debounced search for smooth performance.
    * Pull-to-refresh to update history and automatic refresh on network reconnection after errors.
* **Advanced Filtering and Search:**
    * Efficient search functionality for quickly finding specific anime titles.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/25f464cf-1468-428e-8806-e043af125a00" width="300" alt="Search Anime Skeleton Loading">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/8d3087bb-3875-4a0d-b61a-679605f62f71" width="300" alt="Search Anime Portrait">
          </td>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/d4b1a600-5366-41c2-8788-7df6d6bcb360" width="700" alt="Search Anime Landscape">
          </td>
        </tr>
      </table>
    * Powerful filtering options to refine searches by **keyword, limit, pagination, genre, producers, and more via a bottom sheet interface.**
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/3cc31305-610d-4f5c-9ccd-b3fef8c49c53" width="300" alt="Anime Filter Bottom Sheet">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/7175479a-674b-4ee6-9ba3-b37fc731b157" width="300" alt="Genre Filter Bottom Sheet">
          </td>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/a2492278-e3e6-457d-aa80-b1b074713fb0" width="300" alt="Producer Filter Bottom Sheet">
          </td>
        </tr>
      </table>
* **Detailed Anime Information:**
    * Data stored in **Room Persistence Library**.
    * Comprehensive anime details pages, including high-resolution cover art, character descriptions, and episode lists with search filter.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/46409b76-bdd2-4917-8e2e-13cdde12ea5e" width="300" alt="Detail Anime Skeleton Loading">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/bf8e664d-aba9-4b2d-8b5b-8d06fb5d4fcd" width="300" alt="Detail Anime First Half">
          </td>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/51528d95-6d41-4a23-83fe-1da13d9b6b88" width="300" alt="Detail Anime Second Half">
          </td>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/b47c9e8e-d827-4b78-9fa9-f53f1baaa716" width="700" alt="Detail Anime Landscape">
          </td>
        </tr>
      </table>
* **Episode Streaming:**
    * Integrated episode streaming capabilities, allowing users to watch anime directly within the app, using **Aniwatch API**.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/6723d000-77e8-45e4-a0d2-f484d831c3db" width="300" alt="Streaming Anime Skeleton Loading">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/ecff285a-e7d2-430d-a0ea-a7df8d3bf712" width="300" alt="Streaming Anime Portrait">
          </td>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/6166cbce-370e-458f-a254-3bbdfb7f23c5" width="700" alt="Streaming Anime Landscape">
          </td>
        </tr>
      </table>
    * Store episode streaming data, server and source in **Room Persistence Library**.
    * Support **Picture in Picture (PiP)** mode with with **play/pause** and **seek** controls.
      <table>
        <tr>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/dd796d95-9a43-4c51-97e0-746146ca3b0a" width="300" alt="Streaming Anime PiP">
          </td>
        </tr>
      </table>
* **Media Playback Controls**:
    * Rich media playback notifications with **play/pause**, **next/previous** episode, and **seek** controls, accessible from the lock screen or notification shade.
    * Deep linking to specific episodes via notifications, allowing users to resume watching seamlessly.
    * Periodic watch state updates (position, duration, screenshots) stored locally for accurate Continue Watching functionality.
      <table>
         <tr>
            <td style="text-align: center">
               <img src="https://github.com/user-attachments/assets/d2464155-e7c4-47b6-9391-c39749f7fa88" width="300" alt="Media Playback 1">
            </td>
            <td style="text-align: center">
               <img src="https://github.com/user-attachments/assets/7d5ea1cc-dc87-45be-a4a0-190bc2e73b4b" width="300" alt="Media Playback 2">
            </td>
         </tr>
      </table>
* **Latest Watched Widget:**
    * A home screen widget displaying the most recently watched anime episode with episode title, thumbnail, watch progress, timestamp, and last watched time.
    * Interactive features: Tap the widget to resume watching the episode via deep linking, or use the refresh button to update the widget with the latest data.
    * Visual indicators: Progress bar shows episode watch progress, and distinct gradient backgrounds differentiate filler episodes.
    * Powered by Room Persistence Library for offline data access and Coil for efficient image loading.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/3e76418e-019b-4dd6-91c8-0ec65fd4aca2" width="300" alt="Add Latest Watched Widget">
          </td>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/92192ff3-814b-4bed-966f-b632c4d804b1" width="300" alt="Latest Watched Widget">
          </td>
        </tr>
      </table>
* **Airing Notifications**:
    * Timely reminders for upcoming anime broadcasts, powered by a WorkManager-based scheduler that runs daily at midnight (Asia/Jakarta time).
    * Checks for airing anime within a 30-minute window (11:45 PM–12:15 AM) and sends notifications for episodes airing within 5 minutes, including a deep link to the anime’s detail page.
    * Schedules future notifications for later broadcasts, ensuring timely alerts without redundant API calls.
    * Features duplicate notification prevention, automatic cleanup of old notifications, and temporary pausing of media playback services to optimize performance.
    * Robust error handling with up to three retries using exponential backoff, and skips processing if notifications are disabled or no internet is available (uses cached data when offline).
    * Configurable via the settings screen, with permission handling for Android 13+.
      <table>
         <tr>
            <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/31f6ffb6-07c9-4dab-900b-768cbbbf20a6" width="300" alt="Airing Notification">
          </td>
        </tr>
      </table>
* **Unfinished Watch Notifications**:
    * Gentle reminders to resume unfinished anime episodes, scheduled twice daily at 8:00 AM and 8:00 PM (Asia/Jakarta time) using WorkManager.
    * Operates within 30-minute windows (7:45–8:15 AM and 7:45–8:15 PM), randomly selecting an unfinished episode with remaining episodes in the series.
    * Notifications include episode title, progress details, and a deep link to resume watching, with the number of remaining episodes for context.
    * Features duplicate notification prevention, automatic cleanup of old notifications, and validation to ensure only relevant episodes are notified.
    * Robust error handling with up to three retries using exponential backoff, and skips processing if notifications are disabled.
* **Offline Data Management:**
    * Implementation of **Room Persistence Library** for local data storage, significantly reducing API requests and improving app performance.
    * Caching of frequently accessed anime data, enabling offline browsing and faster loading times.
    * User saved lists, and watch histories stored locally.
* **User-Friendly Interface:**
    * Intuitive Jetpack Compose-based UI optimized for Android, with **dark mode**, **normal/medium/high** contrast modes, and **customizable color styles** (Default, Vibrant, Monochrome).
      <table>
        <tr>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/a2a06c40-b92d-45cb-9f54-e6b17dbad91a" width="300" alt="Dark and Contrast Modes Setting">
          </td>
        </tr>
      </table>
    * Color style previews showcase real anime data (fetched randomly via API) in settings, ensuring an engaging and accurate representation of the selected scheme.
      <table>
        <tr>
           <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/9208aa3b-4190-4f4b-85e4-13c9ad597c82" width="300" alt="Color Style Previews">
          </td>
        </tr>
      </table>
    * Loading Skeletons provide visual feedback during data loading, improving the perceived performance of the app.
    * Idle checking to optimize resource usage and enhance the user experience, the app incorporates an idle detection mechanism.
      <table>
        <tr>
          <td style="text-align: center">
            <img src="https://github.com/user-attachments/assets/3fb03333-5185-4c2d-8948-8eeeb3fc1aa2" width="300" alt="Idle Check">
          </td>
        </tr>
     </table>

## CI/CD with GitHub Actions

AnimeVibe utilizes GitHub Actions for Continuous Integration and Continuous Delivery (CI/CD). This automated workflow ensures code quality and facilitates smoother development by:

* **Automated Building and Testing:** On every code push to the `main`, `feature/*`, and `enhancement/*` branches, and on pull requests targeting these branches, GitHub Actions automatically checks out the code, sets up the Java environment, and runs unit tests and lint checks.
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

I'm actively working on enhancing AnimeVibe with the following features and improvements:
* **Multiplatform Conversion:** We're exploring the possibility of expanding AnimeVibe to other platforms.
