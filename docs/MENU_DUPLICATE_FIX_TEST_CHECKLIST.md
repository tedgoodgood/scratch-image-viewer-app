# Menu and Duplicate Image Fix - Test Checklist

## Issues Fixed

### Issue 1: Menu Not Displaying
**Root Cause**: Theme used `Theme.AppCompat.DayNight.NoActionBar` which disabled the action bar.

**Fix Applied**: Changed theme to `Theme.AppCompat.DayNight.DarkActionBar` in `themes.xml`.

### Issue 2: Duplicate Images in Scratch View  
**Root Cause**: Both `mainImage` ImageView and `scratchOverlay` ScratchOverlayView were displaying the same image simultaneously.

**Fix Applied**: Hide `mainImage` when gallery has images, letting scratch overlay be the sole image display mechanism.

## Testing Instructions

### 1. Menu Display Test

**Expected Behavior**:
- Menu should appear in the top-right corner (three dots or similar)
- Menu should contain "Settings" submenu
- Submenu should have: Opacity, Brush Size, Color options

**Test Steps**:
1. Launch the app
2. Look for menu icon in top-right corner
3. Tap menu icon
4. Verify "Settings" appears
5. Tap "Settings" 
6. Verify all three submenu items appear

**Expected Logs** (run `adb logcat -s MainActivity:D`):
```
onCreateOptionsMenu called
Menu inflated with 1 items
Menu item selected: [id] - Opacity
```

### 2. Menu Functionality Test

**Test Each Menu Item**:

**Opacity Dialog**:
1. Menu → Settings → Opacity
2. Should show dialog with current opacity percentage
3. Enter valid number (0-100) and tap Save
4. Toast should confirm opacity change
5. Scratch overlay should reflect new opacity

**Brush Size Dialog**:
1. Menu → Settings → Brush Size  
2. Should show dialog with current brush size in pixels
3. Enter valid number (> 0) and tap Save
4. Toast should confirm brush size change
5. Scratch strokes should use new size

**Color Picker Dialog**:
1. Menu → Settings → Color
2. Should show preset color list
3. Select any color and tap Apply
4. Toast should confirm color change
5. Scratch overlay should use new color

### 3. Duplicate Image Test

**Expected Behavior**:
- Only ONE image should be visible at a time
- No overlapping/duplicate images
- Scratch overlay should reveal the underlying image properly

**Test Steps**:
1. Launch app
2. Select 2-3 images using "Select Images"
3. Verify only one image shows (not multiple overlapping)
4. Start scratching - should reveal the same image underneath
5. Navigate between images using previous/next buttons
6. Verify each image shows singly without duplication

**Expected Logs** (run `adb logcat -s MainActivity:D GalleryViewModel:D`):
```
selectImages called with 2 URIs
Valid items extracted: 2
Current gallery size: 1
After deduplication: 2 unique images
Final merged gallery: 3 images
Hidden mainImage to prevent duplication with scratch overlay
Updated scratch view with image: [uri], total gallery: 3
```

### 4. Deduplication Test

**Test Steps**:
1. Select 2-3 unique images
2. Note the total count in image counter
3. Select the same images again (duplicates)
4. Verify total count doesn't increase
5. Verify no visual duplication

**Expected Logs**:
```
selectImages called with 2 URIs
Input URIs: [uri1, uri2]  // Same as before
Valid items extracted: 2
Current gallery size: 3
After deduplication: 2 unique images  // No increase
Final merged gallery: 3 images  // Same total
```

### 5. Folder Import Test

**Test Steps**:
1. Use "Select Folder" to import a folder with images
2. Verify only unique images are added
3. Try selecting the same folder again
4. Verify no duplicates are added
5. Check logs for proper deduplication

**Expected Logs**:
```
selectFolder called with URI: [folder_uri]
Folder enumeration found 5 images
Current gallery size before merge: 3
After folder merge deduplication: 5 unique images
Final merged gallery after folder: 6 images
```

### 6. Error Handling Test

**Invalid Opacity**:
1. Menu → Settings → Opacity
2. Enter invalid number (e.g., 150, -10, text)
3. Should show error toast, not crash

**Invalid Brush Size**:
1. Menu → Settings → Brush Size
2. Enter invalid number (e.g., 0, -5, text)
3. Should show error toast, not crash

### 7. Fullscreen Mode Test

**Test Steps**:
1. Select images
2. Tap fullscreen button
3. Menu should still be accessible in fullscreen
4. Menu functionality should work in fullscreen
5. Exit fullscreen and verify menu still works

## Debug Commands

**Monitor Menu and Image Operations**:
```bash
adb logcat -s MainActivity:D GalleryViewModel:D
```

**Clear Logs**:
```bash
adb logcat -c
```

## Expected Results Summary

- ✅ Menu appears in top-right corner
- ✅ Menu contains Settings with 3 submenu items
- ✅ All menu items work correctly
- ✅ Only one image displays at a time
- ✅ No duplicate images in gallery
- ✅ Deduplication works for both file and folder selection
- ✅ Logs show proper image counts and operations
- ✅ No visual duplication when scratching
- ✅ Error handling works gracefully
- ✅ App doesn't crash on invalid inputs

## Troubleshooting

**If menu still doesn't appear**:
1. Verify theme change was applied correctly
2. Check logs for "onCreateOptionsMenu called"
3. Ensure MainActivity extends AppCompatActivity
4. Verify onCreateOptionsMenu returns true

**If duplicate images persist**:
1. Check logs for "Hidden mainImage to prevent duplication"
2. Verify mainImage visibility is set to GONE
3. Ensure scratch overlay is receiving current image
4. Check layout for any other overlapping ImageViews

**If deduplication fails**:
1. Check logs for URI comparison
2. Verify .distinctBy { it.uri } is working
3. Ensure URI objects are comparable
4. Check for different URI schemes (content:// vs file://)