# Folder Import Implementation

## Architecture Overview

The folder import feature extends the existing gallery functionality to allow users to select an entire folder and import all images within it. The implementation uses a dual-path approach to support API levels 14-34.

## Key Components

### 1. UI Layer (MainActivity.kt)
- **New Button**: "Select Folder" button added to top controls
- **Folder Selection Launcher**: Handles result from folder picker
- **API-Level Specific Intents**: Different intents for API 21+ vs 14-20

### 2. ViewModel Layer (GalleryViewModel.kt)
- **selectFolder()**: Main entry point for folder import
- **enumerateImagesFromDocumentFolder()**: Modern API 21+ implementation
- **enumerateImagesFromLegacyFolder()**: Fallback for API 14-20
- **Enhanced Error Handling**: Folder-specific error scenarios
- **Performance Monitoring**: Warning for large folders

### 3. Persistence Layer
- **URI Permission Handling**: Persistable permissions for API 21+
- **Individual Image Persistence**: Images persist, folder URIs don't
- **State Restoration**: Maintains current image and gallery state

## Implementation Details

### API Level Compatibility

#### API 21+ (Android 5.0+) - Primary Implementation
```kotlin
// Uses ACTION_OPEN_DOCUMENT_TREE
Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
}

// DocumentFile API for recursive enumeration
DocumentFile.fromTreeUri(application, folderUri)
```

**Features:**
- Full folder tree access with proper permissions
- Recursive directory traversal
- Robust MIME type filtering
- Persistable URI permissions

#### API 14-20 (Android 4.0-4.4) - Fallback Implementation
```kotlin
// Uses ACTION_PICK for directory selection
Intent(Intent.ACTION_PICK).apply {
    type = "vnd.android.cursor.dir/primary"
}

// MediaStore queries for image enumeration
resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ...)
```

**Features:**
- Limited folder selection due to Android restrictions
- MediaStore-based image enumeration
- Path-based filtering when possible
- Graceful degradation

### Folder Enumeration Logic

#### Recursive Traversal (API 21+)
- **Max Depth**: 10 levels to prevent infinite recursion
- **Cycle Detection**: Tracks visited URIs to prevent loops
- **MIME Filtering**: Only includes files with image MIME types
- **Natural Sorting**: Uses Collator for locale-aware sorting

#### MediaStore Query (API 14-20)
- **Path Filtering**: Attempts to filter by folder path
- **Data Column**: Uses DATA column for path information
- **Fallback**: Falls back to all images if path filtering fails

### Error Handling Strategy

#### Permission Errors
- Graceful handling of permission denials
- Clear error messages for users
- App remains functional after errors

#### Performance Considerations
- Warning for folders with 1000+ images
- Background processing on IO dispatcher
- Progress indicators during enumeration

#### File System Issues
- Handling of deleted/moved files
- Corrupted image file handling
- Network storage limitations

### Integration with Existing Features

#### Gallery State Management
- Seamless merging with existing images
- Deduplication by URI
- Natural sorting maintained
- Current index preservation

#### UI Consistency
- Same loading states as image selection
- Consistent error display mechanism
- Navigation controls work identically
- Fullscreen mode support maintained

## Performance Optimizations

### Enumeration Efficiency
- Early MIME type filtering
- URI deduplication during enumeration
- Lazy loading of image metadata
- Background processing

### Memory Management
- Streaming approach for large folders
- No full image data loaded during enumeration
- Proper cursor management
- Garbage collection friendly

### User Experience
- Loading indicators for feedback
- Performance warnings for large folders
- Graceful degradation for errors
- Responsive UI during processing

## Testing Strategy

### Unit Tests
- MIME type detection logic
- Natural sorting algorithms
- URI parsing and validation
- Error handling scenarios

### Integration Tests
- Folder enumeration end-to-end
- API level-specific behavior
- Permission handling
- State persistence

### Manual Testing
- Cross-API level verification
- Performance testing with large folders
- Error scenario validation
- Accessibility testing

## Known Limitations

1. **API 14-20 Restrictions**
   - Limited folder picker capabilities
   - May not work with all storage locations
   - Dependent on MediaStore indexing

2. **Performance Considerations**
   - Large folders (>1000 images) may be slow
   - Recursive traversal depth limited to 10 levels
   - Network storage may have performance issues

3. **Persistence Design**
   - Folder URIs not persisted (individual images are)
   - Users must re-select folders after app restart
   - Trade-off for simplicity and reliability

## Future Enhancements

### Potential Improvements
1. **Background Processing**: Implement folder enumeration in background service
2. **Progress Indicators**: Show detailed progress for large folders
3. **Selective Import**: Allow users to select specific subfolders
4. **Caching**: Cache folder enumeration results
5. **Enhanced API 14-20 Support**: Custom file browser for older APIs

### Performance Optimizations
1. **Parallel Processing**: Enumerate subfolders in parallel
2. **Incremental Loading**: Load images progressively
3. **Smart Filtering**: More sophisticated file type detection
4. **Memory Optimization**: Reduce memory footprint for large folders

## Security Considerations

### URI Permissions
- Only request read permissions
- Use persistable permissions when available
- Proper permission cleanup on errors

### File Access
- Validate file paths and URIs
- Handle malformed URIs gracefully
- Prevent directory traversal attacks

### Data Privacy
- No unnecessary file metadata collection
- Respect user privacy choices
- Clear permission requests