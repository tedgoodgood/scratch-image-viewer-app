package com.example.composeapp.domain

/**
 * Represents the different types of overlays that can be applied to the scratch view.
 */
enum class OverlayType {
    /**
     * A solid color overlay (gold, silver, bronze, etc.)
     */
    COLOR,
    
    /**
     * A custom image overlay selected by the user
     */
    CUSTOM_IMAGE,
    
    /**
     * A frosted glass effect that blurs the underlying image
     */
    FROSTED_GLASS
}