package com.nurat.planttracker.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.nurat.planttracker.manager.PlantStateRepository
import com.nurat.planttracker.model.PlantState
import java.time.Instant
import java.time.format.DateTimeParseException
import androidx.core.content.edit

class PlantStateRepositoryImpl(private val context: Context) : PlantStateRepository {

    // Regular SharedPreferences for non-sensitive data
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("plant_tracker_prefs", Context.MODE_PRIVATE)

    // Encrypted SharedPreferences for sensitive data (like GitHub token)
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val encryptedSharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "github_auth_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun savePlantState(state: PlantState) {
        sharedPreferences.edit { putString("plant_state", state.name) }
    }

    override fun getPlantState(): PlantState {
        val stateName = sharedPreferences.getString("plant_state", PlantState.SEED.name)
        return try {
            PlantState.valueOf(stateName ?: PlantState.SEED.name)
        } catch (e: IllegalArgumentException) {
            PlantState.SEED // Default to SEED if parsing fails
        }
    }

    override fun saveLastCommitTimestamp(timestamp: Instant?) {
        sharedPreferences.edit { putString("last_commit_timestamp", timestamp.toString()) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getLastCommitTimestamp(): Instant? {
        val timestampString = sharedPreferences.getString("last_commit_timestamp", null)
        return try {
            timestampString?.let { Instant.parse(it) }
        } catch (e: DateTimeParseException) {
            null // Return null if parsing fails
        }
    }

    // New methods to save/retrieve GitHub access token using encryptedSharedPreferences
    fun saveGitHubAccessToken(token: String) {
        encryptedSharedPreferences.edit { putString("github_access_token", token) }
    }

    fun getGitHubAccessToken(): String? {
        return encryptedSharedPreferences.getString("github_access_token", null)
    }

    // New method to save/retrieve GitHub username
    fun saveGitHubUsername(username: String) {
        sharedPreferences.edit { putString("github_username", username) }
    }

    fun getGitHubUsername(): String? {
        return sharedPreferences.getString("github_username", null)
    }
}