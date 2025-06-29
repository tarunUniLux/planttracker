package com.nurat.planttracker.worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nurat.planttracker.data.PlantStateRepositoryImpl
import com.nurat.planttracker.manager.PlantManager
import com.nurat.planttracker.service.GitHubService
import com.nurat.planttracker.service.GitHubServiceImpl // Import the real implementation
import java.time.Instant

class PlantCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        return try {
            val plantStateRepository = PlantStateRepositoryImpl(applicationContext)
            val plantManager = PlantManager(plantStateRepository)

            // Get GitHub credentials from repository
            val username = plantStateRepository.getGitHubUsername()
            val token = plantStateRepository.getGitHubAccessToken()

            if (username.isNullOrEmpty() || token.isNullOrEmpty()) {
                println("PlantCheckWorker: GitHub credentials not set. Cannot check commits.")
                return Result.failure() // Or retry, depending on desired behavior
            }

            // Instantiate real GitHubService
            val githubService: GitHubService = GitHubServiceImpl { token }

            // First, check for wilting based on the last commit timestamp
            plantManager.checkAndAdjustPlantState()
            println("PlantCheckWorker: Performed daily wilt check.")

            // Then, check for new commits and grow the plant if needed.
            val lastCommitTime = plantStateRepository.getLastCommitTimestamp() ?: Instant.EPOCH
            val newCommits = githubService.getCommitsSince(
                username = username,
                repo = null, // Check all user's repos for simplicity or pass specific repo
                since = lastCommitTime
            )

            if (newCommits.isNotEmpty()) {
                plantManager.grow()
                println("PlantCheckWorker: New commits found. Plant grew!")
            } else {
                println("PlantCheckWorker: No new commits found since last check.")
            }

            Result.success()
        } catch (e: Exception) {
            println("PlantCheckWorker failed: ${e.message}")
            Result.failure()
        }
    }
}