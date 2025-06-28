package com.nurat.planttracker.service

import java.time.Instant

class MockGitHubService : GitHubService {
    private val commits = mutableListOf<String>()

    fun addMockCommit(message: String) {
        commits.add(message)
    }

    override suspend fun getCommitsSince(username: String, repo: String?, since: Instant): List<String> {
        // In a real scenario, this would make an actual network request to GitHub API.
        // For this mock, we just return the predefined commits.
        // You'd also filter by 'since' timestamp here.
        println("MockGitHubService: Fetching commits for $username since $since")
        return commits.filter { true } // Simplified: in real app, filter by actual commit date
    }
}