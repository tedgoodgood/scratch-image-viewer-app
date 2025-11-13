# Fix: Scratch Marks Persisting Across Images

## Problem
When users scratched image 1 and then navigated to image 2, the scratch marks from image 1 would appear on image 2, 3, and subsequent images. This persisted until around image 4, when the marks would finally disappear.

## Root Cause
The issue was in the image navigation flow:

1. When user scratched an image, the scratch marks were drawn directly onto the `overlayBitmap` in `ScratchOverlayView` during touch events
2. When navigating to a new image:
   - The `GalleryViewModel` correctly cleared the `scratchSegments` list
   - The underlay image was updated to show the new image
   - **BUT**: The `overlayBitmap` in `ScratchOverlayView` still contained the old scratch marks

The overlay bitmap was not being reset when switching images, so old scratches remained visible on top of new images.

## Solution
Added an explicit call to `resetOverlay()` when the image URI changes in `MainActivity.kt`:

```kotlin
if (targetUnderlayUri != currentImageUri) {
    updateUnderlayImage(targetUnderlayUri)
    // Reset overlay when image changes to clear old scratches
    binding.scratchOverlay.resetOverlay()
}
```

This ensures that:
1. When navigating to a new image (via next/previous buttons)
2. The overlay bitmap is completely reset with a fresh, clean overlay
3. Old scratch marks are removed before displaying the new image

## What `resetOverlay()` Does
- Clears the `scratchSegments` list
- Resets the `scratchPath`
- Redraws the overlay with the base color (no scratches)
- Triggers a view invalidation to update the display

## Testing Scenarios

### Scenario 1: Navigate between images
1. Import 4+ images
2. Scratch image 1
3. Click "Next" to go to image 2
4. ✅ Expected: Image 2 shows clean overlay (no scratches from image 1)
5. Click "Next" to go to image 3
6. ✅ Expected: Image 3 shows clean overlay (no scratches)

### Scenario 2: Navigate back and forth
1. Import images
2. Scratch image 1
3. Go to image 2 (clean)
4. Scratch image 2
5. Go back to image 1 (clean, no scratches from step 2 or step 5)
6. ✅ Expected: Each image starts with a fresh overlay when navigated to

### Scenario 3: Fullscreen navigation
1. Scratch image 1
2. Enter fullscreen mode
3. Use fullscreen next button to go to image 2
4. ✅ Expected: Image 2 shows clean overlay

### Scenario 4: Color changes
1. Scratch image 1 with red overlay
2. Change overlay color to blue
3. Navigate to image 2
4. ✅ Expected: Image 2 shows clean blue overlay (no scratches)

## Files Modified
- `app/src/main/java/com/example/composeapp/MainActivity.kt`
  - Added `resetOverlay()` call when image URI changes (line 247)

## Impact
- Fixes the bug where scratch marks persisted across images
- Ensures each image starts with a clean overlay
- No performance impact (reset is fast)
- Maintains all existing functionality
