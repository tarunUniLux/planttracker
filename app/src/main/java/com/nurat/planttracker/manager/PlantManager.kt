package com.nurat.planttracker.manager

import android.os.Build
import androidx.annotation.RequiresApi
import com.nurat.planttracker.model.PlantState
import java.time.Duration
import java.time.Instant

// Interface for persisting plant state. This allows for dependency injection and testing.
interface PlantStateRepository {
    fun savePlantState(state: PlantState)
    fun getPlantState(): PlantState
    fun saveLastCommitTimestamp(timestamp: Instant?)
    fun getLastCommitTimestamp(): Instant?
}

@RequiresApi(Build.VERSION_CODES.O)
class PlantManager(private val repository: PlantStateRepository) {

    // Threshold for wilting: 24 hours without a commit
    @RequiresApi(Build.VERSION_CODES.O)
    private val WILT_THRESHOLD: Duration = Duration.ofDays(1)

    // Initializes the manager with the current plant state from persistence.
    init {
        // Ensure initial state is loaded when manager is created
        val currentState = repository.getPlantState()
        // If no state is saved, default to SEED
        if (currentState == PlantState.SEED && repository.getLastCommitTimestamp() == null) {
            repository.savePlantState(PlantState.SEED)
            repository.saveLastCommitTimestamp(Instant.now()) // Set initial timestamp
        }
    }

    /**
     * Grows the plant to the next stage.
     * This should be called when a new commit is detected.
     */
    fun grow() {
        val currentStage = repository.getPlantState()
        val nextStage = currentStage.nextStage()
        if (nextStage != currentStage) {
            repository.savePlantState(nextStage)
            repository.saveLastCommitTimestamp(Instant.now()) // Update last growth time
            println("Plant grew from $currentStage to $nextStage") // For logging/debug
        } else {
            println("Plant is already a TREE, cannot grow further.")
        }
    }

    /**
     * Wilts the plant to the previous stage.
     * This should be called if no commits are detected within the threshold.
     */
    fun wilt() {
        val currentStage = repository.getPlantState()
        val previousStage = currentStage.previousStage()
        if (previousStage != currentStage) {
            repository.savePlantState(previousStage)
            println("Plant wilted from $currentStage to $previousStage") // For logging/debug
        } else {
            println("Plant is already a SEED, cannot wilt further.")
        }
    }

    /**
     * Checks if the plant should wilt based on the last commit timestamp.
     * This method should be called periodically (e.g., daily via WorkManager).
     */
    fun checkAndAdjustPlantState() {
        val lastCommitTimestamp = repository.getLastCommitTimestamp()
        if (lastCommitTimestamp == null) {
            // No commit recorded yet, plant should stay at SEED or wilt if it's past initial load
            println("No last commit timestamp found. Plant state: ${repository.getPlantState()}")
            return
        }

        val now = Instant.now()
        val timeSinceLastCommit = Duration.between(lastCommitTimestamp, now)

        if (timeSinceLastCommit >= WILT_THRESHOLD) {
            println("Time since last commit (${timeSinceLastCommit.toHours()} hours) exceeds wilt threshold. Wilting plant.")
            wilt()
            // Reset last commit timestamp to 'now' so it doesn't immediately wilt again tomorrow
            repository.saveLastCommitTimestamp(now)
        } else {
            println("Plant is healthy. Time since last commit: ${timeSinceLastCommit.toHours()} hours.")
        }
    }

    // Returns the current state of the plant
    fun getCurrentPlantState(): PlantState {
        return repository.getPlantState()
    }
}