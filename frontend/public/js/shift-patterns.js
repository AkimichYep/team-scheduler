/**
 * Shared Shift Patterns Constants and Utilities
 * Used across scheduler and summary views
 */

const SHIFT_PATTERNS = {
    'ShiftA_M': ['Off','Off','S','S','S','S','S','D','D','D','D','Off','Off','Off','Off','Off','Off','Off','Off','Off','Off','Off','Off','Off'],
    'ShiftB_M': ['Off','Off','Off','S','S','S','S','S','D','D','D','D','Off','Off','Off','Off','Off','Off','Off','Off','Off','Off','Off','Off'],
    'ShiftC_M': ['Off','Off','Off','Off','Off','Off','Off','S','S','S','S','S','D','D','D','D','Off','Off','Off','Off','Off','Off','Off','Off'],
    'ShiftD_M': ['Off','Off','Off','Off','Off','Off','Off','Off','Off','Off','S','S','S','S','D','D','D','D','S','Off','Off','Off','Off','Off'],
    'ShiftE_M': ['Off','Off','Off','Off','Off','Off','Off','Off','Off','D','D','D','D','S','S','S','S','S','Off','Off','Off','Off','Off','Off'],
    'ShiftA_S': ["Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"],
    'ShiftB_S': ["Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"],
    'ShiftC_S': ["Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off","Off","Off","Off"],
    'ShiftD_S': ["Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off"],
    'ShiftE_S': ["Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off"],
    'ShiftA_D': ["Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"],
    'ShiftB_D': ["Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"],
    'ShiftC_D': ["Off","Off","Off","Off","Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off"],
    'ShiftD_D': ["Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off"],
    'ShiftE_D': ["Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off"],
    'D': new Array(24).fill('D'),
    'S': new Array(24).fill('S'),
    'OnCall': new Array(24).fill('OnCall'),
    'Leave': new Array(24).fill('Leave'),
    'H': new Array(24).fill('H'),
    'V': new Array(24).fill('V'),
    'Off': new Array(24).fill('Off')
};

// Auto-generate OC combo patterns
Object.keys(SHIFT_PATTERNS).forEach(function(key) {
    if (key.startsWith('Shift')) {
        const base = SHIFT_PATTERNS[key];
        SHIFT_PATTERNS['OC_' + key]           = base.map(function(a,i){ return i < 2 ? 'OnCall' : a; });
        SHIFT_PATTERNS[key + '_OC']           = base.map(function(a,i){ return i >= 19 ? 'OnCall' : a; });
        SHIFT_PATTERNS['OC_' + key + '_OC']   = base.map(function(a,i){ return (i < 2 || i >= 19) ? 'OnCall' : a; });
    }
});

/**
 * Resolve actual activity for a given shift type + hour
 */
function resolveHourlyActivity(activity, hour) {
    if (SHIFT_PATTERNS[activity]) {
        return SHIFT_PATTERNS[activity][hour] || 'Off';
    }
    return activity || 'Off';
}

/**
 * Check if activity is a shift type
 */
function isShiftType(activity) {
    return activity in SHIFT_PATTERNS;
}

/**
 * Get activity priority for dominant activity calculation
 */
function activityPriority(activity) {
    const p = {
        'D': 10, 'S': 9, 'OnCall': 8, 'OC': 8,
        'Development': 10, 'Support': 9,
        'ShiftA': 5, 'ShiftB': 5, 'ShiftC': 5, 'ShiftD': 5, 'ShiftE': 5,
        'ShiftA_M': 5, 'ShiftB_M': 5, 'ShiftC_M': 5, 'ShiftD_M': 5, 'ShiftE_M': 5,
        'ShiftA_S': 5, 'ShiftB_S': 5, 'ShiftC_S': 5, 'ShiftD_S': 5, 'ShiftE_S': 5,
        'ShiftA_D': 5, 'ShiftB_D': 5, 'ShiftC_D': 5, 'ShiftD_D': 5, 'ShiftE_D': 5,
        'Leave': 4, 'V': 3, 'Vacation': 3, 'H': 2, 'Holiday': 2, 'Off': 0
    };
    return p[activity] !== undefined ? p[activity] : 1;
}

/**
 * Get the dominant activity from a list of entries
 */
function getDominantActivity(entries) {
    if (!entries || entries.length === 0) return 'Off';

    // Normalize activities
    const normalizedEntries = entries.map(e => {
        let act = e.activity;
        if (act === 'Development') act = 'D';
        if (act === 'Support') act = 'S';
        if (act === 'OnCall') act = 'OC';
        return { ...e, activity: act };
    });

    return normalizedEntries.reduce((best, e) => {
        return activityPriority(e.activity) > activityPriority(best) ? e.activity : best;
    }, 'Off');
}

/**
 * Detect shift pattern from entries
 */
