//package com.nurat.planttracker
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.nurat.planttracker.ui.theme.PlanttrackerTheme
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            PlanttrackerTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    PlanttrackerTheme {
//        Greeting("Android")
//    }
//}

package com.nurat.planttracker

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.nurat.planttracker.data.PlantStateRepositoryImpl
import com.nurat.planttracker.manager.PlantManager
import com.nurat.planttracker.model.PlantState
import com.nurat.planttracker.service.GitHubService
import com.nurat.planttracker.service.GitHubServiceImpl // Import the real implementation
import com.nurat.planttracker.service.GeminiGenAIService
import com.nurat.planttracker.worker.PlantCheckWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.time.Instant

// IMPORTANT: Replace "YOUR_GEMINI_API_KEY" with your actual Gemini API key.
// Obtain it from Google AI Studio: https://aistudio.google.com/
const val GEMINI_API_KEY = "AIzaSyDDmKXoPoMN7pWlOM6bURmhaP8Sir-zKg4"

// We will get the GitHub username and repo from the user input/storage now.
// Remove the old const val GITHUB_USERNAME and GITHUB_REPO

class MainActivity : AppCompatActivity() {

    private lateinit var plantImageView: ImageView
    private lateinit var plantStateTextView: TextView
    private lateinit var aiSuggestionsTextView: TextView
    private lateinit var githubTokenInput: EditText
    private lateinit var githubUsernameInput: EditText
    private lateinit var saveGithubCredentialsButton: Button
    private lateinit var checkCommitsButton: Button // New button to manually check commits

    private lateinit var plantManager: PlantManager
    private lateinit var plantStateRepository: PlantStateRepositoryImpl
    private lateinit var githubService: GitHubService
    private lateinit var genAIService: GeminiGenAIService

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        plantImageView = findViewById(R.id.plant_image_view)
        plantStateTextView = findViewById(R.id.current_plant_state_text_view)
        aiSuggestionsTextView = findViewById(R.id.ai_suggestions_text_view)
        githubTokenInput = findViewById(R.id.github_token_input)
        githubUsernameInput = findViewById(R.id.github_username_input)
        saveGithubCredentialsButton = findViewById(R.id.save_github_credentials_button)
        checkCommitsButton = findViewById(R.id.check_commits_button) // Link new button

        // Initialize repositories and services
        plantStateRepository = PlantStateRepositoryImpl(applicationContext)
        plantManager = PlantManager(plantStateRepository)
        // Instantiate real GitHubService, providing a lambda to get the token
        githubService = GitHubServiceImpl { plantStateRepository.getGitHubAccessToken() }
        genAIService = GeminiGenAIService(GEMINI_API_KEY)

        // Load saved GitHub credentials if they exist
        githubTokenInput.setText(plantStateRepository.getGitHubAccessToken())
        githubUsernameInput.setText(plantStateRepository.getGitHubUsername())

        // Set up listeners
        saveGithubCredentialsButton.setOnClickListener {
            val token = githubTokenInput.text.toString().trim()
            val username = githubUsernameInput.text.toString().trim()
            if (token.isNotEmpty() && username.isNotEmpty()) {
                plantStateRepository.saveGitHubAccessToken(token)
                plantStateRepository.saveGitHubUsername(username)
                Toast.makeText(this, "GitHub credentials saved!", Toast.LENGTH_SHORT).show()
                // Optionally, immediately try to check commits after saving
                checkAndGrowPlant()
            } else {
                Toast.makeText(this, "Please enter both token and username.", Toast.LENGTH_LONG).show()
            }
        }

        // Manual check commits button (for testing/immediate update)
        checkCommitsButton.setOnClickListener {
            checkAndGrowPlant()
        }

        // Initial UI update
        updatePlantUI()

        // Schedule daily plant state check using WorkManager
        scheduleDailyPlantCheck()
    }

    // Helper function to check commits and grow plant
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndGrowPlant() {
        val username = plantStateRepository.getGitHubUsername()
        val token = plantStateRepository.getGitHubAccessToken()

        if (username.isNullOrEmpty() || token.isNullOrEmpty()) {
            Toast.makeText(this, "Please save your GitHub credentials first.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Checking GitHub commits...", Toast.LENGTH_SHORT).show()
            aiSuggestionsTextView.text = "Checking GitHub for new commits and analyzing..."

            val lastCommitTime = plantStateRepository.getLastCommitTimestamp() ?: Instant.EPOCH
            val newCommits = githubService.getCommitsSince(
                username = username,
                repo = null, // Or provide a specific repo from user input if you add that UI
                since = lastCommitTime
            )

            if (newCommits.isNotEmpty()) {
                plantManager.grow()
                updatePlantUI()
                // Analyze the most recent new commit message
                val latestCommitMessage = newCommits.firstOrNull()
                if (latestCommitMessage != null) {
                    val aiSuggestion = genAIService.analyzeCommitMessage(latestCommitMessage)
                    aiSuggestionsTextView.text = "AI Feedback for '$latestCommitMessage':\n$aiSuggestion"
                } else {
                    aiSuggestionsTextView.text = "New commits detected, but no message to analyze."
                }
                Toast.makeText(this@MainActivity, "Plant grew!", Toast.LENGTH_SHORT).show()
            } else {
                aiSuggestionsTextView.text = "No new commits detected since last check to grow the plant."
                Toast.makeText(this@MainActivity, "No new commits.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePlantUI() {
        val currentState = plantManager.getCurrentPlantState()
        plantStateTextView.text = "Plant Stage: ${currentState.name}"
        when (currentState) {
            PlantState.SEED -> plantImageView.setImageResource(R.drawable.plant_seed)
            PlantState.SAPLING -> plantImageView.setImageResource(R.drawable.plant_sapling)
            PlantState.PLANT -> plantImageView.setImageResource(R.drawable.plant_full)
            PlantState.TREE -> plantImageView.setImageResource(R.drawable.plant_tree)
        }
        // Ensure you have these drawable resources (e.g., plant_seed.png, plant_sapling.png, etc.)
        // in your app/src/main/res/drawable folder.
    }

    // WorkManager setup
    private fun scheduleDailyPlantCheck() {
        val plantCheckWorkRequest = PeriodicWorkRequest.Builder(
            PlantCheckWorker::class.java,
            1, TimeUnit.DAYS // Run every 24 hours
        )
            .addTag("plant_check_work")
            // You might want to set constraints like requiring network for this worker
            // .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_plant_check",
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            plantCheckWorkRequest
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // When the app comes to foreground, update UI based on current state
        updatePlantUI()
        // Also trigger a check for wilting/growth, in case WorkManager hasn't run yet
        // or commits happened while the app was closed.
        checkAndGrowPlant() // This will also handle wilting in PlantManager
    }
}
