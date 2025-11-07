# Fullscreen Navigation Controls - Test Checklist

## Overview
This document outlines the manual testing checklist for the fullscreen navigation controls feature. The feature enhances fullscreen mode with clearly visible previous/next navigation buttons and an exit fullscreen control while maintaining responsive scratch gesture interaction.

## Feature Summary
- **Top overlay bar** with 48dp touch targets containing:
  - Previous button (left)
  - Next button (right)
  - Exit fullscreen button (far right)
- **Accessibility**: Content descriptions and proper enabled/disabled states
- **Styling**: Dark background with gradient for contrast compliance
- **Gesture handling**: Scratch overlay accepts touches outside button bounds
- **API coverage**: Works on API 14-34

## Manual Test Cases

### 1. Button Visibility and Display
- [ ] **1.1 Fullscreen mode toggle**
  - Enter fullscreen mode by tapping the fullscreen button
  - Verify the overlay control bar appears at the top of the screen
  - Verify the normal bottom controls disappear
  - Exit fullscreen using the exit button on the overlay bar
  - Verify bottom controls reappear and overlay bar disappears

- [ ] **1.2 Overlay bar styling**
  - Verify the overlay bar has a semi-transparent dark background
  - Verify buttons are clearly visible with good contrast
  - Verify the background does not obscure the image content

### 2. Navigation Button Functionality
- [ ] **2.1 Next button behavior**
  - Enter fullscreen mode with multiple images loaded
  - Tap the Next button on the overlay bar
  - Verify the image changes to the next one
  - Verify the scratch segments reset when changing images
  - Verify the Next button is disabled on the last image

- [ ] **2.2 Previous button behavior**
  - In fullscreen mode, tap the Previous button
  - Verify the image changes to the previous one
  - Verify the scratch segments reset when changing images
  - Verify the Previous button is disabled on the first image

- [ ] **2.3 Button enabled/disabled states**
  - Load the first image, enter fullscreen
  - Verify Previous button appears disabled (grayed out)
  - Navigate to the last image
  - Verify Next button appears disabled (grayed out)
  - Navigate to the middle image
  - Verify both buttons are enabled

### 3. Exit Fullscreen
- [ ] **3.1 Exit button functionality**
  - In fullscreen mode, tap the exit button (X icon)
  - Verify fullscreen mode is toggled off
  - Verify the overlay bar disappears
  - Verify the bottom controls appear again

- [ ] **3.2 Exit via fullscreen button**
  - In fullscreen mode, tap the fullscreen icon in bottom controls (should still be visible or accessible)
  - OR use the overlay bar exit button
  - Verify fullscreen mode is correctly exited

### 4. Touch Interaction Testing
- [ ] **4.1 Scratch gesture outside buttons**
  - Enter fullscreen mode with a color overlay selected
  - Draw scratch gestures in the middle/bottom of the image (away from buttons)
  - Verify scratching works normally
  - Verify the overlay bar does not interfere with scratches

- [ ] **4.2 Touch target precision**
  - Verify each button has approximately 48dp x 48dp touch target
  - Attempt to tap edge of button area
  - Verify reliable activation

- [ ] **4.3 Button touch separation**
  - Tap Previous button, verify only previous action occurs
  - Tap Next button, verify only next action occurs
  - Tap Exit button, verify only exit action occurs
  - Verify no unintended side effects

### 5. Overlay Change Integration
- [ ] **5.1 Scratch state reset on navigation**
  - In fullscreen mode, draw on the current image
  - Tap Next button
  - Verify the previous image's scratch is cleared
  - Verify new image starts with clean overlay

- [ ] **5.2 Different overlay types**
  - Test with Gold overlay
  - Test with Silver overlay
  - Test with Bronze overlay
  - Test with custom image overlay
  - Verify navigation works with all overlay types

### 6. API Level Compatibility

#### API 14-16 (Legacy)
- [ ] **6.1 Basic functionality**
  - Run on API 14 emulator
  - Verify buttons appear and function correctly
  - Verify layout does not crash
  - Verify touch events are properly handled

#### API 19-21 (KitKat - Lollipop)
- [ ] **6.2 Mid-level API**
  - Verify fullscreen controls appear correctly
  - Verify all button interactions work
  - Verify no rendering issues

#### API 24-29 (Nougat - Pie)
- [ ] **6.3 Modern API**
  - Verify all features work correctly
  - Verify proper integration with system UI

#### API 30-34 (Android 11+)
- [ ] **6.4 Latest API**
  - Verify proper gesture navigation compatibility
  - Verify system inset handling
  - Verify all features work as expected

### 7. Screen Size Testing

- [ ] **7.1 Small screens (< 5 inches)**
  - Test on small device/emulator
  - Verify buttons fit without overlap
  - Verify button spacing is appropriate
  - Verify image is still viewable

- [ ] **7.2 Medium screens (5-6.5 inches)**
  - Test standard phone layout
  - Verify all elements are properly displayed

