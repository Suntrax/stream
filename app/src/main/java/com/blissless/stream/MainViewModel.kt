package com.blissless.stream

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // TMDB API Configuration
    companion object {
        private const val TMDB_API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI2YjgxNTQxNDAwYTNjYmNiMmU4MWMyY2ZmYWViYTNjNSIsIm5iZiI6MTc1NTk2MzY5NC42ODYsInN1YiI6IjY4YTllMTJlOWJmNGUzMTRmMzc4MTk3ZSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.TlYw-j9GEaylcyVhawE5ZexOr8p5nFVvLZSMtIRXFD8"
        private const val TMDB_BASE_URL = "https://api.themoviedb.org/3"
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        private const val DEBOUNCE_DELAY = 500L
    }

    // State Flows
    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _seasonInfo = MutableStateFlow<Pair<Int, Int>>(Pair(0, 0))

    // Public State Flows
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val searchResults: StateFlow<List<ContentItem>> = _searchResults.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()
    val seasonInfo: StateFlow<Pair<Int, Int>> = _seasonInfo.asStateFlow()

    // Search job for cancellation
    private var searchJob: Job? = null

    // Episode cache: "seriesId-seasonNumber" -> episodeCount
    private val episodeCache = mutableMapOf<String, Int>()

    // Season cache: seriesId -> Map<seasonNumber, episodeCount>
    private val seasonCache = mutableMapOf<Int, Map<Int, Int>>()

    init {
        setupDebouncedSearch()
    }

    private fun setupDebouncedSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(DEBOUNCE_DELAY)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun searchContent(query: String) {
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val results = searchTMDB(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun searchTMDB(query: String): List<ContentItem> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL("$TMDB_BASE_URL/search/multi?query=$encodedQuery&include_adult=false&language=en-US&page=1")

        val connection = url.openConnection() as HttpsURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("accept", "application/json")
            setRequestProperty("Authorization", "Bearer $TMDB_API_KEY")
            connectTimeout = 10000
            readTimeout = 10000
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw Exception("HTTP Error: $responseCode")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parseSearchResults(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSearchResults(jsonResponse: String): List<ContentItem> {
        val results = mutableListOf<ContentItem>()
        val jsonObject = JSONObject(jsonResponse)
        val resultsArray = jsonObject.optJSONArray("results") ?: return results

        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.getJSONObject(i)
            val mediaType = item.optString("media_type")

            // Skip person results
            if (mediaType == "person") continue

            val name = item.optString("title").ifEmpty { item.optString("name") }
            val id = item.optInt("id")
            val posterPath = item.optString("poster_path")
            val posterUrl = if (posterPath.isNotEmpty()) "$IMAGE_BASE_URL$posterPath" else null

            results.add(
                ContentItem(
                    id = id,
                    name = name,
                    type = mediaType,
                    posterUrl = posterUrl
                )
            )
        }

        return results
    }

    fun getStreamUrl(contentItem: ContentItem, season: Int = 1, episode: Int = 1): String {
        return when (contentItem.type) {
            "movie" -> "https://vidlink.pro/movie/${contentItem.id}"
            "tv" -> "https://vidlink.pro/tv/${contentItem.id}/$season/$episode"
            else -> ""
        }
    }

    /**
     * Returns a map of season number to episode count for all seasons.
     * Only includes seasons that have episodes available (episode_count > 0).
     * This handles cases like "Wednesday" where season 3 exists but has 0 episodes.
     */
    suspend fun getSeasonInfoWithEpisodes(seriesId: Int): Map<Int, Int> = withContext(Dispatchers.IO) {
        // Check cache first
        seasonCache[seriesId]?.let { return@withContext it }

        val url = URL("$TMDB_BASE_URL/tv/$seriesId")

        val connection = url.openConnection() as HttpsURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("accept", "application/json")
            setRequestProperty("Authorization", "Bearer $TMDB_API_KEY")
            connectTimeout = 10000
            readTimeout = 10000
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                return@withContext emptyMap<Int, Int>()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)

            val seasonsArray = jsonObject.optJSONArray("seasons") ?: return@withContext emptyMap<Int, Int>()
            val seasonEpisodeMap = mutableMapOf<Int, Int>()

            for (i in 0 until seasonsArray.length()) {
                val seasonObj = seasonsArray.getJSONObject(i)
                val seasonNumber = seasonObj.optInt("season_number")
                val episodeCount = seasonObj.optInt("episode_count")

                // Only include seasons with actual episodes
                // Also skip season 0 (specials) unless it has episodes
                if (episodeCount > 0 && seasonNumber > 0) {
                    seasonEpisodeMap[seasonNumber] = episodeCount
                    // Also cache individual season data
                    episodeCache["$seriesId-$seasonNumber"] = episodeCount
                }
            }

            // Cache the result
            seasonCache[seriesId] = seasonEpisodeMap

            // Update the old seasonInfo flow for backward compatibility
            val totalSeasons = seasonEpisodeMap.keys.maxOrNull() ?: 0
            val totalEpisodes = seasonEpisodeMap.values.sum()
            _seasonInfo.value = Pair(totalSeasons, totalEpisodes)

            seasonEpisodeMap
        } catch (e: Exception) {
            emptyMap<Int, Int>()
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getSeasonInfo(seriesId: Int): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val seasonData = getSeasonInfoWithEpisodes(seriesId)
        if (seasonData.isEmpty()) {
            Pair(0, 0)
        } else {
            Pair(
                seasonData.keys.maxOrNull() ?: 0,
                seasonData.values.sum()
            )
        }
    }

    suspend fun getEpisodesForSeason(seriesId: Int, seasonNumber: Int): Int {
        // Check cache first
        val cacheKey = "$seriesId-$seasonNumber"
        episodeCache[cacheKey]?.let { return it }

        // Also check the season cache
        seasonCache[seriesId]?.get(seasonNumber)?.let { return it }

        // If not in cache, fetch from API
        return withContext(Dispatchers.IO) {
            val url = URL("$TMDB_BASE_URL/tv/$seriesId/season/$seasonNumber")

            val connection = url.openConnection() as HttpsURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("accept", "application/json")
                setRequestProperty("Authorization", "Bearer $TMDB_API_KEY")
                connectTimeout = 10000
                readTimeout = 10000
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    return@withContext 0
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)

                val episodesArray = jsonObject.optJSONArray("episodes")
                val episodeCount = episodesArray?.length() ?: 0

                // Cache the result
                episodeCache[cacheKey] = episodeCount

                episodeCount
            } catch (e: Exception) {
                0
            } finally {
                connection.disconnect()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

// Data model for content items
data class ContentItem(
    val id: Int,
    val name: String,
    val type: String, // "movie" or "tv"
    val posterUrl: String?
)
