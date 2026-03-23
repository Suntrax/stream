# Stream

A modern Android streaming app for discovering and watching movies and TV shows.

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![MinSDK](https://img.shields.io/badge/MinSDK-28-orange.svg)

## Important

I do not host any content within this app. All content is streamed from third-party sources.

## Features

- **Explore** - Browse trending, popular, and top-rated movies and TV shows
- **Search** - Find content instantly with debounced search
- **Streaming** - Watch with built-in HLS player (ExoPlayer)
- **Episode Selection** - Season and episode picker for TV shows
- **Gesture Controls** - Skip buttons, swipe down to close details
- **Auto-next Episode** - Seamless playback continuation

## Requirements

- Android 8.0+ (API 28+)
- TMDB API key (required for content discovery)

## Installation

Download the APK from [Releases](https://github.com/blissless/stream/releases) and install.

## Tech Stack

- Kotlin
- Media3 ExoPlayer
- TMDB API
- Glide
- MVVM Architecture

## Project Structure

```
app/src/main/java/com/blissless/stream/
├── MainActivity.kt       # Main UI, navigation, adapters
├── MainViewModel.kt     # Business logic and API calls
├── api/                 # Retrofit/TMDB API interface
├── models/              # Data classes
└── utils/               # Extensions and helpers
```

## Disclaimer

This app is for educational purposes only. I do not host, upload, or distribute any content. All streaming links are provided by third-party sources.
