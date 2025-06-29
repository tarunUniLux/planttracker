//package com.nurat.planttracker.service
//
//import android.os.Build
//import androidx.annotation.RequiresApi
//import com.google.gson.annotations.SerializedName
//import okhttp3.Interceptor
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.GET
//import retrofit2.http.Header
//import retrofit2.http.Path
//import retrofit2.http.Query
//import java.time.Instant
//
//// Interface for interacting with the GitHub API.
//// This interface defines the contract for GitHub service.
//interface GitHubService {
//    /**
//     * Fetches commits for a given user within a specified time frame.
//     *
//     * @param username The GitHub username.
//     * @param repo The repository name (or null/empty for all user repos, though this requires
//     * more complex API calls and permissions).
//     * @param since A timestamp (ISO 8601 format) to only return commits after this time.
//     * @return A list of commit messages. Returns an empty list if no commits or error.
//     */
//    suspend fun getCommitsSince(username: String, repo: String? = null, since: Instant): List<String>
//}
//
//// Data classes to match GitHub API JSON response for commits
//data class GitHubCommitResponse(
//    val commit: CommitDetail,
//    val sha: String,
//    val html_url: String
//)
//
//data class CommitDetail(
//    val message: String,
//    val committer: Committer
//)
//
//data class Committer(
//    val name: String,
//    val date: String // ISO 8601 format string
//)
//
//// Retrofit API Interface
//interface GitHubApi {
//    @GET("user/repos") // Get authenticated user's repositories
//    suspend fun getUserRepositories(
//        @Header("Authorization") authHeader: String
//    ): List<GitHubRepository>
//
//    @GET("repos/{owner}/{repo}/commits")
//    suspend fun getRepoCommits(
//        @Header("Authorization") authHeader: String,
//        @Path("owner") owner: String,
//        @Path("repo") repo: String,
//        @Query("since") since: String? = null, // ISO 8601 format timestamp
//        @Query("per_page") perPage: Int = 30 // Number of commits to fetch per page
//    ): List<GitHubCommitResponse>
//}
//
//// Data class for GitHub Repository response (simplified)
//data class GitHubRepository(
//    val name: String,
//    val owner: RepoOwner // Nested owner object
//)
//
//data class RepoOwner(
//    @SerializedName("login")
//    val login: String // The owner's GitHub username
//)
//
//
//// Concrete implementation of GitHubService
//class GitHubServiceImpl(private val githubTokenProvider: () -> String?) : GitHubService {
//
//    private val retrofit: Retrofit by lazy {
//        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
//
//        val authInterceptor = Interceptor { chain ->
//            val original = chain.request()
//            val requestBuilder = original.newBuilder()
//            val token = githubTokenProvider() // Get token from the provider
//            token?.let {
//                requestBuilder.header("Authorization", "token $it")
//            }
//            chain.proceed(requestBuilder.build())
//        }
//
//        val httpClient = OkHttpClient.Builder()
//            .addInterceptor(authInterceptor)
//            .addInterceptor(logging) // Add logging interceptor for debugging
//            .build()
//
//        Retrofit.Builder()
//            .baseUrl("https://api.github.com/")
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    private val githubApi: GitHubApi by lazy {
//        retrofit.create(GitHubApi::class.java)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override suspend fun getCommitsSince(username: String, repo: String?, since: Instant): List<String> {
//        val accessToken = githubTokenProvider()
//        if (accessToken == null) {
//            println("GitHub access token not available. Cannot fetch commits.")
//            return emptyList()
//        }
//
//        return try {
//            val commits = if (repo != null) {
//                // If a specific repo is provided, fetch commits from that repo
//                githubApi.getRepoCommits(
//                    authHeader = "token $accessToken",
//                    owner = username, // Assuming username is the owner for simplicity
//                    repo = repo,
//                    since = since.toString()
//                )
//            } else {
//                // If no specific repo, try to fetch from all user's repos
//                // This is more complex as it involves iterating through repos and getting their commits.
//                // For a truly real app, you might ask the user to select repos or track a few defaults.
//                // For now, let's assume if repo is null, we try to get commits from first user's repo.
//                val userRepos = githubApi.getUserRepositories(authHeader = "token $accessToken")
//                if (userRepos.isNotEmpty()) {
//                    val firstRepo = userRepos.first()
//                    githubApi.getRepoCommits(
//                        authHeader = "token $accessToken",
//                        owner = firstRepo.owner.login,
//                        repo = firstRepo.name,
//                        since = since.toString()
//                    )
//                } else {
//                    println("No repositories found for user $username.")
//                    emptyList()
//                }
//            }
//            commits.filter {
//                Instant.parse(it.commit.committer.date).isAfter(since)
//            }.map { it.commit.message }
//        } catch (e: Exception) {
//            println("Error fetching GitHub commits: ${e.message}")
//            emptyList()
//        }
//    }
//}

package com.nurat.planttracker.service

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.time.format.DateTimeParseException

// Interface for interacting with the GitHub API.
// This interface defines the contract for GitHub service.
interface GitHubService {
    /**
     * Fetches commits for a given user within a specified time frame.
     *
     * @param username The GitHub username.
     * @param repo The repository name (or null/empty for all user repos, though this requires
     * more complex API calls and permissions).
     * @param since A timestamp (ISO 8601 format) to only return commits after this time.
     * @return A list of commit messages. Returns an empty list if no commits or error.
     */
    suspend fun getCommitsSince(username: String, repo: String? = null, since: Instant): List<String>
}

// Data classes to match GitHub API JSON response for commits
data class GitHubCommitResponse(
    val commit: CommitDetail,
    val sha: String,
    val html_url: String
)

data class CommitDetail(
    val message: String,
    val committer: Committer
)

data class Committer(
    val name: String,
    val date: String // ISO 8601 format string
)

// Retrofit API Interface
interface GitHubApi {
    @GET("user/repos") // Get authenticated user's repositories
    suspend fun getUserRepositories(
        @Header("Authorization") authHeader: String
    ): List<GitHubRepository>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getRepoCommits(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("since") since: String? = null, // ISO 8601 format timestamp
        @Query("per_page") perPage: Int = 30 // Number of commits to fetch per page
    ): List<GitHubCommitResponse>
}

// Data class for GitHub Repository response (simplified)
data class GitHubRepository(
    val name: String,
    val owner: RepoOwner // Nested owner object
)

data class RepoOwner(
    @SerializedName("login")
    val login: String // The owner's GitHub username
)


// Concrete implementation of GitHubService
class GitHubServiceImpl(private val githubTokenProvider: () -> String?) : GitHubService {

    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            val token = githubTokenProvider() // Get token from the provider
            token?.let {
                requestBuilder.header("Authorization", "token $it")
            }
            chain.proceed(requestBuilder.build())
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging) // Add logging interceptor for debugging
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val githubApi: GitHubApi by lazy {
        retrofit.create(GitHubApi::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getCommitsSince(username: String, repo: String?, since: Instant): List<String> {
        val accessToken = githubTokenProvider()
        if (accessToken == null) {
            println("GitHub access token not available. Cannot fetch commits.")
            return emptyList()
        }

        return try {
            val commits: List<GitHubCommitResponse> = if (repo != null) {
                // If a specific repo is provided, fetch commits from that repo
                githubApi.getRepoCommits(
                    authHeader = "token $accessToken",
                    owner = username, // Assuming username is the owner for simplicity
                    repo = repo,
                    since = since.toString()
                )
            } else {
                // If no specific repo, find the 'planttracker' repo owned by the provided username
                val userRepos = githubApi.getUserRepositories(authHeader = "token $accessToken")
                val plantTrackerRepo = userRepos.firstOrNull {
                    it.name.equals("planttracker", ignoreCase = true) && it.owner.login.equals(username, ignoreCase = true)
                }

                if (plantTrackerRepo != null) {
                    println("Found 'planttracker' repository for user $username.")
                    githubApi.getRepoCommits(
                        authHeader = "token $accessToken",
                        owner = plantTrackerRepo.owner.login,
                        repo = plantTrackerRepo.name,
                        since = since.toString()
                    )
                } else {
                    println("Could not find 'planttracker' repository for user $username. Please ensure the repository exists and is named 'planttracker'.")
                    emptyList()
                }
            }
            // Filter commits by date, although the API's 'since' parameter usually handles this.
            // This is a safety filter in case API doesn't strictly adhere or for future changes.
            commits.filter {
                try {
                    Instant.parse(it.commit.committer.date).isAfter(since)
                } catch (e: DateTimeParseException) {
                    println("Warning: Could not parse commit date '${it.commit.committer.date}'. Including commit.")
                    true // Include if date parsing fails, to not miss commits
                }
            }.map { it.commit.message }
        } catch (e: Exception) {
            println("Error fetching GitHub commits: ${e.message}")
            emptyList()
        }
    }
}