- [ ] **7.3 Large screens (> 6.5 inches)**
  - Test on large device/emulator
  - Verify layout scales appropriately
  - Verify spacing maintains visual balance

- [ ] **7.4 Tablet/Landscape**
  - Test on tablet or landscape orientation
  - Verify layout adapts correctly
  - Verify button positioning remains optimal

### 8. Accessibility Testing

- [ ] **8.1 Content descriptions**
  - Use accessibility scanner (Accessibility Scanner app)
  - Verify each button has appropriate content description
  - Verify descriptions are clear and descriptive
  - Expected descriptions:
    - Previous button: "Previous image"
    - Next button: "Next image"
    - Exit button: "Exit fullscreen mode"

- [ ] **8.2 Touch target sizes**
  - Verify all buttons meet 48dp minimum touch target size
  - Use visual inspection or Android Studio layout inspector

- [ ] **8.3 Color contrast**
  - Use accessibility color contrast checker
  - Verify buttons have sufficient contrast against background
  - Verify disabled state still maintains visible contrast

- [ ] **8.4 Screen reader compatibility**
  - Enable TalkBack on device
  - Verify buttons are announced correctly
  - Verify state changes are announced (enabled/disabled)

### 9. State Persistence
- [ ] **9.1 Fullscreen state handling**
  - Enter fullscreen mode
  - Rotate device
  - Verify fullscreen mode is maintained or handled gracefully
  - Navigate images, then rotate
  - Verify state is preserved

- [ ] **9.2 Image selection**
  - Select new images while in fullscreen
  - Verify fullscreen mode is exited
  - Verify new images are loaded correctly

### 10. Edge Cases and Error Handling

- [ ] **10.1 Single image**
  - Load only one image
  - Enter fullscreen
  - Verify both navigation buttons are disabled
  - Verify exit button is enabled

- [ ] **10.2 Rapid button taps**
  - In fullscreen mode, rapidly tap Next button multiple times
  - Verify state updates correctly
  - Verify no crashes or state corruption

- [ ] **10.3 Memory stress**
  - Load many images (50+)
  - Navigate rapidly in fullscreen mode
  - Verify no memory leaks or crashes

- [ ] **10.4 Overlay switching during fullscreen**
  - In fullscreen, switch between different overlay colors
  - Verify UI updates correctly
  - Verify navigation still works

## Test Environment Setup

### Required Test Devices/Emulators
- API 14 (Ice Cream Sandwich) - if available
- API 19 (KitKat)
- API 24 (Nougat)
- API 29 (Pie)
- API 34 (Android 14)

### Testing Tools
- Android Emulator or physical devices
- Android Studio Layout Inspector
- Accessibility Scanner (Google Play)
- TalkBack (Google Play)

## Test Results Summary

### Passing Tests
- [ ] All button visibility tests pass
- [ ] Navigation functionality works correctly
- [ ] Touch interactions are responsive
- [ ] Accessibility requirements met
- [ ] All tested API levels function properly

### Known Issues/Notes
(To be filled during testing)

---

## Automated Test Coverage

While manual testing is essential, the following areas should ideally have automated test coverage:

### Unit Tests (ViewModel)
- Navigation state transitions (`goToPrevious`, `goToNext`)
- Button enabled/disabled state computation (`canGoPrevious`, `canGoNext`)
- Fullscreen state toggling

### UI Tests (Instrumentation)
- Fullscreen controls visibility toggle
- Button click handling
- Navigation state reflection in UI
- Scratch overlay interaction while in fullscreen

## Implementation Details

### Layout Structure
```xml
<FrameLayout id="fullscreenControlsContainer">
    <LinearLayout id="fullscreenControlsOverlay">
        <ImageButton id="fullscreenPreviousButton" />
        <Space layout_weight="1" />
        <ImageButton id="fullscreenNextButton" />
        <ImageButton id="fullscreenExitButton" />
    </LinearLayout>
</FrameLayout>
```

### Styling
- **Background**: Dark gradient (`controls_background` drawable)
- **Button Size**: 48dp x 48dp (minimum touch target)
- **Padding**: 8dp around controls
- **Icons**: Android system icons (ic_media_previous, ic_media_next, ic_menu_close_clear_cancel)

### Accessibility
- All buttons have content descriptions
- Buttons properly reflect enabled/disabled state
- Touch targets meet 48dp minimum
- Proper color contrast maintained

---

## Sign-Off

### Test Completed By
- Name: ___________________
- Date: ___________________
- Device/Emulator: ___________________

### Quality Assurance Sign-Off
- All critical tests passed: ☐
- No blocking issues found: ☐
- Ready for release: ☐

---

## Notes for Future Enhancements
1. Consider auto-hiding the overlay bar after a period of inactivity
2. Consider adding image counter to fullscreen controls
3. Consider swipe gestures for navigation in fullscreen
4. Consider animation transitions when showing/hiding controls
