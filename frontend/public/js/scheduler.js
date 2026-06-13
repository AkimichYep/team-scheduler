// Global error handler for debugging
window.onerror = function(message, source, lineno, colno, error) {
    console.error('[GLOBAL_ERROR]', message, 'at', source, 'line:', lineno, ':', colno, error);
    // Also show a toast so user knows something is wrong
    if (typeof showToast === 'function') {
        showToast('JS Error: ' + message, 'error');
    }
    return false;
};
window.onunhandledrejection = function(event) {
    console.error('[UNHANDLED_REJECTION]', event.reason);
};

// CURRENT_USER_ID should be set by the EJS file before loading this script
window.scheduleData = {};
let currentDate = new Date();
window.currentView = 'month';
let selectedDateForModal = null;
let selectedHourForModal = null; // Initialize for hourly view

         // Toast aggregation system
         let pendingSaves = 0;
         let toastTimeout = null;
         const TOAST_DEBOUNCE_DELAY = 5000; // Wait 5 seconds to aggregate multiple saves
         const TOAST_DISPLAY_DELAY = 7000; // Display toast for 7 seconds

         // Shift patterns: maps shift type -> array of 24 activities (index = hour)
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

         // Resolve actual activity for a given shift type + hour
         function resolveHourlyActivity(activity, hour) {
             if (SHIFT_PATTERNS[activity]) {
                 return SHIFT_PATTERNS[activity][hour] || 'Off';
             }
             return activity || 'Off';
         }

         // Generate OnCall combinations for all patterns
         Object.keys(SHIFT_PATTERNS).forEach(key => {
             if (key.startsWith('Shift')) {
                 const base = SHIFT_PATTERNS[key];
                 SHIFT_PATTERNS['OC_' + key] = base.map((a, i) => i < 2 ? 'OnCall' : a);
                 SHIFT_PATTERNS[key + '_OC'] = base.map((a, i) => i >= 19 ? 'OnCall' : a);
                 SHIFT_PATTERNS['OC_' + key + '_OC'] = base.map((a, i) => (i < 2 || i >= 19) ? 'OnCall' : a);
             }
         });

          function isShiftType(activity) {
              return activity in SHIFT_PATTERNS;
          }


          function activityPriority(activity) {
             const p = { 'D': 10, 'S': 9, 'OnCall': 8, 'Development': 10, 'Support': 9, 'ShiftA': 5, 'ShiftB': 5, 'ShiftC': 5, 'ShiftD': 5, 'ShiftE': 5, 'Leave': 4, 'V': 3, 'Vacation': 3, 'H': 2, 'Holiday': 2, 'Off': 0 };
             return p[activity] !== undefined ? p[activity] : 1;
         }

         function getDominantActivity(entries) {
            if (!entries || entries.length === 0) return 'Off';
            
            // Normalize activities for priority calculation
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

          // Given an array of hourly entries for a day, return the one with highest priority activity
          function getDominantEntry(entries) {
              if (!entries || entries.length === 0) return null;
              // Group by activity and count hours
              const counts = {};
              entries.forEach(e => {
                  counts[e.activity] = (counts[e.activity] || 0) + 1;
              });

              // Check if any activity spans 24 hours (for our new shift patterns)
              for (const [activity, count] of Object.entries(counts)) {
                  if (isShiftType(activity)) {
                      // If it's a shift type, it might be grouped (count 1) or hourly (count 24)
                      if (count === 24 || entries.some(e => e.hourRange === '0-23' && e.activity === activity)) {
                          return Object.assign({}, entries[0], { activity: activity });
                      }
                  }
              }

              return entries.reduce((best, e) => {
                  return activityPriority(e.activity) > activityPriority(best.activity) ? e : best;
              });
          }

          // Check if 24 hourly entries exactly match a known shift pattern
          function detectShiftPattern(entries) {
              if (!entries || entries.length === 0) return null;
              
              // NEW: If backend already provided a shift pattern name for 0-23
              const groupedPattern = entries.find(e => e.hourRange === '0-23' && isShiftType(e.activity));
              if (groupedPattern) return groupedPattern.activity;

              const hourMap = new Array(24).fill('Off');
              entries.forEach(e => {
                  if (e.hourRange !== undefined) {
                      const range = e.hourRange.split('-');
                      const start = parseInt(range[0]);
                      const end = range.length > 1 ? parseInt(range[1]) : start;
                      for (let h = start; h <= end; h++) {
                          if (h >= 0 && h < 24) hourMap[h] = e.activity;
                      }
                  } else if (e.hourOfDay !== undefined && e.hourOfDay >= 0 && e.hourOfDay < 24) {
                      hourMap[e.hourOfDay] = e.activity;
                  }
              });
              for (const [shiftName, pattern] of Object.entries(SHIFT_PATTERNS)) {
                  if (pattern.every((act, i) => act === hourMap[i])) {
                      return shiftName;
                  }
              }
              return null;
          }

          // Get the best single-activity label for a day: prefers recognized shift pattern, then dominant entry
          function getDisplayEntryForDay(entries) {
              if (!entries || entries.length === 0) return null;
              const shiftName = detectShiftPattern(entries);
              if (shiftName) {
                  return Object.assign({}, entries[0], { activity: shiftName });
              }
              return getDominantEntry(entries);
          }

         // Expand a shift type and save it to backend as a single activity
         function expandAndSaveShift(dateStr, shiftType, notes) {
             // Send single request with shift name
             return fetch(`/api/proxy/schedule/${CURRENT_USER_ID}`, {
                 method: 'POST',
                 headers: { 'Content-Type': 'application/json' },
                 body: JSON.stringify({
                     date: dateStr,
                     activity: shiftType,
                     notes: notes || ''
                 })
             }).then(r => r.json()).then(dayData => {
                // Update local schedule data with fetched day
                const list = Array.isArray(dayData) ? dayData : [dayData];
                
                // Important: Unpack grouped data into hourly map for this day first
                list.forEach(entry => {
                    if (entry.hourRange !== undefined) {
                        const range = entry.hourRange.split('-');
                        const start = parseInt(range[0]);
                        const end = range.length > 1 ? parseInt(range[1]) : start;
                        for (let h = start; h <= end; h++) {
                            window.scheduleData[`${entry.date}-${h}`] = entry;
                        }
                    } else if (entry.hourOfDay !== undefined) {
                        window.scheduleData[`${entry.date}-${entry.hourOfDay}`] = entry;
                    }
                });

                window.scheduleData[dateStr] = getDisplayEntryForDay(list) || list[0];

                 pendingSaves++;
                 if (toastTimeout) clearTimeout(toastTimeout);
                 toastTimeout = setTimeout(() => {
                     if (pendingSaves > 0) {
                         showToast(`Shift ${shiftType.replace('Shift','')} saved for ${dateStr}`);
                         pendingSaves = 0;
                     }
                 }, TOAST_DEBOUNCE_DELAY);

                 // Re-render only the current view
                 refreshCurrentView();
             }).catch(err => {
                 console.error('Error saving shift:', err);
                 showToast('Error saving shift!', 'error');
             });
         }

        // Initialize on page load
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Escape') {
                closeModal();
            }
        });

        document.addEventListener('DOMContentLoaded', function() {
            console.log('[DEBUG_LOG] DOMContentLoaded triggered');
            try {
                // Ensure currentDate is valid
                if (!currentDate || isNaN(currentDate.getTime())) {
                    currentDate = new Date();
                }
                console.log('[DEBUG_LOG] Initializing view: month. Current date:', currentDate);
                switchView('month');
            } catch (err) {
                console.error('[DEBUG_LOG] Error during initialization:', err);
            }
        });

        window.switchView = function(viewName) {
            console.log('[DEBUG_LOG] switchView called with:', viewName);
            window.currentView = viewName;
            // Hide all views
            document.querySelectorAll('.view-content').forEach(view => {
                view.style.display = 'none';
            });

            // Update buttons
            document.querySelectorAll('.view-btn').forEach(btn => {
                const onclick = btn.getAttribute('onclick') || '';
                if (onclick.includes(`'${viewName}'`)) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });

            // Show selected view
            const viewElement = document.getElementById(viewName + 'View');
            if (viewElement) {
                viewElement.style.display = 'block';
            }

            // Load data for the view
            if (viewName === 'month') {
                loadMonth();
            } else if (viewName === 'year') {
                loadYear();
            }
        }

        window.refreshCurrentView = function() {
            console.log('[DEBUG_LOG] refreshCurrentView called for view:', window.currentView);
            if (window.currentView === 'month') {
                renderMonth();
            } else if (window.currentView === 'year') {
                renderYear();
            } else {
                console.warn('[DEBUG_LOG] refreshCurrentView called with unknown view:', window.currentView);
            }
        }

        function loadMonth() {
            console.log('[DEBUG_LOG] loadMonth called');
            const year = currentDate.getFullYear();
            const month = currentDate.getMonth() + 1;
            const url = `/api/proxy/schedule/month/${CURRENT_USER_ID}/${year}/${month}`;
            console.log('[DEBUG_LOG] Fetching month schedule from:', url);

            fetch(url)
                .then(res => {
                    console.log('[DEBUG_LOG] Response status:', res.status);
                    return res.json();
                })
                .then(data => {
                    console.log('[DEBUG_LOG] Received month data, count:', data.length);
                    window.scheduleData = {};
                    // Group entries by date, store per-hour AND dominant per-day
                    const byDate = {};
                    data.forEach(entry => {
                        // Store grouped entries into hourly map
                        if (entry.hourRange !== undefined) {
                            const range = entry.hourRange.split('-');
                            const start = parseInt(range[0]);
                            const end = range.length > 1 ? parseInt(range[1]) : start;
                            for (let h = start; h <= end; h++) {
                                window.scheduleData[`${entry.date}-${h}`] = entry;
                            }
                        } else if (entry.hourOfDay !== undefined) {
                            window.scheduleData[`${entry.date}-${entry.hourOfDay}`] = entry;
                        }

                        // Group for dominant computation
                        if (!byDate[entry.date]) byDate[entry.date] = [];
                        byDate[entry.date].push(entry);
                    });
                    // Store display entry per date for month/year/week/day views (shift pattern detection first)
                    Object.keys(byDate).forEach(dateStr => {
                        window.scheduleData[dateStr] = getDisplayEntryForDay(byDate[dateStr]) || byDate[dateStr][0];
                    });
                    renderMonth();
                })
                .catch(err => console.error('Error fetching month schedule:', err));
        }

         function renderMonth() {
             console.log('[DEBUG_LOG] renderMonth called');
             const year = currentDate.getFullYear();
             const month = currentDate.getMonth();
             const firstDay = new Date(year, month, 1);
             const lastDay = new Date(year, month + 1, 0);
             const prevLastDay = new Date(year, month, 0).getDate();
             const startDate = firstDay.getDay();
             const endDate = lastDay.getDate();

             const monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
                               'July', 'August', 'September', 'October', 'November', 'December'];
             const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

             document.getElementById('currentMonth').textContent = `${monthNames[month]} ${year}`;

             const calendarContainer = document.getElementById('monthCalendar').parentElement;
             const headerDiv = document.getElementById('monthCalendarHeader') || document.createElement('div');
             headerDiv.id = 'monthCalendarHeader';
             headerDiv.className = 'calendar-header';
             headerDiv.innerHTML = dayNames.map(day => `<div class="calendar-header-cell">${day}</div>`).join('');

             if (!document.getElementById('monthCalendarHeader')) {
                 document.getElementById('monthCalendar').parentElement.insertBefore(headerDiv, document.getElementById('monthCalendar'));
             }

             const calendar = document.getElementById('monthCalendar');
             calendar.innerHTML = '';

             // Previous month's days
             for (let i = startDate - 1; i >= 0; i--) {
                 const dayDiv = document.createElement('div');
                 dayDiv.className = 'day-cell';
                 dayDiv.innerHTML = `<div class="day-number">${prevLastDay - i}</div>`;
                 dayDiv.style.opacity = '0.3';
                 calendar.appendChild(dayDiv);
             }

              // Current month's days
              for (let day = 1; day <= endDate; day++) {
                  const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                   const entry = scheduleData[dateStr] || { activity: 'D', date: dateStr, isOnCall: false };

                  const dayDiv = document.createElement('div');
                  const dayOfWeek = new Date(year, month, day).getDay();
                  const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;

                  let activityClass = getActivityClass(entry.activity);
                  const oncallClass = entry.isOnCall ? 'oncall' : '';
                  dayDiv.className = `day-cell ${isWeekend ? 'weekend' : ''} ${activityClass} ${oncallClass}`;

                  const displayActivity = getActivityDisplay(entry.activity);
                  dayDiv.innerHTML = `
                      <div class="day-number">${day}</div>
                      <div class="day-activity">${displayActivity}</div>
                  `;

                   dayDiv.onclick = () => openModal(dateStr, entry.activity, entry.notes || '');
                  calendar.appendChild(dayDiv);
              }

             // Next month's days
             const totalCells = calendar.children.length;
             const remaining = 42 - totalCells;
             for (let day = 1; day <= remaining; day++) {
                 const dayDiv = document.createElement('div');
                 dayDiv.className = 'day-cell';
                 dayDiv.innerHTML = `<div class="day-number">${day}</div>`;
                 dayDiv.style.opacity = '0.3';
                 calendar.appendChild(dayDiv);
             }
         }

          function getActivityClass(activity) {
              switch(activity) {
                  case 'D': return 'd';
                  case 'S': return 's';
                  case 'OnCall': return 'oncall';
                  case 'Leave': return 'leave';
                  case 'ShiftA': return 'shifta';
                  case 'ShiftB': return 'shiftb';
                  case 'ShiftC': return 'shiftc';
                  case 'ShiftD': return 'shiftd';
                  case 'ShiftE': return 'shifte';
                  case 'Off': return 'off';
                  case 'Vacation': return 'v';
                  case 'H': case 'Holiday': return 'h';
                  case 'Development': return 'd';
                  case 'Support': return 's';
                  default: {
                      // OC combo: OC_ShiftX, ShiftX_OC, OC_ShiftX_OC
                      const ocPre  = /^OC_(Shift[A-E])$/.exec(activity);
                      const ocSuf  = /^(Shift[A-E])_OC$/.exec(activity);
                      const ocBoth = /^OC_(Shift[A-E])_OC$/.exec(activity);
                      if (ocPre)  return getActivityClass(ocPre[1])  + ' oc-pre';
                      if (ocSuf)  return getActivityClass(ocSuf[1])  + ' oc-suf';
                      if (ocBoth) return getActivityClass(ocBoth[1]) + ' oc-both';
                      return 'off';
                  }
              }
          }

          function getActivityDisplay(activity) {
              const map = {
                  'D': 'D', 'S': 'S', 'OnCall': 'OC', 'Leave': 'L',
                  'ShiftA': 'A', 'ShiftB': 'B', 'ShiftC': 'C', 'ShiftD': 'D', 'ShiftE': 'E',
                  'ShiftA_M': 'A', 'ShiftB_M': 'B', 'ShiftC_M': 'C', 'ShiftD_M': 'D', 'ShiftE_M': 'E',
                  'ShiftA_S': 'A', 'ShiftB_S': 'B', 'ShiftC_S': 'C', 'ShiftD_S': 'D', 'ShiftE_S': 'E',
                  'ShiftA_D': 'A', 'ShiftB_D': 'B', 'ShiftC_D': 'C', 'ShiftD_D': 'D', 'ShiftE_D': 'E',
                  'ShiftA_M_OC': 'A >', 'ShiftB_M_OC': 'B >', 'ShiftC_M_OC': 'C >', 'ShiftD_M_OC': 'D >', 'ShiftE_M_OC': 'E >',
                  'ShiftA_S_OC': 'A >', 'ShiftB_S_OC': 'B >', 'ShiftC_S_OC': 'C >', 'ShiftD_S_OC': 'D >', 'ShiftE_S_OC': 'E >',
                  'ShiftA_D_OC': 'A >', 'ShiftB_D_OC': 'B >', 'ShiftC_D_OC': 'C >', 'ShiftD_D_OC': 'D >', 'ShiftE_D_OC': 'E >',
                  'OC_ShiftA_M': '< A', 'OC_ShiftB_M': '< B', 'OC_ShiftC_M': '< C', 'OC_ShiftD_M': '< D', 'OC_ShiftE_M': '< E',
                  'OC_ShiftA_S': '< A', 'OC_ShiftB_S': '< B', 'OC_ShiftC_S': '< C', 'OC_ShiftD_S': '< D', 'OC_ShiftE_S': '< E',
                  'OC_ShiftA_D': '< A', 'OC_ShiftB_D': '< B', 'OC_ShiftC_D': '< C', 'OC_ShiftD_D': '< D', 'OC_ShiftE_D': '< E',
                  'OC_ShiftA_M_OC': '< A >', 'OC_ShiftB_M_OC': '< B >', 'OC_ShiftC_M_OC': '< C >', 'OC_ShiftD_M_OC': '< D >', 'OC_ShiftE_M_OC': '< E >',
                  'OC_ShiftA_S_OC': '< A >', 'OC_ShiftB_S_OC': '< B >', 'OC_ShiftC_S_OC': '< C >', 'OC_ShiftD_S_OC': '< D >', 'OC_ShiftE_S_OC': '< E >',
                  'OC_ShiftA_D_OC': '< A >', 'OC_ShiftB_D_OC': '< B >', 'OC_ShiftC_D_OC': '< C >', 'OC_ShiftD_D_OC': '< D >', 'OC_ShiftE_D_OC': '< E >',
                  'Off': 'Off', 'Vacation': 'V', 'Holiday': 'H', 'Development': 'D', 'Support': 'S'
              };
              
              if (map[activity]) return map[activity];
              
              // OC combo display: < A, A >, < A >
              const ocPre  = /^OC_(Shift[A-E](_[MSD])?)$/.exec(activity);
              const ocSuf  = /^(Shift[A-E](_[MSD])?)_OC$/.exec(activity);
              const ocBoth = /^OC_(Shift[A-E](_[MSD])?)_OC$/.exec(activity);
              
              if (ocPre)  return '< ' + getActivityDisplay(ocPre[1]);
              if (ocSuf)  return getActivityDisplay(ocSuf[1]) + ' >';
              if (ocBoth) return '< ' + getActivityDisplay(ocBoth[1]) + ' >';
              
              if (!activity) return '-';
              return activity;
          }



        function loadYear() {
            const year = currentDate.getFullYear();

            fetch(`/api/proxy/schedule/year/${CURRENT_USER_ID}/${year}`)
                .then(res => res.json())
                .then(data => {
                    window.scheduleData = {};
                    const byDate = {};
                    data.forEach(entry => {
                        // Store grouped entries into hourly map
                        if (entry.hourRange !== undefined) {
                            const range = entry.hourRange.split('-');
                            const start = parseInt(range[0]);
                            const end = range.length > 1 ? parseInt(range[1]) : start;
                            for (let h = start; h <= end; h++) {
                                window.scheduleData[`${entry.date}-${h}`] = entry;
                            }
                        } else if (entry.hourOfDay !== undefined) {
                            window.scheduleData[`${entry.date}-${entry.hourOfDay}`] = entry;
                        }

                        if (!byDate[entry.date]) byDate[entry.date] = [];
                        byDate[entry.date].push(entry);
                    });
                     Object.keys(byDate).forEach(d => {
                         window.scheduleData[d] = getDisplayEntryForDay(byDate[d]) || byDate[d][0];
                     });
                     renderYear();
                })
                .catch(err => console.error('Error fetching year schedule:', err));
        }

        function renderYear() {
             const year = currentDate.getFullYear();
             document.getElementById('currentYear').textContent = `${year}`;

             const monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
                               'July', 'August', 'September', 'October', 'November', 'December'];

             const yearGrid = document.getElementById('yearGrid');
             yearGrid.innerHTML = '';

             for (let month = 0; month < 12; month++) {
                 const monthCard = document.createElement('div');
                 monthCard.className = 'month-card';

                 const monthTitle = document.createElement('div');
                 monthTitle.className = 'month-card-title';
                 monthTitle.textContent = monthNames[month];
                 monthCard.appendChild(monthTitle);

                 const daysGrid = document.createElement('div');
                 daysGrid.className = 'month-card-days';

                 const firstDay = new Date(year, month, 1);
                 const lastDay = new Date(year, month + 1, 0);
                 const startDate = firstDay.getDay();

                 // Add empty cells for days before month starts
                 for (let i = 0; i < startDate; i++) {
                     const emptyDay = document.createElement('div');
                     daysGrid.appendChild(emptyDay);
                 }

                   // Add days of month
                   for (let day = 1; day <= lastDay.getDate(); day++) {
                       const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                       const entry = scheduleData[dateStr];
                       const activity = entry ? entry.activity : 'Off';

                       let activityClass = getActivityClass(activity);
                       const oncallClass = entry?.isOnCall ? 'oncall' : '';

                       const dayCell = document.createElement('div');
                       dayCell.className = `month-card-day ${activityClass} ${oncallClass}`;
                       dayCell.textContent = day;
                       dayCell.style.cursor = 'pointer';
                       dayCell.style.transition = 'all 0.2s';
                       dayCell.style.position = 'relative';
                       dayCell.onclick = () => openModal(dateStr, entry ? entry.activity : 'D', entry ? (entry.notes || '') : '');
                       dayCell.onmouseover = function() {
                           this.style.opacity = '0.8';
                           this.style.transform = 'scale(1.05)';
                       };
                       dayCell.onmouseout = function() {
                           this.style.opacity = '1';
                           this.style.transform = 'scale(1)';
                       };
                       daysGrid.appendChild(dayCell);
                   }

                 monthCard.appendChild(daysGrid);
                 yearGrid.appendChild(monthCard);
             }
         }

        function goToMonth(month) {
            currentDate = new Date(currentDate.getFullYear(), month, 1);
            switchView('month');
        }

        function previousMonth() {
            currentDate.setMonth(currentDate.getMonth() - 1);
            loadMonth();
        }

        function nextMonth() {
            currentDate.setMonth(currentDate.getMonth() + 1);
            loadMonth();
        }

        function previousYear() {
            currentDate.setFullYear(currentDate.getFullYear() - 1);
            loadYear();
        }

        function nextYear() {
            currentDate.setFullYear(currentDate.getFullYear() + 1);
            loadYear();
        }

           function openModal(dateStr, activity, notes) {
                selectedDateForModal = dateStr;
                document.getElementById('modalDate').value = dateStr;
                const headerDate = document.getElementById('headerDate');
                if (headerDate) headerDate.textContent = dateStr;
                document.getElementById('modalActivity').value = activity;
                document.getElementById('modalNotes').value = notes || '';

                // Reset shift-specific inputs
                document.getElementById('selectedShift').value = '';
                document.getElementById('selectedVariant').value = '';
                document.getElementById('ocBefore').checked = false;
                document.getElementById('ocAfter').checked = false;
                
                // If the activity is a shift pattern, try to parse it
                if (activity && activity.includes('Shift')) {
                    let base = activity;
                    if (base.startsWith('OC_')) {
                        document.getElementById('ocBefore').checked = true;
                        base = base.replace('OC_', '');
                    }
                    if (base.endsWith('_OC')) {
                        document.getElementById('ocAfter').checked = true;
                        base = base.replace('_OC', '');
                    }
                    
                    let variant = "";
                    if (base.endsWith('_S')) {
                        variant = "_S";
                        base = base.replace('_S', '');
                    } else if (base.endsWith('_D')) {
                        variant = "_D";
                        base = base.replace('_D', '');
                    } else if (base.endsWith('_M')) {
                        variant = "_M";
                        base = base.replace('_M', '');
                    }
                    
                    document.getElementById('selectedShift').value = base;
                    document.getElementById('selectedVariant').value = variant;
                }

                updateShiftSelection(); // This will also handle button active states

                document.getElementById('editModal').style.display = 'block';
            }

           function selectActivityType(type) {
               document.getElementById('modalActivity').value = type;
               document.getElementById('selectedShift').value = ''; // Reset shift selection

               // Update button styles
               const buttons = document.querySelectorAll('.activity-btn');
               buttons.forEach(btn => btn.classList.remove('active'));

               let typeClass = getActivityClass(type);
               const classSel2 = typeClass.trim().split(/\s+/).map(c => '.' + c).join('');
               const activeBtn = document.querySelector(`.activity-btn${classSel2}`);
               if (activeBtn) {
                   activeBtn.classList.add('active');
               }

               // Clear preview when selecting non-shift type
               const preview = document.getElementById('shiftPreview');
               if (preview) preview.textContent = "Select a shift to see hours.";
           }

          function closeModal() {
              document.getElementById('editModal').style.display = 'none';
              selectedHourForModal = null;  // Reset hour when closing modal
              // Reset shift selection
              document.getElementById('selectedShift').value = '';
              document.getElementById('ocBefore').checked = false;
              document.getElementById('ocAfter').checked = false;
          }

          function selectShift(shiftType, variant = "_M") {
              const baseType = shiftType.replace(/_S|_D$/, '');
              if (['ShiftA', 'ShiftB', 'ShiftC', 'ShiftD', 'ShiftE'].includes(baseType)) {
                  document.getElementById('selectedShift').value = baseType;
                  document.getElementById('selectedVariant').value = variant;
              } else {
                  // If it's a basic activity (D, S, etc.), selectActivityType handles it
                  selectActivityType(shiftType);
                  return;
              }
              updateShiftSelection();
          }

          function renderShiftRows() {
              const columns = {
                  '_M': document.querySelector('#colMixed .shift-col-rows'),
                  '_S': document.querySelector('#colSupport .shift-col-rows'),
                  '_D': document.querySelector('#colDev .shift-col-rows')
              };
              
              if (!columns['_M']) return;
              
              const selectedShift = document.getElementById('selectedShift').value;
              let selectedVariant = document.getElementById('selectedVariant').value;
              if (selectedShift && !selectedVariant) selectedVariant = '_M';
              
              const ocBefore = document.getElementById('ocBefore').checked;
              const ocAfter = document.getElementById('ocAfter').checked;
              
              const shifts = ['ShiftA', 'ShiftB', 'ShiftC', 'ShiftD', 'ShiftE'];
              const variants = ['_M', '_S', '_D'];
              
              variants.forEach(v => {
                  let html = '';
                  shifts.forEach(shiftId => {
                      let patternKey = shiftId + v;
                      if (ocBefore && ocAfter) patternKey = `OC_${patternKey}_OC`;
                      else if (ocBefore) patternKey = `OC_${patternKey}`;
                      else if (ocAfter) patternKey = `${patternKey}_OC`;
                      
                      const pattern = SHIFT_PATTERNS[patternKey];
                      if (!pattern) return;
                      
                      const isActive = selectedShift === shiftId && selectedVariant === v;
                      
                      // Generate pseudographics
                      const visual = pattern.map(act => {
                          if (act === 'S') return '<span style="color:#2196F3; font-weight:bold;">S</span>';
                          if (act === 'D') return '<span style="color:#4CAF50; font-weight:bold;">D</span>';
                          if (act === 'OnCall') return '<span style="color:#FF9800; font-weight:bold;">O</span>';
                          return '<span style="color:#ccc;">.</span>';
                      }).join('');
                      
                      html += `
                          <div class="shift-row ${isActive ? 'active' : ''}" 
                               onclick="selectShift('${shiftId}', '${v}')" 
                               style="display: flex; align-items: center; gap: 4px; padding: 4px 6px; cursor: pointer; border-radius: 4px; border: 1px solid ${isActive ? '#2196F3' : '#eee'}; background: ${isActive ? '#e3f2fd' : 'white'}; font-family: monospace; font-size: 10px; overflow: hidden; white-space: nowrap;">
                              <div style="min-width: 15px; font-weight: bold; color: #333;">${shiftId.replace('Shift','')}</div>
                              <div style="flex: 1; letter-spacing: 1px;">${visual}</div>
                          </div>
                      `;
                  });
                  columns[v].innerHTML = html;
              });
          }

          function updateShiftSelection() {
              const selectedShift = document.getElementById('selectedShift').value;
              const variant = document.getElementById('selectedVariant').value;
              const ocBefore = document.getElementById('ocBefore').checked;
              const ocAfter = document.getElementById('ocAfter').checked;

              let baseWithVariant = selectedShift ? (selectedShift + (variant || "_M")) : '';
              let activity = baseWithVariant;
              
              if (selectedShift) {
                  document.getElementById('modalActivity').value = activity;
              }

              // Handle OnCall checkboxes
              if (baseWithVariant) {
                  if (ocBefore && ocAfter) {
                      activity = `OC_${baseWithVariant}_OC`;
                  } else if (ocBefore) {
                      activity = `OC_${baseWithVariant}`;
                  } else if (ocAfter) {
                      activity = `${baseWithVariant}_OC`;
                  }
              } else {
                  // If no shift selected but OnCall checked, it might be 24h OnCall
                  // but usually handled by selectActivityType('OnCall')
              }

              // Update active state of buttons for basic activities
              document.querySelectorAll('.activity-buttons-container .activity-btn').forEach(btn => {
                  btn.classList.remove('active');
              });
              
              if (!selectedShift) {
                  const currentActivity = document.getElementById('modalActivity').value;
                  const btn = Array.from(document.querySelectorAll('.activity-btn')).find(b => {
                      const onClickAttr = b.getAttribute('onclick') || "";
                      return b.textContent.trim() === currentActivity || 
                             onClickAttr.includes(`'${currentActivity}'`) ||
                             onClickAttr.includes(`"${currentActivity}"`);
                  });
                  if (btn) btn.classList.add('active');
              }

              // Render shift rows to reflect selection and OnCall changes
              renderShiftRows();

              // Update preview
              const preview = document.getElementById('shiftPreview');
              if (activity && SHIFT_PATTERNS[activity]) {
                  const pattern = SHIFT_PATTERNS[activity];
                  const hours = pattern.map((act, i) => act !== 'Off' ? i : null).filter(h => h !== null);
                  if (hours.length > 0) {
                      const start = hours[0];
                      const end = hours[hours.length - 1] + 1;
                      preview.textContent = `Hours: ${start.toString().padStart(2,'0')}:00 - ${end.toString().padStart(2,'0')}:00 (${hours.length}h)`;
                  } else {
                      preview.textContent = "Off (0h)";
                  }
              } else {
                  preview.textContent = "Select a shift to see hours.";
              }

              // Update hidden input
              document.getElementById('modalActivity').value = activity || document.getElementById('modalActivity').value;
          }

          function toggleAdvancedOptions() {
              const advancedOptions = document.getElementById('advancedOptions');
              const toggleBtn = document.getElementById('advancedToggleBtn');

              if (advancedOptions.style.display === 'none') {
                  advancedOptions.style.display = 'block';
                  toggleBtn.textContent = '▼ Advanced Options (Shifts & OnCall)';
              } else {
                  advancedOptions.style.display = 'none';
                  toggleBtn.textContent = '▶ Advanced Options (Shifts & OnCall)';
              }
          }

           function fillNext5Days() {
               const activity = document.getElementById('modalActivity').value;
               const notes = document.getElementById('modalNotes').value;
               const startDate = new Date(selectedDateForModal);

               if (isShiftType(activity)) {
                   // For shift types, batch all 5 days with single request per day
                   const promises = [];

                   // Save current day
                   promises.push(
                       fetch(`/api/proxy/schedule/${CURRENT_USER_ID}`, {
                           method: 'POST',
                           headers: { 'Content-Type': 'application/json' },
                           body: JSON.stringify({
                               date: selectedDateForModal,
                               activity: activity,
                               notes: notes || ''
                           })
                       }).then(r => r.json())
                   );

                   // Save next 4 days
                   for (let i = 1; i < 5; i++) {
                       const nextDate = new Date(startDate);
                       nextDate.setDate(nextDate.getDate() + i);
                       const nextDateStr = formatDate(nextDate);
                       promises.push(
                           fetch(`/api/proxy/schedule/${CURRENT_USER_ID}`, {
                               method: 'POST',
                               headers: { 'Content-Type': 'application/json' },
                               body: JSON.stringify({
                                   date: nextDateStr,
                                   activity: activity,
                                   notes: notes || ''
                               })
                           }).then(r => r.json())
                       );
                   }

                   Promise.all(promises).then(results => {
                       results.forEach((dayData, index) => {
                           const dateKey = index === 0 ? selectedDateForModal : formatDate(new Date(new Date(selectedDateForModal).setDate(new Date(selectedDateForModal).getDate() + index)));
                           const list = Array.isArray(dayData) ? dayData : [dayData];
                           window.scheduleData[dateKey] = getDisplayEntryForDay(list) || list[0];
                       });

                       pendingSaves++;
                       if (toastTimeout) clearTimeout(toastTimeout);
                       toastTimeout = setTimeout(() => {
                           if (pendingSaves > 0) {
                               showToast(`Shift ${activity.replace('Shift','')} applied for 5 days`);
                               pendingSaves = 0;
                           }
                       }, TOAST_DEBOUNCE_DELAY);

                       // Re-render current view
                       refreshCurrentView();
                   }).catch(err => {
                       console.error('Error saving shift for 5 days:', err);
                       showToast('Error saving shift!', 'error');
                   });
               } else {
                   // For non-shift activities, use original method
                   saveActivityToBackend(selectedDateForModal, activity, notes);

                   // Save next 4 days
                   for (let i = 1; i < 5; i++) {
                       const nextDate = new Date(startDate);
                       nextDate.setDate(nextDate.getDate() + i);
                       const nextDateStr = formatDate(nextDate);
                       saveActivityToBackend(nextDateStr, activity, notes);
                   }
               }

              closeModal();
              showToast('Filling next 5 days with selected activity...');
          }

           function saveActivityToBackend(dateStr, activity, notes) {
               if (isShiftType(activity)) {
                   return expandAndSaveShift(dateStr, activity, notes);
               }
               return fetch(`/api/proxy/schedule/${CURRENT_USER_ID}`, {
                   method: 'POST',
                   headers: {
                       'Content-Type': 'application/json'
                   },
                   body: JSON.stringify({
                       date: dateStr,
                       activity: activity,
                       isOnCall: activity === 'OnCall',
                       notes: notes || ''
                   })
               })
               .then(res => res.json())
               .then(dayData => {
                   const list = Array.isArray(dayData) ? dayData : [dayData];
                   window.scheduleData[dateStr] = getDisplayEntryForDay(list) || list[0];
                   pendingSaves++;

                   if (toastTimeout) {
                       clearTimeout(toastTimeout);
                   }

                   toastTimeout = setTimeout(() => {
                       if (pendingSaves > 0) {
                           const message = pendingSaves === 1
                               ? '1 entry saved'
                               : `${pendingSaves} entries saved`;
                           showToast(message);
                           pendingSaves = 0;
                       }
                   }, TOAST_DEBOUNCE_DELAY);

                   // Re-render current view
                   refreshCurrentView();
               })
               .catch(err => {
                   console.error('Error saving activity:', err);
                   showToast('Error saving data!', 'error');
               });
           }

           function saveModalChanges() {
               const activity = document.getElementById('modalActivity').value;
               const notes = document.getElementById('modalNotes').value;

               if (isShiftType(activity)) {
                   expandAndSaveShift(selectedDateForModal, activity, notes);
                   closeModal();
                   return;
               }

               // Immediately save to backend
               fetch(`/api/proxy/schedule/${CURRENT_USER_ID}`, {
                   method: 'POST',
                   headers: {
                       'Content-Type': 'application/json'
                   },
                   body: JSON.stringify({
                       date: selectedDateForModal,
                       activity: activity,
                       isOnCall: activity === 'OnCall',
                       notes: notes
                   })
               })
               .then(res => res.json())
               .then(savedEntry => {
                   // Only fetch the updated day
                   return fetch(`/api/proxy/schedule/day/${CURRENT_USER_ID}/hours?date=${selectedDateForModal}`)
                       .then(res => res.json())
                       .then(dayData => {
                           const list = Array.isArray(dayData) ? dayData : [dayData];
                           scheduleData[selectedDateForModal] = getDisplayEntryForDay(list) || list[0];

                           closeModal();

                           // Increment pending saves
                           pendingSaves++;

                           // Clear previous timeout
                           if (toastTimeout) {
                               clearTimeout(toastTimeout);
                           }

                           // Set new debounced toast
                           toastTimeout = setTimeout(() => {
                               if (pendingSaves > 0) {
                                   const message = pendingSaves === 1
                                       ? '1 entry saved'
                                       : `${pendingSaves} entries saved`;
                                   showToast(message);
                                   pendingSaves = 0;
                               }
                           }, TOAST_DEBOUNCE_DELAY);

                           // Re-render current view
                           refreshCurrentView();
                       });
               })
               .catch(err => {
                   console.error('Error saving schedule:', err);
                   alert('Error saving schedule entry');
               });
           }

           function setAsDefaultTemplate(dateStr, activity) {
               // This would set the selected activity as default for the user
               // For now, just set the isDefault flag in the database
               // The backend would handle creating/updating a schedule template based on this
               console.log(`Setting ${activity} on ${dateStr} as default template`);
           }

         function saveSchedule() {
             // This function is deprecated - saving now happens automatically when changes are made
             // Optionally show a toast if there are pending saves
             if (toastTimeout) {
                 clearTimeout(toastTimeout);
                 if (pendingSaves > 0) {
                     const message = pendingSaves === 1
                         ? '1 entry saved'
                         : `${pendingSaves} entries saved`;
                     showToast(message);
                     pendingSaves = 0;
                 }
             }
         }

          function saveActivityEntry(dateStr, activity, notes) {
              if (isShiftType(activity)) {
                  return expandAndSaveShift(dateStr, activity, notes);
              }
              fetch(`/api/proxy/schedule/${CURRENT_USER_ID}`, {
                  method: 'POST',
                  headers: {
                      'Content-Type': 'application/json'
                  },
                  body: JSON.stringify({
                      date: dateStr,
                      activity: activity,
                      isOnCall: activity === 'OnCall',
                      notes: notes || ''
                  })
              })
              .then(res => res.json())
              .then(savedEntry => {
                  // Update local scheduleData with the saved entry
                  scheduleData[dateStr] = savedEntry;

                  // Increment pending saves
                  pendingSaves++;

                  // Clear previous timeout
                  if (toastTimeout) {
                      clearTimeout(toastTimeout);
                  }

                  // Set new debounced toast
                  toastTimeout = setTimeout(() => {
                      if (pendingSaves > 0) {
                          const message = pendingSaves === 1
                              ? '1 entry saved'
                              : `${pendingSaves} entries saved`;
                          showToast(message);
                          pendingSaves = 0;
                      }
                  }, TOAST_DEBOUNCE_DELAY);

                  // Refresh current view to ensure consistency
                  refreshCurrentView();
              })
              .catch(err => {
                  console.error('Error saving activity:', err);
                  showToast('Error saving data!', 'error');
              });
          }

         function showToast(message, type = 'success') {
             // Remove any existing toast
             const existingToast = document.querySelector('.toast-notification');
             if (existingToast) {
                 existingToast.classList.add('hide');
                 setTimeout(() => existingToast.remove(), 300);
             }

             // Create new toast
             const toast = document.createElement('div');
             toast.className = 'toast-notification';
             toast.textContent = message;
             if (type === 'error') {
                 toast.style.backgroundColor = '#f44336';
             }
             document.body.appendChild(toast);

             // Auto remove after 7 seconds
             setTimeout(() => {
                 toast.classList.add('hide');
                 setTimeout(() => toast.remove(), 300);
             }, TOAST_DISPLAY_DELAY);
         }

        function formatDate(date) {
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        }

        // Close modal when clicking outside
        window.onclick = function(event) {
            const modal = document.getElementById('editModal');
            if (event.target === modal) {
                closeModal();
            }
        }


         // Override saveModalChanges to support hourly saves
         const originalSaveModalChanges = saveModalChanges;
         window.saveModalChanges = function() {
             const activity = document.getElementById('modalActivity').value;
             const notes = document.getElementById('modalNotes').value;
             const hour = selectedHourForModal;

             // If a shift type is selected, always expand to full day pattern
             if (isShiftType(activity)) {
                 expandAndSaveShift(selectedDateForModal, activity, notes);
                 closeModal();
                 selectedHourForModal = null;
                 return;
             }

             // Build request body - only include hour if it's defined (hourly view mode)
             const requestBody = {
                 date: selectedDateForModal,
                 activity: activity,
                 isOnCall: activity === 'OnCall',
                 notes: notes
             };

             // Only include hour if we're in hourly edit mode
             if (hour !== undefined && hour !== null) {
                 requestBody.hour = hour;
             }

             fetch(`/api/proxy/schedule/${CURRENT_USER_ID}`, {
                 method: 'POST',
                 headers: {
                     'Content-Type': 'application/json'
                 },
                 body: JSON.stringify(requestBody)
             })
             .then(res => res.json())
             .then(savedEntry => {
                 // Use the correct key based on whether hour is defined
                 const key = hour !== undefined && hour !== null ? `${selectedDateForModal}-${hour}` : selectedDateForModal;
                 scheduleData[key] = savedEntry;
                 closeModal();

                 // Reset selectedHourForModal after saving
                 selectedHourForModal = null;

                 pendingSaves++;

                 if (toastTimeout) {
                     clearTimeout(toastTimeout);
                 }

                 toastTimeout = setTimeout(() => {
                     if (pendingSaves > 0) {
                         const message = pendingSaves === 1
                             ? '1 entry saved'
                             : `${pendingSaves} entries saved`;
                         showToast(message);
                         pendingSaves = 0;
                     }
                 }, TOAST_DEBOUNCE_DELAY);

                 // Reload current view to show changes
                 refreshCurrentView();
             })
             .catch(err => {
                 console.error('Error saving schedule:', err);
                 alert('Error saving schedule entry');
             });
         };

    

