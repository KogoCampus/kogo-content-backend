package com.kogo.content.search

data class ScoreField(
    val field: String,
    val boost: Double? = null,
    val boostPath: String? = null
)
