package org.verumomnis.engine

/**
 * Data class representing a normalized sentence from evidence.
 *
 * @param text The cleaned sentence text
 * @param index The position index in the original document
 * @param timestamp Optional timestamp extracted from the sentence (epoch millis)
 */
data class Sentence(
    val text: String,
    val index: Int,
    val timestamp: Long? = null
)
