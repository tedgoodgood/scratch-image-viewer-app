package com.example.composeapp.util

import java.util.Locale

class NaturalSortComparator : Comparator<String> {
    private val segmentRegex = Regex("\\d+|\\D+")

    override fun compare(a: String, b: String): Int {
        val aSegments = segmentRegex.findAll(a.lowercase(Locale.getDefault())).map { it.value }.toList()
        val bSegments = segmentRegex.findAll(b.lowercase(Locale.getDefault())).map { it.value }.toList()

        val minSize = minOf(aSegments.size, bSegments.size)
        for (index in 0 until minSize) {
            val aSegment = aSegments[index]
            val bSegment = bSegments[index]

            val aIsNumber = aSegment.firstOrNull()?.isDigit() == true
            val bIsNumber = bSegment.firstOrNull()?.isDigit() == true

            val result = when {
                aIsNumber && bIsNumber -> compareNumericSegments(aSegment, bSegment)
                else -> aSegment.compareTo(bSegment, ignoreCase = true)
            }

            if (result != 0) {
                return result
            }
        }

        if (aSegments.size != bSegments.size) {
            return aSegments.size.compareTo(bSegments.size)
        }

        return a.compareTo(b, ignoreCase = true)
    }

    private fun compareNumericSegments(a: String, b: String): Int {
        val trimmedA = a.trimStart('0')
        val trimmedB = b.trimStart('0')
        val normalizedA = if (trimmedA.isEmpty()) "0" else trimmedA
        val normalizedB = if (trimmedB.isEmpty()) "0" else trimmedB

        val lengthComparison = normalizedA.length.compareTo(normalizedB.length)
        if (lengthComparison != 0) {
            return lengthComparison
        }

        val valueComparison = normalizedA.compareTo(normalizedB)
        if (valueComparison != 0) {
            return valueComparison
        }

        return a.length.compareTo(b.length)
    }
}
