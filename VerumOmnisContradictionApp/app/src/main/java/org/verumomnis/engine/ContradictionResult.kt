package org.verumomnis.engine

data class ContradictionResult(
    val a: Sentence,
    val b: Sentence,
    val reason: String
)
