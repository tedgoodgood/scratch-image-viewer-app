# Folder Import Feature Test Checklist

## Overview
This checklist covers testing the folder import functionality across different API levels and scenarios.

## API Level Compatibility

### API 21+ (Android 5.0+) - Primary Path
- [ ] Use `ACTION_OPEN_DOCUMENT_TREE` intent
- [ ] Request persistable URI permissions
- [ ] Use DocumentFile API for recursive enumeration
- [ ] Support nested folder traversal (max depth 10)
- [ ] Handle permission persistence across app restarts

### API 14-20 (Android 4.0-4.4) - Fallback Path
- [ ] Use `ACTION_PICK` with directory type
- [ ] Use MediaStore queries for image enumeration
- [ ] Handle limited folder selection capabilities
- [ ] Provide appropriate error messages for unsupported scenarios

## UI Testing

### Button Interaction
- [ ] "Select Folder" button appears next to "Select Images" button
- [ ] Button is properly styled and sized
- [ ] Button is accessible with proper content description
- [ ] Button works in both normal and fullscreen modes

### Folder Picker Dialog
- [ ] Folder picker opens correctly on API 21+
- [ ] Fallback picker attempts to open on API 14-20
- [ ] Graceful handling when picker is cancelled
- [ ] Proper chooser dialog title "Select Folder"

## Functional Testing

### Folder Enumeration
- [ ] Images are found recursively in subfolders
- [ ] Only image files are included (MIME type filtering)
- [ ] Natural sorting of images by filename
- [ ] Duplicate prevention (same URI not added twice)
- [ ] Empty folder handling shows appropriate error

### Large Folder Performance
- [ ] Folders with 100+ images load within reasonable time
- [ ] Performance warning for folders with 1000+ images
- [ ] Memory usage remains reasonable during enumeration
- [ ] App remains responsive during folder processing

### Integration with Existing Features
- [ ] Folder images merge seamlessly with manually selected images
- [ ] Navigation (previous/next) works correctly with folder images
- [ ] Image counter updates properly
- [ ] Scratch overlay functionality works with folder images
- [ ] Fullscreen mode works with folder images
- [ ] State restoration preserves folder images (individual URIs, not folder)

## Error Handling

### Permission Issues
- [ ] Graceful handling of permission denials
- [ ] Clear error messages for permission failures
- [ ] App doesn't crash on permission issues

### File System Issues
- [ ] Handling of deleted/moved files after selection
- [ ] Handling of corrupted image files
- [ ] Handling of network storage that becomes unavailable
- [ ] Handling of read-only folders

### Empty/Invalid Folders
- [ ] Clear error for folders with no images
- [ ] Clear error for folders with only non-image files
- [ ] Graceful handling of invalid folder URIs

## State Persistence

### URI Permissions
- [ ] Persistable permissions are requested on API 21+
- [ ] Individual image URIs persist across app restarts
- [ ] Folder access remains available after app restart (API 21+)

### Gallery State
- [ ] Current image index preserved after app restart
- [ ] Folder images remain in gallery after app restart
- [ ] Scratch overlay state preserved appropriately

## Performance Testing

### Enumeration Speed
- [ ] Small folders (<50 images): <2 seconds
- [ ] Medium folders (50-200 images): <5 seconds
- [ ] Large folders (200-1000 images): <15 seconds
- [ ] Very large folders (1000+ images): Warning shown, still functional

### Memory Usage
- [ ] Memory usage scales reasonably with folder size
- [ ] No memory leaks during folder operations
- [ ] Garbage collection works properly after folder operations

## Accessibility Testing

- [ ] "Select Folder" button has proper content description
- [ ] Error messages are accessible via screen readers
- [ ] Loading states are announced appropriately
- [ ] Performance warnings are accessible

## Edge Cases

### Special Characters
- [ ] Folders with spaces in names
- [ ] Folders with special characters in names
- [ ] Image files with special characters in names
- [ ] International characters in folder/file names

### File System Scenarios
- [ ] Symbolic links (if supported)
- [ ] Network storage folders
- [ ] External SD card folders
- [ ] System folders (if accessible)

### Concurrent Operations
- [ ] Folder selection while another operation is in progress
- [ ] Multiple rapid folder selections
- [ ] App backgrounding during folder enumeration

## Manual Test Scenarios

### Basic Workflow
1. Launch app
2. Tap "Select Folder" button
3. Choose a folder with images
4. Verify all images are loaded in natural order
5. Navigate through images using previous/next buttons
6. Test scratch overlay on folder images
7. Test fullscreen mode
8. Restart app and verify images persist

### Empty Folder Workflow
1. Launch app
2. Tap "Select Folder" button
3. Choose an empty folder
4. Verify appropriate error message is shown
5. Verify app remains functional

### Large Folder Workflow
1. Launch app
2. Tap "Select Folder" button
3. Choose a folder with 1000+ images
4. Verify performance warning is shown
5. Verify images still load within reasonable time
6. Test app responsiveness during loading

### API 14-20 Specific Testing
1. Test on emulator/device running API 14-20
2. Verify fallback folder picker attempts to open
3. Test MediaStore-based enumeration
4. Verify appropriate error handling for limitations

## Automation Notes

- Unit tests should cover image MIME type detection
- Unit tests should cover natural sorting algorithms
- Integration tests should cover folder enumeration logic
- UI tests should cover button interactions and error states

## Known Limitations

1. **API 14-20**: Limited folder selection capabilities due to Android restrictions
2. **Folder Persistence**: Folder URIs are not persisted across app restarts for simplicity
3. **Large Folders**: Performance may degrade with very large folders (>1000 images)
4. **Network Storage**: May have limited support depending on storage provider