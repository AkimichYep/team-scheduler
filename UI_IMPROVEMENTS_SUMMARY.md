# Scheduler UI Improvements - Summary

## Overview
Added color-coded statuses and improved modal popup for better user experience. OnCall is now an overlay feature that can be combined with regular activities.

## Changes Made

### Backend (Java)

#### 1. **ScheduleEntry Model** (`src/main/java/com/scheduler/model/ScheduleEntry.java`)
- Added `isOnCall` Boolean field to track OnCall overlay status
- Updated activity field documentation to exclude "O" (OnCall is now separate)
- Activities are now: Dev, Spt, Off, V (Vacation), H (Holiday)

#### 2. **ScheduleController** (`src/main/java/com/scheduler/controller/ScheduleController.java`)
- Updated `ScheduleEntryRequest` DTO to include `isOnCall` field with getter/setter
- Updated `updateScheduleEntry()` methods to accept and handle isOnCall parameter
- Maintains backward compatibility with old signature methods

#### 3. **ScheduleService** (`src/main/java/com/scheduler/service/ScheduleService.java`)
- Added overloaded `updateScheduleEntry()` methods to support isOnCall parameter:
  - `updateScheduleEntry(userId, date, activity, isOnCall, notes)`
  - `updateScheduleEntry(userId, date, hour, activity, isOnCall, notes)`
- Kept original methods for backward compatibility

### Frontend (EJS & JavaScript)

#### 1. **Updated Legend** (scheduler.ejs)
- Changed activity names display:
  - "D - Development" → "Dev - Development"
  - "S - Support" → "Spt - Support"
  - Removed "O - OnCall" as a main activity
  - Added "🔴 OnCall (Overlay)" to show it's an additive feature
- Added visual gradient indicator for OnCall overlay

#### 2. **Enhanced Modal Dialog**
- Split activity selection into two sections:
  - **Regular Activity** section with buttons for: Dev, Spt, Off, V, H
  - **OnCall Enhancement** section with checkbox
  - Clear separation with visual divider
  - Helpful text: "Check this if you're on-call during these hours"
  - OnCall uses red color indicator (🔴)

#### 3. **Color-Coded Status Styling**
- Added CSS classes for visual indicators:
  - `.oncall-overlay` - gradient overlay effect
  - `.day-cell.oncall::after` - red indicator dot
  - `.activity-cell.oncall::after` - red checkmark badge
  - `.month-card-day.oncall::after` - red checkmark badge
