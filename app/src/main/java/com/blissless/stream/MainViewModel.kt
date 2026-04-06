package com.blissless.stream

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

@OptIn(kotlinx.coroutines.FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TMDB_API_KEY = BuildConfig.TMDB_API_KEY
        private const val TMDB_BASE_URL = "https://api.themoviedb.org/3"
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        private const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780"
        private const val DEBOUNCE_DELAY = 500L
        private const val CACHE_DURATION_MS = 10 * 60 * 1000L // 10 minutes

        private val movieGenreMap = mapOf(
            28 to "Action",
            12 to "Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            14 to "Fantasy",
            36 to "History",
            27 to "Horror",
            10402 to "Music",
            9648 to "Mystery",
            10749 to "Romance",
            878 to "Sci-Fi",
            10770 to "TV Movie",
            53 to "Thriller",
            10752 to "War",
            37 to "Western"
        )

        private val tvGenreMap = mapOf(
            10759 to "Action & Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            10762 to "Kids",
            9648 to "Mystery",
            10763 to "News",
            10764 to "Reality",
            10765 to "Sci-Fi & Fantasy",
            10766 to "Soap",
            10767 to "Talk",
            10768 to "War & Politics",
            37 to "Western"
        )

        fun getGenreNames(genreIds: List<Int>, mediaType: String): List<String> {
            val genreMap = if (mediaType == "tv") tvGenreMap else movieGenreMap
            return genreIds.mapNotNull { genreMap[it] }.take(3)
        }

        const val GENRE_ACTION = 28
        const val GENRE_COMEDY = 35
        const val GENRE_HORROR = 27
        const val GENRE_SCIFI = 878
        const val GENRE_ANIMATION = 16
        const val GENRE_THRILLER = 53
        const val GENRE_FANTASY = 14
        const val GENRE_CRIME = 80
    }

    private val _searchResults = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _seasonInfo = MutableStateFlow(Pair(0, 0))

    private val _trending = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _popularMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _popularTVShows = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _topRatedMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _nowPlaying = MutableStateFlow<List<ContentItem>>(emptyList())

    private val _actionMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _comedyMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _horrorMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _sciFiMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _animationMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _thrillerMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _fantasyMovies = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _topRatedTVShows = MutableStateFlow<List<ContentItem>>(emptyList())
    private val _crimeTVShows = MutableStateFlow<List<ContentItem>>(emptyList())

    val searchResults: StateFlow<List<ContentItem>> = _searchResults.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()
    val seasonInfo: StateFlow<Pair<Int, Int>> = _seasonInfo.asStateFlow()

    val trending: StateFlow<List<ContentItem>> = _trending.asStateFlow()
    val popularMovies: StateFlow<List<ContentItem>> = _popularMovies.asStateFlow()
    val popularTVShows: StateFlow<List<ContentItem>> = _popularTVShows.asStateFlow()
    val topRatedMovies: StateFlow<List<ContentItem>> = _topRatedMovies.asStateFlow()
    val nowPlaying: StateFlow<List<ContentItem>> = _nowPlaying.asStateFlow()

    val actionMovies: StateFlow<List<ContentItem>> = _actionMovies.asStateFlow()
    val comedyMovies: StateFlow<List<ContentItem>> = _comedyMovies.asStateFlow()
    val horrorMovies: StateFlow<List<ContentItem>> = _horrorMovies.asStateFlow()
    val sciFiMovies: StateFlow<List<ContentItem>> = _sciFiMovies.asStateFlow()
    val animationMovies: StateFlow<List<ContentItem>> = _animationMovies.asStateFlow()
    val thrillerMovies: StateFlow<List<ContentItem>> = _thrillerMovies.asStateFlow()
    val fantasyMovies: StateFlow<List<ContentItem>> = _fantasyMovies.asStateFlow()
    val topRatedTVShows: StateFlow<List<ContentItem>> = _topRatedTVShows.asStateFlow()
    val crimeTVShows: StateFlow<List<ContentItem>> = _crimeTVShows.asStateFlow()

    private var searchJob: Job? = null
    private val episodeCache = mutableMapOf<String, Int>()
    private val seasonCache = mutableMapOf<Int, Map<Int, Int>>()
    private val contentDetailsCache = mutableMapOf<String, ContentDetails>()

    private data class CacheEntry<T>(val data: T, val timestamp: Long)
    private val tmdbCache = mutableMapOf<String, CacheEntry<*>>()

    private val searchTrigger = MutableStateFlow("")

    @Suppress("UNCHECKED_CAST")
    private fun <T> getFromCache(key: String): T? {
        val entry = tmdbCache[key] as? CacheEntry<T> ?: return null
        val now = System.currentTimeMillis()
        return if (now - entry.timestamp < CACHE_DURATION_MS) {
            entry.data
        } else {
            tmdbCache.remove(key)
            null
        }
    }

    private fun <T> putInCache(key: String, data: T) {
        tmdbCache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    init {
        viewModelScope.launch {
            searchTrigger
                .debounce(DEBOUNCE_DELAY)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun searchContent(query: String) {
        searchTrigger.value = query
    }

    /**
     * Clears search results - called when opening search overlay
     * to prevent previous results from appearing
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _isLoading.value = false
        _error.value = null
    }

    /**
     * Clears the search trigger to prevent debounced searches from firing
     */
    fun clearSearchTrigger() {
        searchTrigger.value = ""
        searchJob?.cancel()
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
        val cacheKey = "search_$query"
        val cached: List<ContentItem>? = getFromCache(cacheKey)
        if (cached != null) return@withContext cached

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
            val results = parseSearchResults(response)
            putInCache(cacheKey, results)
            results
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

            if (mediaType == "person") continue

            val name = item.optString("title").ifEmpty { item.optString("name") }
            val id = item.optInt("id")
            val posterPath = item.optString("poster_path")
            val backdropPath = item.optString("backdrop_path")
            val posterUrl = if (posterPath.isNotEmpty()) "$IMAGE_BASE_URL$posterPath" else null
            val backdropUrl = if (backdropPath.isNotEmpty()) "$BACKDROP_BASE_URL$backdropPath" else null
            val voteAverage = item.optDouble("vote_average", 0.0)

            val genreIdsArray = item.optJSONArray("genre_ids")
            val genreIds = mutableListOf<Int>()
            genreIdsArray?.let {
                for (j in 0 until it.length()) {
                    genreIds.add(it.getInt(j))
                }
            }

            results.add(
                ContentItem(
                    id = id,
                    name = name,
                    type = mediaType,
                    posterUrl = posterUrl,
                    backdropUrl = backdropUrl,
                    voteAverage = voteAverage,
                    genreIds = genreIds
                )
            )
        }

        return results
    }

    suspend fun getContentDetails(contentItem: ContentItem): ContentDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "${contentItem.type}-${contentItem.id}"
        contentDetailsCache[cacheKey]?.let { return@withContext it }

        val endpoint = if (contentItem.type == "movie") "movie/${contentItem.id}" else "tv/${contentItem.id}"
        val url = URL("$TMDB_BASE_URL/$endpoint?language=en-US")

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
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val details = parseContentDetails(response, contentItem.type)
            if (details != null) {
                contentDetailsCache[cacheKey] = details
            }
            details
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseContentDetails(jsonResponse: String, mediaType: String): ContentDetails? {
        return try {
            val json = JSONObject(jsonResponse)

            val title = json.optString("title").ifEmpty { json.optString("name") }
            val overview = json.optString("overview", "No description available.")
            val posterPath = json.optString("poster_path")
            val backdropPath = json.optString("backdrop_path")
            val voteAverage = json.optDouble("vote_average", 0.0)
            val voteCount = json.optInt("vote_count", 0)

            val genresArray = json.optJSONArray("genres")
            val genres = mutableListOf<String>()
            genresArray?.let {
                for (i in 0 until it.length()) {
                    val genreObj = it.getJSONObject(i)
                    genres.add(genreObj.optString("name"))
                }
            }

            val releaseDate = json.optString("release_date").ifEmpty { json.optString("first_air_date", "") }

            val runtime = if (mediaType == "movie") {
                json.optInt("runtime", 0)
            } else {
                json.optInt("episode_run_time", 0).let {
                    val runTimes = json.optJSONArray("episode_run_time")
                    if (runTimes != null && runTimes.length() > 0) runTimes.getInt(0) else 0
                }
            }

            val numberOfSeasons = if (mediaType == "tv") json.optInt("number_of_seasons", 1) else 1
            val numberOfEpisodes = if (mediaType == "tv") json.optInt("number_of_episodes", 1) else 1
            val status = json.optString("status", "")

            val tagline = json.optString("tagline", "")

            val seasonsArray = json.optJSONArray("seasons")
            val seasons = mutableListOf<SeasonInfo>()
            seasonsArray?.let {
                for (i in 0 until it.length()) {
                    val seasonObj = it.getJSONObject(i)
                    val seasonNumber = seasonObj.optInt("season_number", 0)
                    val episodeCount = seasonObj.optInt("episode_count", 0)
                    val seasonName = seasonObj.optString("name", "Season $seasonNumber")
                    if (seasonNumber > 0) {
                        seasons.add(SeasonInfo(seasonNumber, episodeCount, seasonName))
                    }
                }
            }
            if (seasons.isEmpty() && mediaType == "tv") {
                for (i in 1..numberOfSeasons) {
                    seasons.add(SeasonInfo(i, 24))
                }
            }

            ContentDetails(
                id = json.optInt("id"),
                title = title,
                overview = overview,
                posterUrl = if (posterPath.isNotEmpty()) "$IMAGE_BASE_URL$posterPath" else null,
                backdropUrl = if (backdropPath.isNotEmpty()) "$BACKDROP_BASE_URL$backdropPath" else null,
                voteAverage = voteAverage,
                voteCount = voteCount,
                genres = genres,
                releaseDate = releaseDate,
                runtime = runtime,
                numberOfSeasons = numberOfSeasons,
                numberOfEpisodes = numberOfEpisodes,
                status = status,
                tagline = tagline,
                type = mediaType,
                seasons = seasons
            )
        } catch (_: Exception) {
            null
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDB("trending/all/week")
                _trending.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load trending: ${e.message}"
            }
        }
    }

    fun loadPopularMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDB("movie/popular", "movie")
                _popularMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load popular movies: ${e.message}"
            }
        }
    }

    fun loadPopularTVShows() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDB("tv/popular", "tv")
                _popularTVShows.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load popular TV shows: ${e.message}"
            }
        }
    }

    fun loadTopRatedMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDB("movie/top_rated", "movie")
                _topRatedMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load top rated: ${e.message}"
            }
        }
    }

    fun loadNowPlaying() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDB("movie/now_playing", "movie", page = 1)
                _nowPlaying.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load now playing: ${e.message}"
            }
        }
    }

    fun loadActionMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/movie", GENRE_ACTION, "movie")
                _actionMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load action movies: ${e.message}"
            }
        }
    }

    fun loadComedyMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/movie", GENRE_COMEDY, "movie")
                _comedyMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load comedy movies: ${e.message}"
            }
        }
    }

    fun loadHorrorMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/movie", GENRE_HORROR, "movie")
                _horrorMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load horror movies: ${e.message}"
            }
        }
    }

    fun loadSciFiMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/movie", GENRE_SCIFI, "movie")
                _sciFiMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load sci-fi movies: ${e.message}"
            }
        }
    }

    fun loadAnimationMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/movie", GENRE_ANIMATION, "movie")
                _animationMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load animation: ${e.message}"
            }
        }
    }

    fun loadThrillerMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/movie", GENRE_THRILLER, "movie")
                _thrillerMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load thriller movies: ${e.message}"
            }
        }
    }

    fun loadFantasyMovies() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/movie", GENRE_FANTASY, "movie")
                _fantasyMovies.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load fantasy movies: ${e.message}"
            }
        }
    }

    fun loadTopRatedTVShows() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDB("tv/top_rated", "tv")
                _topRatedTVShows.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load top rated TV shows: ${e.message}"
            }
        }
    }

    fun loadCrimeTVShows() {
        viewModelScope.launch {
            try {
                val results = fetchFromTMDBWithGenre("discover/tv", GENRE_CRIME, "tv")
                _crimeTVShows.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load crime TV shows: ${e.message}"
            }
        }
    }

    fun loadExploreContent() {
        loadTrending()
        loadPopularMovies()
        loadPopularTVShows()
        loadTopRatedMovies()
        loadTopRatedTVShows()
        loadActionMovies()
        loadComedyMovies()
        loadHorrorMovies()
        loadSciFiMovies()
        loadAnimationMovies()
        loadThrillerMovies()
        loadFantasyMovies()
    }

    private suspend fun fetchFromTMDB(endpoint: String, forceMediaType: String? = null, page: Int = 1): List<ContentItem> = withContext(Dispatchers.IO) {
        val cacheKey = "tmdb_${endpoint}_$page"
        val cached: List<ContentItem>? = getFromCache(cacheKey)
        if (cached != null) return@withContext cached

        val url = URL("$TMDB_BASE_URL/$endpoint?language=en-US&page=$page")

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
                return@withContext emptyList()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val results = parseContentResults(response, forceMediaType)
            putInCache(cacheKey, results)
            results
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetchFromTMDBWithGenre(endpoint: String, genreId: Int, mediaType: String): List<ContentItem> = withContext(Dispatchers.IO) {
        val cacheKey = "tmdb_${endpoint}_${genreId}"
        val cached: List<ContentItem>? = getFromCache(cacheKey)
        if (cached != null) return@withContext cached

        val url = URL("$TMDB_BASE_URL/$endpoint?language=en-US&page=1&with_genres=$genreId&sort_by=popularity.desc")

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
                return@withContext emptyList()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val results = parseContentResults(response, mediaType)
            putInCache(cacheKey, results)
            results
        } finally {
            connection.disconnect()
        }
    }

    private fun parseContentResults(jsonResponse: String, forceMediaType: String? = null): List<ContentItem> {
        val results = mutableListOf<ContentItem>()
        val jsonObject = JSONObject(jsonResponse)
        val resultsArray = jsonObject.optJSONArray("results") ?: return results

        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.getJSONObject(i)

            val mediaType = forceMediaType ?: item.optString("media_type", if (item.has("title")) "movie" else "tv")
            val name = item.optString("title").ifEmpty { item.optString("name") }
            val id = item.optInt("id")
            val posterPath = item.optString("poster_path")
            val backdropPath = item.optString("backdrop_path")
            val posterUrl = if (posterPath.isNotEmpty()) "$IMAGE_BASE_URL$posterPath" else null
            val backdropUrl = if (backdropPath.isNotEmpty()) "$BACKDROP_BASE_URL$backdropPath" else null
            val voteAverage = item.optDouble("vote_average", 0.0)

            results.add(
                ContentItem(
                    id = id,
                    name = name,
                    type = mediaType,
                    posterUrl = posterUrl,
                    backdropUrl = backdropUrl,
                    voteAverage = voteAverage
                )
            )
        }

        return results
    }

    suspend fun getSeasonInfoWithEpisodes(seriesId: Int): Map<Int, Int> = withContext(Dispatchers.IO) {
        val cacheKey = "season_$seriesId"
        val cached: Map<Int, Int>? = getFromCache(cacheKey)
        if (cached != null) return@withContext cached

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
                return@withContext emptyMap()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)

            val seasonsArray = jsonObject.optJSONArray("seasons") ?: return@withContext emptyMap()
            val seasonEpisodeMap = mutableMapOf<Int, Int>()

            for (i in 0 until seasonsArray.length()) {
                val seasonObj = seasonsArray.getJSONObject(i)
                val seasonNumber = seasonObj.optInt("season_number")
                val episodeCount = seasonObj.optInt("episode_count")

                if (episodeCount > 0 && seasonNumber > 0) {
                    seasonEpisodeMap[seasonNumber] = episodeCount
                    episodeCache["$seriesId-$seasonNumber"] = episodeCount
                }
            }

            putInCache(cacheKey, seasonEpisodeMap)
            seasonCache[seriesId] = seasonEpisodeMap

            val totalSeasons = seasonEpisodeMap.keys.maxOrNull() ?: 0
            val totalEpisodes = seasonEpisodeMap.values.sum()
            _seasonInfo.value = Pair(totalSeasons, totalEpisodes)

            seasonEpisodeMap
        } catch (_: Exception) {
            emptyMap()
        } finally {
            connection.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

data class ContentItem(
    val id: Int,
    val name: String,
    val type: String,
    val posterUrl: String?,
    val backdropUrl: String? = null,
    val voteAverage: Double = 0.0,
    val genreIds: List<Int> = emptyList(),
    val progressPosition: Long = 0L,
    val progressDuration: Long = 0L,
    val progressSeason: Int = 1,
    val progressEpisode: Int = 1
)

data class SeasonInfo(
    val seasonNumber: Int,
    val episodeCount: Int,
    val name: String = "Season $seasonNumber"
)

data class ContentDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<String>,
    val releaseDate: String,
    val runtime: Int,
    val numberOfSeasons: Int,
    val numberOfEpisodes: Int,
    val status: String,
    val tagline: String,
    val type: String,
    val seasons: List<SeasonInfo> = emptyList()
)