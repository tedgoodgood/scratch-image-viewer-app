# Frosted Glass Overlay Test Checklist

## Feature Overview
The frosted glass overlay effect provides a blurred version of the underlying image that users can scratch through, alongside existing color and custom image overlay options.

## Implementation Details

### API Level Compatibility
- **API 31+**: Uses RenderEffect (fallback to RenderScript due to Canvas limitations)
- **API 17-30**: Uses RenderScript with ScriptIntrinsicBlur
- **API 14-16**: Uses CPU-based Stack Blur algorithm with reduced radius

### Blur Radius Configuration
- **Standard**: 15px radius (API 17+)
- **Reduced**: 10px radius (API 14-16 for performance)

### Performance Optimizations
- **Caching**: Blurred bitmaps are cached by URI and dimensions
- **Memory Management**: Cache is cleared when switching overlay types
- **Lazy Loading**: Blur is computed only when needed

## Test Scenarios

### 1. Basic Functionality
- [ ] Frosted glass button appears in overlay selection UI
- [ ] Clicking frosted glass button activates frosted overlay mode
- [ ] Frosted overlay shows blurred version of current image
- [ ] Scratch interactions work correctly on frosted overlay
- [ ] Reset button clears scratches on frosted overlay

### 2. Overlay Type Switching
- [ ] Switching from color to frosted works correctly
- [ ] Switching from custom image to frosted works correctly
- [ ] Switching from frosted to color works correctly
- [ ] Switching from frosted to custom image works correctly
- [ ] Only one overlay type is active at any time

### 3. Image Navigation
- [ ] Frosted overlay updates when navigating to next image
- [ ] Frosted overlay updates when navigating to previous image
- [ ] Frosted overlay works with different image formats
- [ ] Frosted overlay scales correctly for different image sizes

### 4. State Persistence
- [ ] Frosted overlay type survives screen rotation
- [ ] Frosted overlay type survives process death
- [ ] Selected color is restored when switching back from frosted
- [ ] Custom image URI is preserved when switching back from frosted

### 5. Performance Testing
- [ ] Blur computation completes within acceptable time (<2 seconds)
- [ ] Cached blur results are reused for same image/dimensions
- [ ] Memory usage remains reasonable during blur operations
- [ ] UI remains responsive during blur processing
- [ ] Large images don't cause OOM crashes

### 6. API Level Testing
- [ ] API 14-16: Stack blur produces acceptable quality
- [ ] API 17-30: RenderScript blur works correctly
- [ ] API 31+: Fallback blur works correctly
- [ ] No crashes on any supported API level

### 7. Edge Cases
- [ ] Frosted overlay handles null/invalid image URIs gracefully
- [ ] Frosted overlay works with very small images
- [ ] Frosted overlay works with very large images
- [ ] Frosted overlay handles corrupted image files
- [ ] Cache is properly managed on memory pressure

### 8. Visual Quality
- [ ] Blur effect is visually appealing and frosted-glass-like
- [ ] Underlying image content is visible but obscured
- [ ] No visual artifacts in blur effect
- [ ] Consistent blur quality across different images
- [ ] Proper transparency/alpha handling

### 9. Integration Testing
- [ ] Frosted overlay works with fullscreen mode
- [ ] Frosted overlay works with brush size changes
- [ ] Frosted overlay works with folder import
- [ ] Frosted overlay works with image selection
- [ ] Error states don't break frosted overlay functionality

### 10. Accessibility
- [ ] Frosted glass button has proper content description
- [ ] Frosted overlay mode is announced to screen readers
- [ ] Sufficient contrast for UI elements over frosted overlay
- [ ] Touch targets remain accessible over frosted overlay

## Manual Testing Instructions

### Testing Blur Quality
1. Load a high-detail image
2. Select frosted glass overlay
3. Verify the image is blurred but still recognizable
4. Check for banding or artifacts in the blur
5. Test with images of different sizes and aspect ratios

### Testing Performance
1. Load a large image (2000x2000+ pixels)
2. Time the blur computation
3. Navigate between multiple images rapidly
4. Monitor memory usage during operations
5. Test on low-end devices if available

### Testing State Management
1. Set frosted overlay
2. Rotate device/screen
3. Kill app process and restart
4. Verify overlay type is preserved
5. Test with different combinations of settings

## Expected Behaviors

### Visual
- Smooth, gaussian-like blur effect
- Image content visible but obscured
- No sharp edges or artifacts
- Consistent blur across entire image

### Performance
- Blur computation <2 seconds for typical images
- Smooth UI interactions during processing
- No ANRs or crashes
- Reasonable memory footprint

### Functional
- Scratch reveals underlying sharp image
- Overlay type switching is instant
- State persists across configuration changes
- Graceful fallback on errors

## Known Limitations

1. **RenderScript Deprecation**: Uses deprecated RenderScript API for API 17-30
2. **Performance on API 14-16**: Reduced blur radius for performance
3. **Memory Usage**: Large images may require significant memory for blur
4. **Cache Size**: Unlimited cache may grow with many different images

## Future Improvements

1. **Modern Blur API**: Migrate to newer blur APIs when available
2. **Progressive Loading**: Show progressive blur during computation
3. **Memory Optimization**: Implement LRU cache with size limits
4. **Custom Blur Radius**: Allow user to adjust blur intensity
5. **Performance Monitoring**: Add performance metrics and warnings