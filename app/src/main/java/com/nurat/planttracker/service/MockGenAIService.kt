package com.nurat.planttracker.service

// Dummy implementation for demonstration and testing purposes
class MockGenAIService : GenAIService {
    override suspend fun analyzeCommitMessage(commitMessage: String): String {
        return "AI Suggestion for '$commitMessage': Consider adding unit tests and improving variable names."
    }
}