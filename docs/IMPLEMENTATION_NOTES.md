# Substitution Integration Implementation Notes

## Overview
This document describes the implementation of substitution and teacher absence display in the timetable feature. The core infrastructure is complete, but two key functions need library-specific details to be fully functional.

## What's Been Implemented

### 1. Data Flow (✅ Complete)
```
SubstitutionRepository (injected)
    ↓
TimetableViewModel.loadSubstitutionData()
    ↓
TimetableState (substitutionStatus, teacherAbsences, dailySubstitutions)
    ↓
TimetableSubScreen.processSubstitutions()
    ↓
Timetable component (substitutions map)
    ↓
LessonSpot → findSubstitutionForLesson()
    ↓
Lesson component (with substitutionColor & originalText)
```

### 2. Files Modified
- `TimetableState.kt` - Added dailySubstitutions field
- `TimetableViewModel.kt` - Added SubstitutionRepository injection and loadSubstitutionData()
- `TimetableSubScreen.kt` - Added processSubstitutions() and passes data to Timetable
- `Timetable.kt` - Added substitution parameter, matching logic, and originalText display

### 3. Visual Features Implemented
- ✅ Colored borders on lessons with substitutions (2dp width)
- ✅ Original text display in bottom-left corner (red, bold, 10sp)
- ✅ Teacher absences section with expandable cards
- ✅ Substitution status info display

### 4. Colors (from Color.kt)
```kotlin
val substitution = grade_5      // General substitution (red)
val dropped = Color(0xFF4CAF50) // Lesson dropped (green)
val joined = Color(0xFF2196F3)  // Classes joined (blue)
val room_change = Color(0xFFFF9800) // Room changed (orange)
val shifted = Color(0xFF9C27B0) // Time shifted (purple)
val separated = Color(0xFF795548) // Class separated (brown)
```

## What Needs Completion

### ⚠️ TODO 1: Complete `findSubstitutionForLesson()` Function
**Location:** `app/src/main/kotlin/me/tomasan7/jecnamobile/ui/component/Timetable.kt:396`

**Current State:** Returns `null` (no substitutions shown)

**What's Needed:**
1. Understand the structure of `SubstitutedLesson` from `io.github.stevekk11.dtos`
2. Understand the map key format from `DailySchedule.classSubs`

**Likely SubstitutedLesson fields to check:**
- `subject` or `subjectCode` - to match lesson.subjectName.short
- `teacher` or `teacherCode` - to match lesson.teacherName?.short
- `hour` or `period` - to match the lesson's time slot
- `group` - to match lesson.group (for split lessons)
- `day` or `dayOfWeek` - to match which day
- Boolean flags: `isDropped`, `isShifted`, `isJoined`, `isSeparated`, `roomChanged` (✅ confirmed)

**Likely map key formats to handle:**
- Option A: `"hour_group"` (e.g., "3_A" for 3rd hour, group A)
- Option B: `"day_hour"` (e.g., "Monday_3")
- Option C: `"subject_hour_group"`
- Option D: Simple integer index

**Example Implementation Pattern:**
```kotlin
private fun findSubstitutionForLesson(
    lesson: Lesson,
    substitutions: Map<String, SubstitutedLesson>
): SubstitutedLesson? {
    return substitutions.values.firstOrNull { sub ->
        // Match by subject (if available)
        val subjectMatches = lesson.subjectName.short?.let { subjectShort ->
            subjectShort.equals(sub.subject, ignoreCase = true)
        } ?: true
        
        // Match by teacher (if available)
        val teacherMatches = lesson.teacherName?.short?.let { teacherShort ->
            teacherShort.equals(sub.teacher, ignoreCase = true)
        } ?: true
        
        // Match by group (important for split lessons)
        val groupMatches = if (lesson.group != null) {
            lesson.group == sub.group
        } else {
            sub.group == null || sub.group.isEmpty()
        }
        
        // Return true if at least one identifier matches and group is compatible
        (subjectMatches || teacherMatches) && groupMatches
    }
}
```

### ⚠️ TODO 2: Extract originalText from SubstitutedLesson
**Location:** `app/src/main/kotlin/me/tomasan7/jecnamobile/ui/component/Timetable.kt:199`

**Current State:** Returns `null` (falls back to showing lesson.clazz)

**What's Needed:**
Find the field name in `SubstitutedLesson` that contains the original information.

