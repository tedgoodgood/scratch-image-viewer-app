# Two-Layer Rendering System Implementation Test Checklist

## Overview
This document provides a comprehensive test checklist for the two-layer rendering system implementation that fixes the fundamental black background issue in Custom Image Overlay and Frosted Glass modes.

## Key Changes Implemented

### 1. GalleryState Field Renaming
- **Before**: `baseImageUri` 
- **After**: `underlayImageUri` (clearer naming)
- **Purpose**: The image revealed when scratching (underlay layer)

### 2. Proper Two-Layer Rendering System
- **Layer 1 (Bottom)**: Underlay image - what shows when scratching
- **Layer 2 (Top)**: Overlay - color/custom image/frosted glass that gets scratched away
- **Rendering Order**: Underlay → Overlay → Scratch paths (CLEAR mode)

### 3. UI Button Improvements
- **Clear Naming**: "Select Underlay" button for underlay image selection
- **Bottom Padding**: Added 32dp bottom padding to prevent navigation bar overlap
- **Proper State Management**: Separate tracking for underlay vs overlay images

### 4. Enhanced Logging
- Added comprehensive debug logging in `ScratchOverlayView.onDraw()`
- Added logging in `MainActivity.updateUnderlayImage()`
- Better error handling and state tracking

## Test Scenarios

### ✅ Basic Functionality Tests

#### 1. App Startup
- [ ] App launches successfully
- [ ] Default image loads (Golden Fortune)
- [ ] Controls are visible and accessible
- [ ] No buttons hidden by navigation bar

#### 2. Color Overlay Mode
- [ ] Tap any color button (Gold/Silver/Bronze)
- [ ] Color overlay appears with 98% opacity
- [ ] Scratch reveals underlay image (not black)
- [ ] Reset button clears scratches immediately
- [ ] Debug logs show: "Drew base image bitmap as underlay"

#### 3. Custom Image Overlay Mode
- [ ] Tap "Select Underlay" button
- [ ] Select an image for underlay
- [ ] Tap gallery button (custom overlay)
- [ ] Select different image for overlay
- [ ] Overlay image appears on top
- [ ] Scratch reveals underlay image (not black)
- [ ] Reset button clears scratches immediately

#### 4. Frosted Glass Mode
- [ ] Tap "Select Underlay" button
- [ ] Select an image for underlay
- [ ] Tap frosted glass button
- [ ] Blurred overlay appears
- [ ] Scratch reveals underlay image (not black)
- [ ] Blur effect is visible and strong
- [ ] Reset button clears scratches immediately

### ✅ Navigation Tests

#### 5. Image Navigation
- [ ] Previous/Next buttons work correctly
- [ ] Image counter updates properly
- [ ] Scratches clear when navigating between images
- [ ] Underlay image persists across navigation (if set)

#### 6. Fullscreen Mode
- [ ] Fullscreen button toggles correctly
- [ ] Navigation controls appear in fullscreen
- [ ] Exit fullscreen works
- [ ] Overlay functionality works in fullscreen

### ✅ Import Tests

#### 7. Single Image Selection
- [ ] "Select Images" button opens file picker
- [ ] Multiple images can be selected
- [ ] Images load into gallery
- [ ] Navigation works with imported images

#### 8. Folder Import
- [ ] "Select Folder" button opens folder picker
- [ ] Folder enumeration works on API 21+
- [ ] Folder enumeration works on API 14-20 (fallback)
- [ ] Images from folder load into gallery
- [ ] Performance warning for large folders (>1000 images)
- [ ] Empty folder shows appropriate error message

### ✅ Edge Cases

#### 9. No Underlay Set
- [ ] Color overlay works without explicit underlay
- [ ] Custom overlay shows warning if no underlay
- [ ] Frosted glass shows warning if no underlay
- [ ] Debug logs show warning: "No underlay image available"

#### 10. Error Handling
- [ ] Invalid image selection handled gracefully
- [ ] Permission denied handled gracefully
- [ ] File system errors handled gracefully
- [ ] App doesn't crash on errors

### ✅ Performance Tests

