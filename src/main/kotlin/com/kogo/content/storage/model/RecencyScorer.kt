package com.kogo.content.storage.model

import org.bson.Document
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.math.exp

@Component
class RecencyScorer(
    private val maxValue: Double = 1.0,  // c: controls the maximum value
    private val steepness: Double = 0.5, // a: controls the steepness of the transition
    private val threshold: Duration = Duration.ofDays(7) // s: threshold where transition occurs
) {
    /**
     * Calculates a recency score using a sigmoid function
     * f(x) = c * (1 - 1/(1 + e^(-a(x-s))))
     *
     * @param timestamp The timestamp to calculate recency for
     * @param currentTime The current time (defaults to now)
     * @return A score between 0 and maxValue
     */
    fun calculateScore(timestamp: Instant, currentTime: Instant = Instant.now()): Double {
        val ageInSeconds = Duration.between(timestamp, currentTime).seconds
        val thresholdInSeconds = threshold.seconds

        val x = (ageInSeconds - thresholdInSeconds).toDouble()
        return maxValue * (1.0 - 1.0 / (1.0 + exp(-steepness * x)))
    }

    /**
     * Creates a MongoDB Document representing the sigmoid function for use in aggregation pipelines
     * The document expects an "ageInSeconds" field to be present in the pipeline
     *
     * @return Document representing the sigmoid function calculation
     */
    fun toAggregationDocument(): Document {
        return Document("\$multiply", listOf(
            maxValue,
            Document("\$subtract", listOf(
                1.0,
                Document("\$divide", listOf(
                    1.0,
                    Document("\$add", listOf(
                        1.0,
                        Document("\$exp", Document("\$multiply", listOf(
                            -steepness,
                            Document("\$subtract", listOf(
                                "\$ageInSeconds",
                                threshold.seconds
                            ))
                        )))
                    ))
                ))
            ))
        ))
    }

    /**
     * Creates a list of Documents for the complete recency calculation pipeline,
     * including the age calculation and the sigmoid function
     *
     * @param timestampField The name of the field containing the timestamp to calculate recency from
     * @param currentTime The reference time for age calculation (defaults to now)
     * @return List of Documents representing the complete recency calculation pipeline
     */
    fun toAggregationPipeline(
        timestampField: String = "createdAt",
        currentTime: Instant = Instant.now()
    ): List<Document> {
        return listOf(
            // Calculate age in seconds
            Document("\$addFields", Document("ageInSeconds",
                Document("\$divide", listOf(
                    Document("\$subtract", listOf(
                        currentTime.toEpochMilli(),
                        Document("\$toLong", "\$$timestampField")
                    )),
                    1000 // Convert milliseconds to seconds
                ))
            )),
            // Apply sigmoid function
            Document("\$addFields", Document("recencyScore", toAggregationDocument()))
        )
    }

    companion object {
        fun create(
            maxValue: Double = 1.0,
            steepness: Double = 0.5,
            threshold: Duration = Duration.ofDays(7)
        ): RecencyScorer = RecencyScorer(maxValue, steepness, threshold)
    }
}
