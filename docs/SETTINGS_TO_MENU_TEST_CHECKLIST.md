# Settings to Menu Migration - Test Checklist

## Overview
This checklist verifies that all settings (opacity, brush size, color) have been successfully moved from the main UI to a menu system with input fields and color picker.

## UI Changes Verification

### ✅ Main UI Cleanup
- [ ] No opacity slider visible in main UI
- [ ] No brush size slider visible in main UI  
- [ ] No color picker button visible in main UI
- [ ] No gold/silver/bronze color buttons visible in main UI
- [ ] Only gallery image display area remains
- [ ] Only scratch overlay canvas remains
- [ ] Basic action buttons remain: Select Images, Select Folder, Reset
- [ ] Navigation controls remain: Previous, Next, Fullscreen
- [ ] Image counter remains visible
- [ ] Clean, minimal UI achieved

### ✅ Menu Implementation
- [ ] Menu appears when tapping menu icon (⋮)
- [ ] Settings menu item exists with preferences icon
- [ ] Settings submenu expands to show: Opacity, Brush Size, Color
- [ ] All menu items are selectable
- [ ] Menu icons display correctly

## Settings Dialogs Verification

### ✅ Opacity Dialog
- [ ] Opacity dialog opens when "Opacity" menu item selected
- [ ] Dialog title: "Set Opacity (%)"
- [ ] Input field shows current opacity value (converted from 0-255 to 0-100%)
- [ ] Input field is numeric only (TYPE_CLASS_NUMBER)
- [ ] Input field selects all text on focus
- [ ] "Save" button validates input (0-100 range)
- [ ] "Cancel" button closes dialog without saving
- [ ] Toast shows "Opacity set to X%" on successful save
- [ ] Toast shows error message for invalid input
- [ ] Setting persists after app restart
- [ ] Overlay opacity updates immediately after save

### ✅ Brush Size Dialog  
- [ ] Brush size dialog opens when "Brush Size" menu item selected
- [ ] Dialog title: "Set Brush Size (pixels)"
- [ ] Input field shows current brush size value
- [ ] Input field accepts decimal numbers (TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL)
- [ ] Input field selects all text on focus
- [ ] "Save" button validates input (greater than 0)
- [ ] "Cancel" button closes dialog without saving
- [ ] Toast shows "Brush size set to X" on successful save
- [ ] Toast shows error message for invalid input
- [ ] Setting persists after app restart
- [ ] Brush size updates immediately for new scratches

### ✅ Color Picker Dialog
- [ ] Color dialog opens when "Color" menu item selected
- [ ] Dialog title: "Select Color"
- [ ] Preset colors list shows: Red, Blue, Green, Yellow, Magenta, Cyan, Black, White, Gray, Dark Gray, Light Gray, Orange
- [ ] Current color is pre-selected in list
- [ ] Single choice selection works
- [ ] "Apply" button confirms selection
- [ ] "Cancel" button closes dialog without saving
- [ ] Toast shows "Color set to [ColorName]" on selection
- [ ] Setting persists after app restart
- [ ] Overlay color updates immediately after selection

## Functionality Verification

### ✅ Settings Persistence
- [ ] Opacity setting saves and restores on app restart
- [ ] Brush size setting saves and restores on app restart
- [ ] Color setting saves and restores on app restart
- [ ] All settings work independently
- [ ] Settings survive app process kill
- [ ] Settings work after selecting new images/folders

### ✅ Error Handling
- [ ] Invalid opacity input (text, negative, >100) shows error toast
- [ ] Invalid brush size input (text, zero, negative) shows error toast
- [ ] Empty input fields show appropriate error messages
- [ ] Dialogs handle edge cases gracefully
- [ ] App doesn't crash on invalid input

### ✅ Integration Testing
- [ ] Settings work with all overlay types (color, custom image, frosted glass)
- [ ] Settings work in both normal and fullscreen modes
- [ ] Settings work with all gallery navigation (previous/next)
- [ ] Reset button clears scratches but preserves settings
- [ ] Settings work after folder import
- [ ] Settings work after image selection

## Performance and UX

### ✅ User Experience
- [ ] Menu is easily accessible and discoverable
- [ ] Dialogs have appropriate titles and clear instructions
- [ ] Input fields have helpful hints and validation
- [ ] Toast messages provide clear feedback
- [ ] Settings changes apply immediately
- [ ] No lag or delay in opening dialogs
- [ ] Clean, uncluttered main interface

### ✅ Accessibility
- [ ] Menu items are accessible via screen readers
- [ ] Dialog content is accessible
- [ ] Input fields have appropriate content descriptions
- [ ] Color names are announced for accessibility
- [ ] All interactive elements are focusable

## Technical Implementation

### ✅ Code Quality
- [ ] No references to removed UI elements remain in code
- [ ] All imports are clean and unused imports removed
- [ ] Menu resource file is properly formatted
- [ ] Dialog implementations follow Android best practices
- [ ] Persistence logic is correctly implemented
- [ ] Error handling is comprehensive

### ✅ Architecture
- [ ] Settings logic properly separated from UI
- [ ] ViewModel methods correctly handle persistence
- [ ] State management is consistent
- [ ] No memory leaks in dialog implementations
- [ ] Proper lifecycle management

## Regression Testing

### ✅ Existing Features
- [ ] Image selection still works
- [ ] Folder import still works  
- [ ] Gallery navigation still works
- [ ] Fullscreen mode still works
- [ ] Reset functionality still works
- [ ] Scratch overlay still works
- [ ] All overlay types still work
- [ ] Error handling for other features still works

## Success Criteria
- [ ] ✅ All settings moved to menu (no settings in main UI)
- [ ] ✅ Opacity - input field (0-100%) with validation
- [ ] ✅ Brush Size - input field with decimal support
- [ ] ✅ Color - preset color picker with 12 colors
- [ ] ✅ All settings saved and restored on app restart
- [ ] ✅ Clean, minimal UI achieved
- [ ] ✅ Settings accessible via menu
- [ ] ✅ Proper error handling for invalid inputs
- [ ] ✅ No regression in existing functionality

## Notes
- Settings are stored in SavedStateHandle for persistence
- Input validation prevents invalid values
- Toast messages provide user feedback
- Dialogs use standard Android AlertDialog patterns
- Color picker uses preset colors for simplicity and API 14 compatibility