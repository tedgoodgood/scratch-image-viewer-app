# Frosted Glass Overlay Implementation

## Architecture Overview

The frosted glass overlay feature extends the existing scratch overlay system to include a third overlay type that displays a blurred version of the underlying image.

## Core Components

### 1. OverlayType Enum
```kotlin
enum class OverlayType {
    COLOR,          // Solid color overlays (gold, silver, bronze)
    CUSTOM_IMAGE,    // User-selected custom images
    FROSTED_GLASS    // Blurred version of base image
}
```

### 2. Updated GalleryState
Added `overlayType: OverlayType` field to track current overlay mode, with `COLOR` as default.

### 3. ScratchOverlayView Extensions
- `setFrostedGlassOverlay(uri: Uri?)` - Sets frosted glass mode
- `loadFrostedGlassOverlay(uri: Uri)` - Loads and blurs image
- Blur implementation methods for different API levels
- Blur caching system

## Blur Implementation Strategy

### API Level Detection
```kotlin
val blurredBitmap = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
        applyBlurWithRenderScript(scaledBitmap)
    }
    else -> {
        applyBlurWithStackBlur(scaledBitmap)
    }
}
```

### API 31+ (RenderEffect)
Originally intended to use `RenderEffect.createBlurEffect()`, but due to Canvas integration limitations, falls back to RenderScript for consistency.

### API 17-30 (RenderScript)
```kotlin
val rs = RenderScript.create(context)
val input = Allocation.createFromBitmap(rs, bitmap)
val output = Allocation.createTyped(rs, input.type)
val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

blur.setRadius(BLUR_RADIUS)
blur.setInput(input)
blur.forEach(output)
output.copyTo(bitmap)
```

### API 14-16 (Stack Blur)
Custom implementation of the stack blur algorithm:
- Separable Gaussian blur approximation
- Horizontal pass followed by vertical pass
- Reduced blur radius (10px vs 15px) for performance
- Pure CPU-based processing

## Caching System

### Cache Key Strategy
```kotlin
val cacheKey = "${uri.toString()}_${width}x${height}"
```
Cache key includes:
- Image URI for content identification
- View dimensions for proper scaling

### Cache Management
- `blurCache: MutableMap<String, Bitmap>` stores computed blurs
- Cache cleared when switching overlay types
- Bitmaps recycled to free memory
- Copy-on-write for overlay modifications

## UI Integration

### Layout Updates
Added frosted glass button to overlay selection:
```xml
<ImageButton
    android:id="@+id/frostedGlassButton"
    android:src="@drawable/frosted_glass"
    android:contentDescription="Frosted glass overlay" />
```

### Button Styling
Created `frosted_glass.xml` drawable with gradient background to represent frosted glass appearance.

### State Management
Updated `updateUI()` in MainActivity:
```kotlin
when (state.overlayType) {
    OverlayType.CUSTOM_IMAGE -> { /* custom image logic */ }
    OverlayType.FROSTED_GLASS -> {
        binding.scratchOverlay.setFrostedGlassOverlay(state.currentImage?.uri)
    }
    OverlayType.COLOR -> { /* color logic */ }
}
```

## Performance Considerations

### Blur Radius Optimization
- **API 17+**: 15px radius for better quality
- **API 14-16**: 10px radius for performance
- Adjustable based on device capabilities

### Memory Management
- Original bitmap scaled to view dimensions
- Blurred bitmap cached for reuse
- Cache cleared on overlay type changes
- Bitmap recycling to prevent leaks

### Async Processing
All blur operations run on background threads:
```kotlin
CoroutineScope(Dispatchers.Main).launch {
    val bitmap = withContext(Dispatchers.IO) { /* blur computation */ }
    // UI update on main thread
}
```

## State Persistence

### Saved State
Added overlay type and scratch color to persistence:
```kotlin
savedStateHandle[KEY_OVERLAY_TYPE] = state.overlayType.name
savedStateHandle[KEY_SCRATCH_COLOR] = state.scratchColor
```

### State Restoration
```kotlin
val overlayType = try {
    storedOverlayType?.let { OverlayType.valueOf(it) } ?: OverlayType.COLOR
} catch (e: IllegalArgumentException) {
    OverlayType.COLOR
}
```

## Error Handling

### Graceful Degradation
- RenderScript failures fall back to stack blur
- Stack blur failures fall back to color overlay
- Invalid URIs handled with error states
- Memory pressure triggers cache clearing

### User Feedback
- Loading states during blur computation
- Error messages for unsupported scenarios
- Fallback to color overlay on failures

## Testing Strategy

### Unit Testing
- Blur algorithm correctness with test patterns
- Cache hit/miss scenarios
- State persistence and restoration

### Integration Testing
- Overlay type switching
- Image navigation with blur
- Configuration changes

### Performance Testing
- Blur computation timing
- Memory usage profiling
- Cache effectiveness

## Future Enhancements

### Modern Blur APIs
- Migrate to newer blur APIs when RenderScript is fully removed
- Implement proper RenderEffect integration

### Performance Optimizations
- LRU cache with size limits
- Progressive blur loading
- Background precomputation

### User Customization
- Adjustable blur radius
- Blur intensity controls
- Multiple blur algorithms

## Technical Notes

### RenderScript Deprecation
RenderScript is deprecated in API 31 but remains functional. The implementation includes deprecation warnings but maintains compatibility.

### Stack Blur Algorithm
The custom stack blur implementation provides a good balance of quality and performance for older devices. It approximates Gaussian blur through separable filters.

### Memory Considerations
Large images require significant memory for blur operations. The implementation scales images to view dimensions before processing to minimize memory usage.

### Thread Safety
All bitmap operations are performed on appropriate threads. UI updates happen on main thread, while blur computation runs on background threads.