- Maintains distinct colors for each activity type:
  - Dev: Green (#c8e6c9)
  - Spt: Blue (#bbdefb)
  - Off: Gray (#ffe0b2)
  - Vacation: Pink (#f8bbd0)
  - Holiday: Purple (#ce93d8)

#### 4. **Updated Views**

**Month View:**
- Shows activity clearly (Dev, Spt, Off, V, H)
- Red indicator (●) appears when OnCall is enabled
- Click on date to open modal for editing

**Week View:**
- Activity dropdown with new names
- New "OnCall" checkbox column
- Immediate save on change
- Notes field maintained

**Day View:**
- Activity dropdown with new names
- New "OnCall" checkbox column
- Immediate save on change
- Notes field maintained

**24-Hour Hourly View:**
- Shows activity abbreviation (Dev, Spt, Off, etc.)
- Red checkmark (✔) indicator for OnCall hours
- Click on specific hour to edit
- Modal includes OnCall checkbox for that hour

**Year View:**
- Displays activity name
- Red checkmark (✔) indicator for OnCall days
- Maintains monthly grid layout

#### 5. **JavaScript Functions Updated**
- `openModal()` - now accepts `isOnCall` parameter
- `openHourlyModal()` - now accepts `isOnCall` parameter
- `selectActivityType()` - supports new activity names
- `fillNext5Days()` - includes OnCall when filling
- `saveActivityToBackend()` - sends isOnCall field
- `saveActivityEntry()` - handles both old and new signatures
- `saveModalChanges()` - includes isOnCall in request
- All render functions updated to display OnCall indicator

## Activity Type Mapping

### New Activity Types (Database)
| Code | Display | Color | Description |
|------|---------|-------|-------------|
| Dev | Dev | Green | Development work |
| Spt | Spt | Blue | Support/Maintenance |
| Off | Off | Gray | Day off |
| V | V | Pink | Vacation |
| H | H | Purple | Holiday |

### OnCall (Overlay)
- Can be combined with ANY activity type
- Indicated by red indicators (●, ✔, or visual badge)
- Does not override regular work activity
- User can work Dev + OnCall, Off + OnCall, etc.

## Database Migration

The application uses H2 in-memory database with JPA/Hibernate automatic schema generation.

**New Column:**
- `is_on_call` (BOOLEAN, NOT NULL, DEFAULT: false) on `schedule_entries` table

Changes will be applied automatically on application startup.

## How to Use

### Setting OnCall for a Date:
1. Click on a date in the calendar (Month, Year, or Day view)
2. Modal appears with activity options
3. Select main activity (Dev, Spt, Off, etc.)
4. Check "🔴 OnCall" checkbox if on-call that day
5. Add notes if needed
6. Click "Save"

### Quick Actions:
- **Fill Next 5 Days**: Select activity+OnCall status, click "Fill Next 5 Days" button
- **Week View**: 
  - Dropdown to select activity
  - Checkbox to toggle OnCall
  - Notes field
  - Changes save immediately
- **Day View**: Same as week view but for single day

### Visual Indicators:
- **Month/Year View**: Red dot (●) or checkmark (✔) indicates OnCall
- **Week View**: OnCall checkbox column
- **Day View**: OnCall checkbox column
- **Hourly View**: Red checkmark (✔) indicates OnCall hour

## Technical Details

### Database Schema Change
```sql
ALTER TABLE schedule_entries ADD COLUMN is_on_call BOOLEAN NOT NULL DEFAULT false;
```

### API Request Format
```json
{
  "date": "2026-06-15",
  "activity": "Dev",
  "isOnCall": true,
  "notes": "Update during standup"
}
```

### API Response Format
```json
{
  "id": 1,
  "date": "2026-06-15",
  "hourOfDay": 0,
  "activity": "Dev",
  "isOnCall": true,
  "notes": "Update during standup"
}
```

## Backward Compatibility

- Old activity type "O" (OnCall) is no longer recognized
- Existing schedules with "O" activity will need migration
- All legacy code paths updated to support new parameter
- Frontend gracefully handles missing isOnCall field (defaults to false)

## Testing Checklist

- [ ] Login and navigate to scheduler
- [ ] Click on a date to open modal
- [ ] Select regular activity (Dev, Spt, Off, etc.)
- [ ] Check OnCall checkbox
- [ ] Save and verify red indicator appears
- [ ] Week view: Change activity and toggle OnCall via checkboxes
- [ ] Day view: Same as week view
- [ ] Month view: Click on cell with OnCall to verify data
- [ ] Year view: Check OnCall indicator across year
- [ ] Hourly view: Select an hour, toggle OnCall, verify checkmark appears
- [ ] Fill next 5 days with activity + OnCall
- [ ] Verify toast notifications appear on save
- [ ] Check database contains is_on_call column

## Build & Deploy

```bash
# Build
mvn clean package

# Run
java -jar target/team-scheduler-0.0.1-SNAPSHOT.jar

# Frontend
npm install
node server.js
```

Visit: `http://localhost:3000/scheduler`

## Notes for User

- OnCall is **NOT** a replacement for regular activity selection
- OnCall acts as an **overlay** or **badge** on top of your work activity
- Users can be: "Dev + OnCall", "Support + OnCall", "Off + OnCall", etc.
- This provides flexible scheduling where users can work their regular hours AND be on-call
- Perfect for tracking when team members are available for urgent issues while doing their regular work

## Future Enhancements

- Bulk OnCall scheduling for multiple days
- OnCall templates (recurring on-call patterns)
- OnCall shift management
- Conflict detection (e.g., Vacation + OnCall warning)
- OnCall statistics/reporting

