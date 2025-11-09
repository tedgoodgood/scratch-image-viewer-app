# Two-Layer Image System Implementation Test Checklist

## Overview
This checklist verifies that the black background issue has been fixed by implementing a proper two-layer rendering system.

## Critical Fixes Implemented

### 1. GalleryState Updates
- [x] Renamed `baseImageUri` to `underlayImageUri` for clarity
- [x] Updated all references in ViewModel and persistence logic
- [x] Ensured proper field naming throughout the codebase

### 2. ViewModel Fixes
- [x] Updated `selectUnderlayImage()` to use `underlayImageUri`
- [x] Fixed state persistence to use new field name
- [x] Updated state restoration logic
- [x] Maintained backward compatibility

### 3. MainActivity Logic Improvements
- [x] Simplified underlay image selection logic
- [x] Fixed base image update timing and conditions
- [x] Added comprehensive logging for debugging
- [x] Improved error handling in bitmap loading
- [x] Added default underlay image initialization
- [x] Enhanced bitmap scaling logic

### 4. ScratchOverlayView Rendering Fixes
- [x] Enhanced `onDraw()` with step-by-step rendering
- [x] Added comprehensive logging for debugging
- [x] Improved overlay bitmap creation and management
- [x] Fixed size change handling
- [x] Enhanced scratch path rendering
- [x] Added fallback for missing underlay (gray instead of black)

## Expected Behavior

### Color Overlay Mode
1. **Initial State**: Color overlay (98% opacity) over underlay image
2. **Scratching**: Reveals underlay image (NOT black)
3. **Reset**: Clears all scratches immediately

### Custom Image Overlay Mode
1. **Selection**: User selects overlay image via "Custom Overlay" button
2. **Rendering**: Custom image displays over underlay image
3. **Scratching**: Reveals underlay image (NOT black)
4. **Reset**: Clears all scratches immediately

### Frosted Glass Mode
1. **Activation**: User taps "Frosted Glass" button
2. **Rendering**: Blurred version of underlay image displays
3. **Scratching**: Reveals clear underlay image (NOT black)
4. **Reset**: Clears all scratches immediately

## Test Scenarios

### Test 1: Fresh App Launch
1. Launch app
2. Verify color overlay is active (gold color visible)
3. Scratch the overlay
4. **Expected**: Should see default image underlay (NOT black)
5. Tap Reset button
6. **Expected**: Scratches clear immediately

### Test 2: Custom Image Overlay
1. Tap "Select Underlay" and choose an image
2. Tap "Custom Overlay" button and choose a different image
3. Verify overlay shows the selected image
4. Scratch the overlay
5. **Expected**: Should see the underlay image (NOT black)
6. Tap Reset button
7. **Expected**: Scratches clear immediately

### Test 3: Frosted Glass
1. Tap "Select Underlay" and choose an image (or use current gallery image)
2. Tap "Frosted Glass" button
3. Verify blurred effect is visible
4. Scratch the overlay
5. **Expected**: Should see the clear underlay image (NOT black)
6. Tap Reset button
7. **Expected**: Scratches clear immediately

### Test 4: Gallery Navigation
1. Load multiple images via "Select Images" or "Select Folder"
2. Navigate between images
3. **Expected**: Underlay should update to current image if no explicit underlay set
4. Test scratching on different images
5. **Expected**: Each image should be revealed when scratched

### Test 5: Folder Import
1. Tap "Select Folder"
2. Choose a folder with images
3. **Expected**: All images should load and be visible in gallery
4. Navigate through images
5. **Expected**: Underlay should update to current image

## Debug Logging

Key log tags to monitor:
- `MainActivity:D` - Base image updates and UI state changes
- `ScratchOverlayView:D` - Rendering operations and overlay management
- `GalleryViewModel` - State changes and persistence

Sample expected log output:
```
D/MainActivity: Underlay update: type=COLOR, underlayUri=null, currentUri=content://..., target=content://...
D/MainActivity: Updating underlay image to: content://...
D/MainActivity: Underlay bitmap loaded successfully: 1080x1920
D/ScratchOverlayView: onDraw: baseImage=true, overlayBitmap=true, scratches=5
D/ScratchOverlayView: Drew underlay image: 1080x1920
D/ScratchOverlayView: Drew overlay bitmap: 1080x1920
```

## UI Elements Verification

### Button Functionality
- [x] "Select Underlay" - opens image picker for underlay selection
- [x] "Custom Overlay" - opens image picker for overlay selection
- [x] Color buttons (Gold, Silver, Bronze) - activate color overlay
- [x] "Frosted Glass" - activates frosted glass effect
- [x] "Reset" - clears all scratches

### Layout Verification
- [x] Bottom controls not hidden by navigation bar
- [x] All buttons accessible and properly spaced
- [x] Fullscreen controls work correctly
- [x] Image counter updates correctly

## Performance Considerations

### Bitmap Management
- [x] Proper bitmap recycling when not needed
- [x] Efficient scaling to view dimensions
- [x] Background thread loading for images
- [x] Blur caching for frosted glass effect

### Memory Management
- [x] Clear overlay bitmaps when changing modes
- [x] Recycle bitmaps in size changes
- [x] Proper error handling for OOM scenarios

## Edge Cases

### Error Handling
- [x] Invalid image URIs handled gracefully
- [x] Network failures for remote images
- [x] Corrupted image files
- [x] Permission denied scenarios

### State Management
- [x] App restart maintains overlay type
- [x] Screen rotation preserves state
- [x] Background/foreground transitions work
- [x] Memory pressure handling

## Success Criteria

### Must Pass
1. **No Black Background**: All scratching reveals an image, never black
2. **Proper Layer Order**: Underlay → Overlay → Scratches (CLEAR mode)
3. **Immediate Reset**: Reset button clears scratches instantly
4. **All Overlay Types Work**: Color, Custom Image, Frosted Glass
5. **Folder Import**: Successfully loads and displays images
6. **UI Responsiveness**: All buttons work and are visible

### Should Pass
1. **Performance**: Smooth scratching without lag
2. **Memory Usage**: No memory leaks or OOM crashes
3. **Error Recovery**: Graceful handling of edge cases
4. **Debugging**: Comprehensive logging for troubleshooting

## Implementation Notes

### Key Changes Made
1. **Field Renaming**: `baseImageUri` → `underlayImageUri` for clarity
2. **Rendering Logic**: Step-by-step canvas drawing with logging
3. **State Management**: Simplified underlay selection logic
4. **Error Handling**: Comprehensive error catching and logging
5. **Default Behavior**: Always ensure an underlay image is available

### Technical Details
- Canvas rendering follows strict layer order
- Bitmap scaling preserves aspect ratio
- Scratch paths use PorterDuff.Mode.CLEAR
- Underlay defaults to current gallery image
- Gray fallback prevents black background

This implementation should completely eliminate the black background issue and provide a robust two-layer image system for all overlay types.