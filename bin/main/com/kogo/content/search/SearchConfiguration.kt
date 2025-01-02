package com.kogo.content.search

import org.bson.Document
import java.util.Date

data class SearchConfiguration(
    /**
     * List of fields to perform text search on.
     * These fields will be searched for the query text with equal weight.
     * Example: ["title", "content"] will search both title and content fields
     */
    val textSearchFields: List<String>,

    /**
     * List of date fields to boost scores based on recency.
     * Documents closer to the current date will receive higher scores.
     * Example: NearField("createdAt", "7d", 1.5) will boost documents created within 7 days by 1.5x
     */
    val nearFields: List<NearField<*>> = emptyList(),

    /**
     * List of fields to apply additional score boosting.
     * Matches in these fields will receive higher scores based on the boost value.
     * Example: ScoreField("title", boost = 2.0) will make title matches twice as important
     */
    val scoreFields: List<ScoreField> = emptyList(),

    /**
     * Maximum number of character edits allowed for fuzzy matching.
     * Higher values allow more typo tolerance but may reduce precision.
     * Example: 1 allows single-character typos like "helllo" matching "hello"
     */
    val fuzzyMaxEdits: Int = 1,
)

/**
 * Represents a proximity-based search configuration for numeric, date, or geographic fields
 */
sealed class NearField<T> {
    abstract val field: String
    abstract val origin: T
    abstract val pivot: Long
    abstract val score: Score?
}

/**
 * Date-based proximity search
 * @param field The date field to search
 * @param origin Reference date to calculate proximity from
 * @param pivot Time window in milliseconds (e.g., 86400000 for 1 day)
 * @param boost Optional score multiplier
 */
data class DateNearField(
    override val field: String,
    override val origin: Date,
    override val pivot: Long,  // milliseconds
    override val score: Score? = null
) : NearField<Date>() {
    companion object {
        const val ONE_DAY_MS: Long = 86_400_000
        const val ONE_WEEK_MS: Long = 7 * ONE_DAY_MS
        const val ONE_MONTH_MS: Long = 30 * ONE_DAY_MS
    }
}

/**
 * Numeric proximity search
 * @param field The numeric field to search
 * @param origin Reference number to calculate proximity from
 * @param pivot Numeric distance that results in a 0.5 score
 * @param boost Optional score multiplier
 */
data class NumericNearField(
    override val field: String,
    override val origin: Number,
    override val pivot: Long,
    override val score: Score? = null
) : NearField<Number>()

/**
 * Geographic proximity search
 * @param field The geo field to search
 * @param origin Reference point as [GeoPoint]
 * @param pivot Distance in meters that results in a 0.5 score
 * @param boost Optional score multiplier
 */
data class GeoNearField(
    override val field: String,
    override val origin: GeoPoint,
    override val pivot: Long,  // meters
    override val score: Score? = null
) : NearField<GeoPoint>()

/**
 * Represents a geographic point using GeoJSON format
 */
data class GeoPoint(
    val longitude: Double,
    val latitude: Double
) {
    fun toDocument() = Document().apply {
        put("type", "Point")
        put("coordinates", listOf(longitude, latitude))
    }
}

/**
 * Represents scoring options for Atlas Search
 */
sealed class Score {
    /**
     * Multiply the result score by the given number
     */
    data class Boost(val value: Double) : Score()

    /**
     * Replace the result score with the given number
     */
    data class Constant(val value: Double) : Score()

    /**
     * Replace the result score with the value from the given field path
     */
    data class Path(val path: String) : Score()

    /**
     * Replace the result score with a custom expression
     */
    data class Function(val expression: Document) : Score()
}

data class ScoreField(
    val field: String,
    val score: Score
)
