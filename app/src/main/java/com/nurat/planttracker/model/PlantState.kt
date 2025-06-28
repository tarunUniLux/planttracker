package com.nurat.planttracker.model

enum class PlantState {
    SEED,
    SAPLING,
    PLANT,
    TREE;

    // Helper function to get the next stage for growth
    fun nextStage(): PlantState {
        return when (this) {
            SEED -> SAPLING
            SAPLING -> PLANT
            PLANT -> TREE
            TREE -> TREE // Plant cannot grow beyond tree
        }
    }

    // Helper function to get the previous stage for wilting
    fun previousStage(): PlantState {
        return when (this) {
            TREE -> PLANT
            PLANT -> SAPLING
            SAPLING -> SEED
            SEED -> SEED // Plant cannot wilt below seed
        }
    }
}