#### 11. Memory Management
- [ ] Bitmap recycling works properly
- [ ] No memory leaks during navigation
- [ ] Blur caching works for frosted glass
- [ ] Large images don't cause OOM

#### 12. Rendering Performance
- [ ] Scratching is smooth and responsive
- [ ] No lag during overlay changes
- [ ] Frame rate remains stable

## Debug Logging Usage

### Enable Debug Logs
```bash
adb logcat -s MainActivity:D ScratchOverlayView:D
```

### Key Log Messages to Watch For

#### Successful Underlay Loading
```
MainActivity: Underlay image loaded and set successfully
ScratchOverlayView: Drew base image bitmap as underlay
ScratchOverlayView: onDraw complete: underlayDrawn=true, overlayBitmap=true, scratches=N
```

#### Missing Underlay Warning
```
ScratchOverlayView: No underlay image available - this will cause black background when scratching!
ScratchOverlayView: onDraw complete: underlayDrawn=false, overlayBitmap=true, scratches=N
```

#### Overlay State Changes
```
MainActivity: Overlay type changed: CUSTOM_IMAGE, URI: content://...
MainActivity: Setting custom overlay with URI: content://...
MainActivity: Setting frosted glass overlay with URI: content://...
MainActivity: Setting color overlay: -13421825
```

## Expected Behavior Summary

### ✅ What Should Work
1. **Color Overlay**: 98% opacity color, scratch reveals underlay
2. **Custom Image**: Selected overlay image, scratch reveals underlay  
3. **Frosted Glass**: Blurred underlay, scratch reveals sharp underlay
4. **Reset**: Immediate scratch clearing for all modes
5. **Navigation**: Proper state management across images
6. **Import**: Both single images and folders work

### ❌ What Should NOT Happen
1. **Black Background**: Never see black when scratching
2. **Button Overlap**: All buttons visible and accessible
3. **Crashes**: Graceful error handling throughout
4. **Confusion**: Clear button naming and functionality

## Technical Implementation Details

### Two-Layer Rendering Flow
```
1. ScratchOverlayView.onDraw() called
2. Draw underlay image (baseImageBitmap) first
3. Draw overlay bitmap on top (overlayBitmap)
4. Scratch paths use PorterDuff.Mode.CLEAR
5. Result: Transparent areas show underlay below
```

### State Management Flow
```
1. User selects underlay → underlayImageUri set
2. User selects overlay type → overlayType set
3. MainActivity updates underlay bitmap → setBaseImage()
4. MainActivity updates overlay bitmap → setCustomOverlay()/setFrostedGlassOverlay()
5. ScratchOverlayView renders two layers
```

### Error Recovery
```
1. No underlay → Warning logged, no crash
2. Invalid image → Fallback to previous state
3. Permission denied → Error message shown
4. Memory pressure → Bitmap recycling, cache clearing
```

## Test Data Recommendations

### Test Images
- **Small**: < 1MB (fast loading)
- **Medium**: 1-5MB (typical phone photos)
- **Large**: > 5MB (stress test)
- **Different formats**: JPEG, PNG, WebP

### Test Folders
- **Empty**: 0 images (error handling)
- **Small**: 10-50 images (normal use)
- **Large**: 1000+ images (performance warning)
- **Nested**: Subfolders (recursive traversal)

## API Level Testing

### API 14-16 (Legacy)
- [ ] Stack blur works correctly
- [ ] MediaStore fallback works
- [ ] Performance acceptable

### API 17-30 (RenderScript)
- [ ] RenderScript blur works
- [ ] DocumentFile API works
- [ ] Permission handling works

### API 31+ (Modern)
- [ ] RenderScript fallback works
- [ ] Modern APIs used where available
- [ ] No deprecated API warnings

## Conclusion

This implementation provides a robust two-layer rendering system that eliminates the black background issue while maintaining all existing functionality. The comprehensive logging and error handling ensure both good user experience and easy debugging.

The key insight is that scratching should ALWAYS reveal an underlay image - never fall back to black or gray. The implementation enforces this by requiring an underlay image for all overlay modes and providing clear feedback when none is available.