# Overlay Fixes Test Checklist

## Overview
This checklist tests the critical fixes for overlay opacity, custom image rendering, frosted glass effect, and reset button functionality.

## Test Environment
- Test on multiple API levels: 14, 16, 19, 21+
- Test with different image sizes and formats
- Test both portrait and landscape orientations

## 1. Color Overlay Opacity (50% → 90%)

### Test Steps:
1. Launch app
2. Select any image from gallery
3. Tap any color overlay button (Gold, Silver, Bronze)
4. **Expected**: Overlay should be much more opaque (90%), barely see the image underneath
5. Compare with previous behavior - should be significantly less transparent

### Verification:
- [ ] Gold overlay is 90% opaque (0xE6D4AF37)
- [ ] Silver overlay is 90% opaque (0xE6C0C0C0) 
- [ ] Bronze overlay is 90% opaque (0xE6CD7F32)
- [ ] Image underneath is barely visible before scratching
- [ ] Scratch reveals clear image underneath

## 2. Custom Image Overlay - Black Background Fix

### Test Steps:
1. Launch app
2. Select any image from gallery
3. Tap "Custom Overlay" button
4. Select a different image as overlay
5. **Critical**: Scratch on the overlay
6. **Expected**: Should see the ORIGINAL gallery image underneath, NOT black

### Verification:
- [ ] Custom overlay image loads properly
- [ ] Scratching reveals the original gallery image (not black)
- [ ] No black background visible through scratches
- [ ] Overlay rendering works in both portrait and landscape
- [ ] Works across different API levels

## 3. Frosted Glass - Stronger Blur + 90% Opacity

### Test Steps:
1. Launch app
2. Select any image from gallery
3. Tap "Frosted Glass" button
4. **Expected**: Should see heavily blurred image with 90% opacity
5. Scratch to verify original image underneath

### Verification:
- [ ] Frosted glass effect is much stronger (blur radius increased)
- [ ] Original image is almost unrecognizable when blurred
- [ ] 90% opacity makes it very opaque before scratching
- [ ] Scratch reveals clear original image
- [ ] Blur quality is acceptable across API levels
- [ ] API 14-16: Stack blur with 30px radius
- [ ] API 17+: RenderScript with 35px radius

## 4. Reset Button - Immediate Response

### Test Steps:
1. Launch app
2. Select any overlay type (color, custom, frosted glass)
3. Make several scratches on the overlay
4. Tap "Reset" button
5. **Expected**: All scratches should disappear immediately

### Verification:
- [ ] Reset button responds immediately when tapped
- [ ] All scratch marks disappear instantly
- [ ] Overlay is restored to clean state
- [ ] Works for all overlay types (color, custom, frosted glass)
- [ ] No lag or delay in reset response
- [ ] UI updates properly after reset

## 5. Cross-Feature Integration Tests

### Test Steps:
1. Test switching between different overlay types
2. Test reset after switching overlay types
3. Test navigation (previous/next) with different overlays
4. Test fullscreen mode with all overlay types

### Verification:
- [ ] Switching overlay types works without crashes
- [ ] Reset works after switching overlay types
- [ ] Navigation preserves overlay behavior
- [ ] Fullscreen mode works with all overlay types
- [ ] Base image updates correctly when navigating

## 6. Performance Tests

### Test Steps:
1. Test with large images (4K+)
2. Test rapid scratching
3. Test frequent overlay type switching
4. Test memory usage over time

### Verification:
- [ ] Large images load without crashing
- [ ] Rapid scratching remains responsive
- [ ] Overlay switching is smooth
- [ ] No memory leaks detected
- [ ] Blur cache works effectively

## 7. Edge Cases

### Test Steps:
1. Test with very small images
2. Test with unusual aspect ratios
3. Test with corrupted/invalid overlay images
4. Test during low memory conditions

### Verification:
- [ ] Small images scale properly
- [ ] Unusual aspect ratios handled correctly
- [ ] Invalid overlay images fall back gracefully
- [ ] Low memory handled without crashes

## 8. API Level Specific Tests

### API 14-16 (Legacy):
- [ ] Stack blur works with 30px radius
- [ ] Custom overlay rendering works
- [ ] Reset button works
- [ ] Color opacity is 90%

### API 17-30 (RenderScript):
- [ ] RenderScript blur works with 35px radius
- [ ] All features work as expected
- [ ] Performance is acceptable

### API 31+ (Modern):
- [ ] RenderScript fallback works
- [ ] All features work as expected
- [ ] No compatibility issues

## Bug Regression Tests

### Previous Issues to Verify Fixed:
- [ ] No black background in custom image overlay
- [ ] Reset button responds immediately
- [ ] Color overlays are 90% opaque
- [ ] Frosted glass has strong blur effect
- [ ] All overlays work consistently

## Acceptance Criteria

All of the following must be true:
- [ ] Color overlay: 90% opacity (much more opaque than before)
- [ ] Custom image overlay: Scratch reveals original image (NOT black)
- [ ] Frosted glass: Strong blur (unrecognizable), 90% opacity
- [ ] Reset button: Works immediately, clears all scratches
- [ ] All features work without crashes across API levels
- [ ] Performance remains acceptable
- [ ] No regressions in existing functionality

## Test Notes

### Critical Issues to Watch For:
1. **Black Background**: If custom overlay shows black instead of original image
2. **Reset Not Working**: If reset button doesn't clear scratches immediately
3. **Insufficient Blur**: If frosted glass effect is still too weak
4. **Wrong Opacity**: If overlays are still too transparent

### Performance Metrics:
- Overlay loading: < 2 seconds
- Reset response: < 500ms
- Scratch responsiveness: < 50ms lag
- Memory usage: No significant leaks over time

### Test Data:
- Use variety of image sizes (small, medium, large, 4K)
- Use different image formats (JPEG, PNG, WebP)
- Test with both content URIs and file URIs
- Test with different overlay images