package com.nurat.planttracker.manager

import com.nurat.planttracker.model.PlantState
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.temporal.ChronoUnit

// Mock implementation of PlantStateRepository for testing purposes
class MockPlantStateRepository : PlantStateRepository {
    private var plantState: PlantState = PlantState.SEED
    private var lastCommitTimestamp: Instant? = null

    override fun savePlantState(state: PlantState) {
        this.plantState = state
    }

    override fun getPlantState(): PlantState {
        return plantState
    }

    override fun saveLastCommitTimestamp(timestamp: Instant?) {
        this.lastCommitTimestamp = timestamp
    }

    override fun getLastCommitTimestamp(): Instant? {
        return lastCommitTimestamp
    }
}

class PlantManagerTest {

    private lateinit var mockRepository: MockPlantStateRepository
    private lateinit var plantManager: PlantManager

    @Before
    fun setUp() {
        mockRepository = MockPlantStateRepository()
        plantManager = PlantManager(mockRepository)
        // Reset state before each test
        mockRepository.savePlantState(PlantState.SEED)
        mockRepository.saveLastCommitTimestamp(Instant.now())
    }

    @Test
    fun `grow should advance plant state from SEED to SAPLING`() {
        mockRepository.savePlantState(PlantState.SEED)
        plantManager.grow()
        assertEquals(PlantState.SAPLING, mockRepository.getPlantState())
    }

    @Test
    fun `grow should advance plant state from SAPLING to PLANT`() {
        mockRepository.savePlantState(PlantState.SAPLING)
        plantManager.grow()
        assertEquals(PlantState.PLANT, mockRepository.getPlantState())
    }

    @Test
    fun `grow should advance plant state from PLANT to TREE`() {
        mockRepository.savePlantState(PlantState.PLANT)
        plantManager.grow()
        assertEquals(PlantState.TREE, mockRepository.getPlantState())
    }

    @Test
    fun `grow should not advance plant state beyond TREE`() {
        mockRepository.savePlantState(PlantState.TREE)
        plantManager.grow()
        assertEquals(PlantState.TREE, mockRepository.getPlantState())
    }

    @Test
    fun `wilt should reduce plant state from TREE to PLANT`() {
        mockRepository.savePlantState(PlantState.TREE)
        plantManager.wilt()
        assertEquals(PlantState.PLANT, mockRepository.getPlantState())
    }

    @Test
    fun `wilt should reduce plant state from PLANT to SAPLING`() {
        mockRepository.savePlantState(PlantState.PLANT)
        plantManager.wilt()
        assertEquals(PlantState.SAPLING, mockRepository.getPlantState())
    }

    @Test
    fun `wilt should reduce plant state from SAPLING to SEED`() {
        mockRepository.savePlantState(PlantState.SAPLING)
        plantManager.wilt()
        assertEquals(PlantState.SEED, mockRepository.getPlantState())
    }

    @Test
    fun `wilt should not reduce plant state below SEED`() {
        mockRepository.savePlantState(PlantState.SEED)
        plantManager.wilt()
        assertEquals(PlantState.SEED, mockRepository.getPlantState())
    }

    @Test
    fun `checkAndAdjustPlantState should wilt plant if last commit was more than 24 hours ago`() {
        mockRepository.savePlantState(PlantState.SAPLING)
        // Set last commit timestamp to be 25 hours ago
        mockRepository.saveLastCommitTimestamp(Instant.now().minus(25, ChronoUnit.HOURS))

        plantManager.checkAndAdjustPlantState()
        assertEquals(PlantState.SEED, mockRepository.getPlantState())
    }

    @Test
    fun `checkAndAdjustPlantState should not wilt plant if last commit was less than 24 hours ago`() {
        mockRepository.savePlantState(PlantState.SAPLING)
        // Set last commit timestamp to be 23 hours ago
        mockRepository.saveLastCommitTimestamp(Instant.now().minus(23, ChronoUnit.HOURS))

        plantManager.checkAndAdjustPlantState()
        assertEquals(PlantState.SAPLING, mockRepository.getPlantState()) // Should remain SAPLING
    }

    @Test
    fun `checkAndAdjustPlantState should update lastCommitTimestamp after wilting`() {
        mockRepository.savePlantState(PlantState.SAPLING)
        val initialTimestamp = Instant.now().minus(25, ChronoUnit.HOURS)
        mockRepository.saveLastCommitTimestamp(initialTimestamp)

        plantManager.checkAndAdjustPlantState()
        assertNotEquals(initialTimestamp, mockRepository.getLastCommitTimestamp())
        // The new timestamp should be close to 'now' when wilted
        assertTrue(mockRepository.getLastCommitTimestamp()!!.isAfter(Instant.now().minus(1, ChronoUnit.MINUTES)))
    }

    @Test
    fun `grow should update last commit timestamp`() {
        val initialTimestamp = mockRepository.getLastCommitTimestamp()
        plantManager.grow()
        assertNotNull(mockRepository.getLastCommitTimestamp())
        assertNotEquals(initialTimestamp, mockRepository.getLastCommitTimestamp())
    }

    @Test
    fun `manager initializes plant state to SEED if no state exists`() {
        val newMockRepository = MockPlantStateRepository() // Fresh repository
        newMockRepository.savePlantState(PlantState.SEED) // Ensure default state is SEED
        newMockRepository.saveLastCommitTimestamp(null) // No timestamp saved

        val newPlantManager = PlantManager(newMockRepository)
        assertEquals(PlantState.SEED, newMockRepository.getPlantState())
        assertNotNull(newMockRepository.getLastCommitTimestamp()) // Should set an initial timestamp
    }
}
