# Quick Start Guide - Enhanced Scheduler

## What's New?

✨ **Color-coded activity statuses** on the scheduler page
✨ **OnCall as an overlay** - can be combined with any activity
✨ **Improved modal popup** with clearer organization
✨ **Visual indicators** for OnCall hours (red badges)

## Getting Started

### 1. Start the Application

```bash
# Terminal 1: Start Spring Boot backend
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler
java -jar target/team-scheduler-0.0.1-SNAPSHOT.jar

# Terminal 2: Start Node.js frontend
cd C:\Users\Maksym_Yepaneshnikov\spring\team-scheduler\frontend
npm install  # if not already done
node server.js
```

### 2. Navigate to Scheduler
- Open browser: `http://localhost:3000`
- Login with your credentials
- Click "My Scheduler" or navigate to `http://localhost:3000/scheduler`

## Main Features

### Activity Types

| Type | Color | Meaning |
|------|-------|---------|
| **Dev** | 🟢 Green | Development work |
| **Spt** | 🔵 Blue | Support/Maintenance |
| **Off** | ⚪ Gray | Day off |
| **V** | 🔴 Pink | Vacation |
| **H** | 🟣 Purple | Holiday |

### OnCall (Overlay) - NEW!

Red indicator: **🔴** or **✔** means you're on-call
- Combines with ANY activity type
- Example: "Dev + OnCall" = Working on development AND available for urgent issues
- Shows on all views with red badge/indicator

### Views Available

1. **Month View** - See full month at glance
2. **Week View** - Detailed dropdown + checkbox for each day
3. **Day View** - Focus on single day with all details
4. **Hourly View (24-Hour)** - Hour-by-hour activity tracking
5. **Year View** - All 12 months in card layout

## How to Set Your Schedule

### Method 1: Click and Edit (Month/Year View)

```
1. Click on any date
2. Modal window appears
3. Select your ACTIVITY (Dev, Spt, Off, V, or H)
4. ✔ Check "OnCall" if you're on-call that day
5. Add notes (optional)
6. Click "Save"
```

### Method 2: Dropdown Selection (Week/Day View)

```
1. Select activity from dropdown
2. ✔ Check "OnCall" checkbox if needed
3. Add notes in the text field
4. Changes save automatically
```

### Method 3: Hour-by-Hour (24-Hour View)

```
1. Select date
2. Click on any hour (00-01, 01-02, etc.)
3. Modal appears for that specific hour
4. Select activity
5. ✔ Check "OnCall" if needed
6. Save
```

### Quick Fill: Apply Same Activity to Next 5 Days

```
1. Click on a date (Month view)
2. Select activity (e.g., "Dev")
3. ✔ Check "OnCall" if applicable
4. Click "Fill Next 5 Days" button
5. Activity is copied to next 4 days
```

## Color Legend

Look at the top of the scheduler page - you'll see the legend showing:

```
🟢 Dev - Development
🔵 Spt - Support
⚪ Off - Day off
🔴 V - Vacation
🟣 H - Holiday
🔴 🔴 OnCall (Overlay)
```

## Understanding OnCall

### What is OnCall?

OnCall is an **overlay** status - it doesn't replace your main activity.

**Examples:**
- **Dev + OnCall** = You're coding (development) but also on-call for urgent issues
- **Spt + OnCall** = You're doing support work AND you're on-call
- **Off + OnCall** = You're off (not working) but available for emergencies
- **Vacation + OnCall** = You're on vacation but can help if critical

### Visual Indicators

**When you're OnCall, you'll see:**
- 🔴 Red dot (●) on calendar dates
- ✔ Red checkmark on year/hourly view
- 🔴 Red badge on week/day view

## Tips & Tricks

### Save Time with Batch Operations

1. **Fill next 5 days**: Click date → select activity → check OnCall → click "Fill Next 5 Days"
2. **Templates**: Use existing patterns as templates for recurring schedules

### Track Your Week

1. Go to **Week View**
2. See all 7 days with dropdowns
3. Quickly update OnCall status with checkboxes

### Focus on One Day

1. Go to **Day View**
2. Use date picker or arrows to navigate
3. All 24 hours if needed (via 24-Hour View)

### See Full Year

1. Go to **Year View**
2. Scroll through 12 monthly cards
3. Click any day to edit

### Get Hour-Specific

1. Go to **24-Hour View**
2. Select specific date
3. Click on any hour to edit just that hour
4. Perfect for tracking shifts with breaks

## Data Persistence

- ✅ Changes save automatically (you'll see toast notification)
- ✅ Data stored in database
- ✅ Multiple saves aggregated (for performance)
- ✅ No manual "Save" button needed in most views

## Common Workflows

### Recurring Support Rotation

```
Week 1: Spt (Support), Off + OnCall (on-call weekends)
Week 2: Dev (Development), Off + OnCall
Week 3: Spt + OnCall
```

1. Use **Fill Next 5 Days** for weekday patterns
2. Manually click dates for weekend patterns
3. OnCall checkbox for on-call status

### Vacation with Emergency Access

```
Vacation (V) + OnCall (✔)
```

1. Set activity to "V" (Vacation)
2. Check "OnCall" box
3. Shows you're away but contactable

### All Development Work

```
Mon-Fri: Dev + OnCall (if on-call)
Sat-Sun: Off + OnCall (if on-call)
```

1. Week view makes this easy
2. Set all Dev rows
3. Toggle OnCall where needed

## Keyboard & Navigation

- **Previous/Next buttons** in Month, Week, Day, Year views
- **Date picker** in Day and 24-Hour views
- **Dropdown** in Week/Day view for quick selection
- **Checkbox** for OnCall (Tab to select, Space to toggle)

## Troubleshooting

**Changes not saving?**
- Check browser console (F12) for errors
- Ensure backend is running on port 8080
- Look for toast notification (bottom right)

**Can't see OnCall indicator?**
- Refresh page (F5)
- Check if you actually checked the OnCall box
- Open developer tools (F12) to inspect

**Activity options showing old names (D, S, O)?**
- Clear browser cache (Ctrl+Shift+Delete)
- Hard refresh (Ctrl+F5)
- Sign out and back in

## Support

- Backend API: `http://localhost:8080/api`
- Frontend: `http://localhost:3000`
- H2 Console: `http://localhost:8080/h2-console` (for debugging)

Enjoy your enhanced scheduler! 🚀

