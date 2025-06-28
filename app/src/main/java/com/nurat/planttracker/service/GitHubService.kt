package com.nurat.planttracker.service

import java.time.Instant

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

// Dummy implementation for demonstration and testing purposes