function detectShiftPattern(entries) {
    if (!entries || entries.length === 0) return null;

    const hourMap = new Array(24).fill('Off');
    let onCallHours = [];

    entries.forEach(e => {
        if (e.hourOfDay !== undefined && e.hourOfDay >= 0 && e.hourOfDay < 24) {
            let act = e.activity;
            if (act === 'Development' || act === 'D') act = 'D';
            else if (act === 'Support' || act === 'S') act = 'S';
            else if (act === 'OnCall' || act === 'OC') act = 'OnCall';

            hourMap[e.hourOfDay] = act;
            if (act === 'OnCall') {
                onCallHours.push(e.hourOfDay);
            }
        }
    });

    // Try to match base patterns
    for (const [shiftName, pattern] of Object.entries(SHIFT_PATTERNS)) {
        if (!/^Shift[A-E](_[MSD])?$/.test(shiftName)) continue;

        if (pattern.every((act, i) => act === hourMap[i])) {
            return shiftName;
        }
    }

    // Try base pattern matching (allowing for OnCall additions)
    const hourMapForMatching = hourMap.slice();
    for (let i = 0; i < 24; i++) {
        if (hourMapForMatching[i] === 'OnCall') {
            hourMapForMatching[i] = 'Off';
        }
    }

    let matchedShiftName = null;
    for (const [shiftName, pattern] of Object.entries(SHIFT_PATTERNS)) {
        if (!/^Shift[A-E](_[MSD])?$/.test(shiftName)) continue;

        if (pattern.every((act, i) => act === hourMapForMatching[i])) {
            matchedShiftName = shiftName;
            break;
        }
    }

    // Check for OnCall patterns if base pattern matched
    if (matchedShiftName && onCallHours.length > 0) {
        const basePattern = SHIFT_PATTERNS[matchedShiftName];
        let shiftStart = -1, shiftEnd = -1;
        for (let i = 0; i < 24; i++) {
            if (basePattern[i] !== 'Off') {
                if (shiftStart === -1) shiftStart = i;
                shiftEnd = i;
            }
        }

        const minOnCall = Math.min(...onCallHours);
        const maxOnCall = Math.max(...onCallHours);
        const onCallBefore = minOnCall < shiftStart;
        const onCallAfter = maxOnCall > shiftEnd;

        if (onCallBefore && onCallAfter) return 'OC_' + matchedShiftName + '_OC';
        if (onCallBefore) return 'OC_' + matchedShiftName;
        if (onCallAfter) return matchedShiftName + '_OC';
    }

    return matchedShiftName;
}

/**
 * Get CSS class for activity display
 */
function getActivityClass(activity) {
    const map = {
        'D': 'd', 'S': 's', 'OnCall': 'oncall', 'OC': 'oncall', 'Leave': 'leave',
        'ShiftA': 'shifta', 'ShiftB': 'shiftb', 'ShiftC': 'shiftc', 'ShiftD': 'shiftd', 'ShiftE': 'shifte',
        'ShiftA_M': 'shifta', 'ShiftB_M': 'shiftb', 'ShiftC_M': 'shiftc', 'ShiftD_M': 'shiftd', 'ShiftE_M': 'shifte',
        'ShiftA_S': 'shifta', 'ShiftB_S': 'shiftb', 'ShiftC_S': 'shiftc', 'ShiftD_S': 'shiftd', 'ShiftE_S': 'shifte',
        'ShiftA_D': 'shifta', 'ShiftB_D': 'shiftb', 'ShiftC_D': 'shiftc', 'ShiftD_D': 'shiftd', 'ShiftE_D': 'shifte',
        'Off': 'off', 'V': 'v', 'Vacation': 'v', 'H': 'h', 'Holiday': 'h'
    };
    if (map[activity] !== undefined) return map[activity];

    const ocPre  = /^OC_(Shift[A-E](_[MSD])?)$/.exec(activity);
    const ocSuf  = /^(Shift[A-E](_[MSD])?)_OC$/.exec(activity);
    const ocBoth = /^OC_(Shift[A-E](_[MSD])?)_OC$/.exec(activity);
    if (ocPre)  return getActivityClass(ocPre[1])  + ' oc-pre';
    if (ocSuf)  return getActivityClass(ocSuf[1])  + ' oc-suf';
    if (ocBoth) return getActivityClass(ocBoth[1]) + ' oc-both';
    return 'off';
}

/**
 * Get activity display text
 */
function getActivityDisplay(activity) {
    const map = {
        'D': 'D', 'S': 'S', 'OnCall': 'OC', 'OC': 'OC', 'Leave': 'L',
        'ShiftA': 'A', 'ShiftB': 'B', 'ShiftC': 'C', 'ShiftD': 'D', 'ShiftE': 'E',
        'ShiftA_M': 'A', 'ShiftB_M': 'B', 'ShiftC_M': 'C', 'ShiftD_M': 'D', 'ShiftE_M': 'E',
        'ShiftA_S': 'A', 'ShiftB_S': 'B', 'ShiftC_S': 'C', 'ShiftD_S': 'D', 'ShiftE_S': 'E',
        'ShiftA_D': 'A', 'ShiftB_D': 'B', 'ShiftC_D': 'C', 'ShiftD_D': 'D', 'ShiftE_D': 'E',
        'Off': 'Off', 'V': 'V', 'Vacation': 'V', 'H': 'H', 'Holiday': 'H'
    };
    if (map[activity] !== undefined) return map[activity];
    if (!activity) return '-';

    const ocPre  = /^OC_(Shift[A-E](_[MSD])?)$/.exec(activity);
    const ocSuf  = /^(Shift[A-E](_[MSD])?)_OC$/.exec(activity);
    const ocBoth = /^OC_(Shift[A-E](_[MSD])?)_OC$/.exec(activity);
    if (ocPre)  return '< ' + getActivityDisplay(ocPre[1]);
    if (ocSuf)  return getActivityDisplay(ocSuf[1]) + ' >';
    if (ocBoth) return '< ' + getActivityDisplay(ocBoth[1]) + ' >';
    return activity;
}

