<div align="center">
    
Stream

A modern Android streaming application for discovering and watching movies and TV shows. Built with Kotlin and powered by TMDB API for content discovery.

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![MinSDK](https://img.shields.io/badge/MinSDK-28-orange.svg)

</div>

## ✨ Features

### 🎬 Content Discovery
- **Trending Carousel** - Auto-scrolling featured content with dynamic background transitions
- **Multiple Categories** - Popular movies, TV shows, top rated, now playing, and genre-specific sections
- **Search** - Instant search across movies and TV shows with debounced queries
- **Detailed Info** - Comprehensive content details including ratings, genres, runtime, and status

### 📺 Streaming
- **HLS Streaming** - Smooth video playback via ExoPlayer
- **Episode Selection** - Season and episode picker for TV shows
- **Playback Controls** - Skip forward/backward with double-tap gestures
- **Auto-next Episode** - Automatic playback of next episode
- **Resize Modes** - Fit, Fill, and Stretch options
- **Ad Blocking** - Built-in ad domain blocking for cleaner streaming

### 🎨 Modern UI
- **Dark Theme** - Sleek dark interface optimized for viewing
- **Card-based Design** - Clean Material Design components
- **Smooth Animations** - Crossfade transitions and fluid scrolling
- **Responsive Indicators** - Real-time carousel position updates

## 📱 Screenshots

| Explore Page | Search Results | Content Details |
|:------------:|:--------------:|:---------------:|
| *Trending carousel with categories* | *Compact search cards* | *Full content information* |

| Episode Selector | Video Player |
|:---------------:|:------------:|
| *Season and episode picker* | *ExoPlayer with controls* |

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Async:** Kotlin Coroutines & Flow
- **Networking:** HttpsURLConnection
- **Image Loading:** Glide
- **Video Player:** ExoPlayer (Media3)
- **UI Components:** Material Design Components

## 📦 Dependencies

```gradle
// AndroidX
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'

// Material Design
implementation 'com.google.android.material:material:1.11.0'

// ExoPlayer
implementation 'androidx.media3:media3-exoplayer:1.2.1'
implementation 'androidx.media3:media3-ui:1.2.1'
implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.16.0'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

## 🔑 API Configuration

This app uses the TMDB API for content discovery. To run the app:

1. Get your API key from [TMDB](https://www.themoviedb.org/settings/api)
2. Replace the API key in `MainViewModel.kt`:

```kotlin
companion object {
    private const val TMDB_API_KEY = "YOUR_API_KEY_HERE"
    // ...
}
```

## 🏗️ Building the Project

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/blissless-stream.git
```

2. Open in Android Studio

3. Add your TMDB API key

4. Build and run on device/emulator (API 21+)

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/blissless/stream/
│   │   ├── MainActivity.kt       # Main UI and adapters
│   │   ├── MainViewModel.kt      # Business logic and API calls
│   │   ├── ContentItem.kt        # Data models (in MainViewModel)
│   │   └── ContentDetails.kt     # Detailed content model
│   ├── res/
│   │   ├── drawable/             # Icons and backgrounds
│   │   ├── values/               # Colors, strings, themes
│   │   └── xml/                  # Network security config
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## 🎯 Key Components

### MainActivity
- Creates UI programmatically (no XML layouts)
- Handles navigation between Explore and Search
- Manages video playback and player controls
- Contains all RecyclerView adapters

### MainViewModel
- Manages content state with StateFlow
- Handles API calls with coroutines
- Implements debounced search
- Caches content details and season info

### Adapters
- `SearchOverlayAdapter` - Compact search result cards
- `HorizontalContentAdapter` - Explore section cards
- `CarouselAdapter` - Featured content carousel

## 🔒 Privacy & Security

- No user data collection
- No analytics tracking
- Ad domain blocking for cleaner streaming experience
- Network security configuration for secure connections

## 📄 License

This project is for educational purposes only. Content is provided by TMDB API.

**Note:** This app is not affiliated with TMDB or any streaming service. Use responsibly and respect content copyrights.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---


Made with ❤️ by Blissless

