#!/bin/bash

# Test script to verify status bar color consistency across activities
# After the theme fix, all activities should now have a blue status bar

DEVICE="emulator-5554"
PACKAGE="de.kalass.agime"

echo "=== Testing Status Bar Color Fix ==="
echo "Expected: Blue status bar on ALL activities"
echo "Device: $DEVICE"
echo ""

# Function to test an activity
test_activity() {
    local activity=$1
    local description=$2
    
    echo "Testing: $description"
    echo "Activity: $activity"
    
    # Launch the activity
    adb -s $DEVICE shell am start -n "$PACKAGE/$activity" 2>/dev/null
    
    echo "✓ Activity launched - Please check status bar color"
    echo "Press Enter to continue to next test..."
    read
    echo ""
}

# Test main activity (reference - should be blue)
test_activity ".AgimeMainActivity" "Main Activity (reference)"

# Test other common activities
test_activity ".activityrecord.ActivityRecordActivity" "Activity Record"
test_activity ".activitytype.ActivityTypeCRUDActivity" "Activity Type Management"
test_activity ".project.ProjectCRUDActivity" "Project Management"
test_activity ".settings.SettingsActivity" "Settings"

echo "=== Test Complete ==="
echo ""
echo "Summary:"
echo "- All activities should now have a BLUE status bar"
echo "- This matches the AgimeMainActivity reference"
echo "- The fix was applied to the Android 15+ theme files"
echo ""
echo "If any activity still shows a grey status bar, please report which one."