**Possible field names to check:**
- `originalText`
- `original`
- `originalTeacher`
- `originalSubject`
- `wasTeacher` / `wasSubject`
- `note` or `remark`

**Example Implementation:**
```kotlin
val originalText = substitutedLesson?.let { sub ->
    // Replace 'originalTeacher' with actual field name
    sub.originalTeacher ?: sub.originalSubject ?: sub.original
}
```

**Alternative approach if multiple fields:**
```kotlin
val originalText = substitutedLesson?.let { sub ->
    buildString {
        if (sub.originalTeacher != null) {
            append("was: ${sub.originalTeacher}")
        }
        if (sub.originalSubject != null) {
            if (isNotEmpty()) append(" ")
            append(sub.originalSubject)
        }
    }.takeIf { it.isNotEmpty() }
}
```

## How to Find Library Structure

### Method 1: Check Library Documentation
Look for the GitHub repository: `https://github.com/stevekk11/jecna-supl`

### Method 2: Use IDE Features
1. In Android Studio, navigate to any usage of `SubstitutedLesson`
2. Cmd/Ctrl + Click on the class name
3. IDE will show the decompiled library source

### Method 3: Build Project
```bash
./gradlew build
```
Then check the library JAR in:
```
~/.gradle/caches/modules-2/files-2.1/io.github.stevekk11/jecna-supl/1.0.3/
```

### Method 4: Gradle Dependencies Task
```bash
./gradlew app:dependencies --configuration runtimeClasspath | grep jecna-supl
```

### Method 5: Check API Response
Since the data comes from `https://jecnarozvrh.jzitnik.dev/versioned/v1`:
```bash
curl https://jecnarozvrh.jzitnik.dev/versioned/v1/schedule/daily
```
Examine the JSON structure to understand the data format.

## Testing Checklist

Once the TODOs are completed:

### Unit Tests (if applicable)
- [ ] Test `findSubstitutionForLesson()` with various lesson types
- [ ] Test matching with group-specific substitutions
- [ ] Test matching when lesson is not split

### Manual Testing
- [ ] Build project: `./gradlew assembleDebug`
- [ ] Install on device/emulator
- [ ] Navigate to Timetable screen
- [ ] Verify substitutions show colored borders
- [ ] Verify original text appears in red (bottom-left)
- [ ] Verify colors match substitution type:
  - Dropped → Green
  - Joined → Blue
  - Room change → Orange
  - Shifted → Purple
  - Separated → Brown
  - General substitution → Red
- [ ] Test with split lessons (verify correct group)
- [ ] Verify teacher absences section displays correctly
- [ ] Take screenshots for PR

### Edge Cases
- [ ] No substitutions available (should show normal timetable)
- [ ] API error (should fail silently, show normal timetable)
- [ ] Split lesson with substitution (should match correct group)
- [ ] Multiple substitutions on same day

## Additional Notes

### Class Symbol Setup
The `SubstitutionRepository.setClassSymbol()` is called somewhere in the app initialization. If substitutions aren't appearing, verify:
1. The class symbol is being set correctly
2. The format matches what the API expects (e.g., "4.A", "4A", "4-A")

### Data Refresh
Substitution data is loaded:
- When timetable data is loaded (fresh or from cache)
- Via `loadSubstitutionData()` in ViewModel
- This ensures substitutions are always current with timetable

### Performance
- Substitution matching runs for each lesson spot on render
- Current implementation is O(n*m) where n=lessons, m=substitutions
- Consider caching or indexing if performance is an issue

### Future Enhancements
- [ ] Cache substitution data separately
- [ ] Show substitution details in lesson dialog
- [ ] Add filter to show only lessons with substitutions
- [ ] Notification when new substitutions appear
- [ ] Historical substitution view

## Questions for Code Review

1. Should we handle the case where SubstitutionRepository returns null differently?
2. Should substitution status be shown even when there are no substitutions?
3. Should we add loading state specifically for substitution data?
4. Color choices - are they accessible for colorblind users?

## References

- Problem Statement: PR/Issue description
- Library: `io.github.stevekk11:jecna-supl:1.0.3`
- API Endpoint: `https://jecnarozvrh.jzitnik.dev/versioned/v1`
- Color definitions: `app/src/main/kotlin/me/tomasan7/jecnamobile/ui/theme/Color.kt`
- Example pattern: `PredictedGrade` feature in grades module
