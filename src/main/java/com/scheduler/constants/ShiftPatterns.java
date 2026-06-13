package com.scheduler.constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftPatterns {
    public static final Map<String, List<String>> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put("ShiftA", Arrays.asList("Off","Off","S","S","S","S","S","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftB", Arrays.asList("Off","Off","Off","S","S","S","S","S","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftC", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftD", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","S","S","S","S","D","D","D","D","S","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftE", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","D","D","D","D","S","S","S","S","S","Off","Off","Off","Off","Off","Off"));

        // 24-hour patterns
        PATTERNS.put("D", Arrays.asList(new String[24]));
        PATTERNS.put("S", Arrays.asList(new String[24]));
        PATTERNS.put("OnCall", Arrays.asList(new String[24]));
        PATTERNS.put("Leave", Arrays.asList(new String[24]));
        PATTERNS.put("H", Arrays.asList(new String[24]));
        PATTERNS.put("V", Arrays.asList(new String[24]));

        // Helper to populate 24-hour constants correctly
        fill24h("D", "D");
        fill24h("S", "S");
        fill24h("OnCall", "OnCall");
        fill24h("Leave", "Leave");
        fill24h("H", "Holiday");
        fill24h("V", "Vacation");

        // Generate OnCall combinations for ShiftA-E
        for (String shift : Arrays.asList("ShiftA", "ShiftB", "ShiftC", "ShiftD", "ShiftE")) {
            List<String> base = PATTERNS.get(shift);
            
            // OC_Shift
            String[] ocPre = new String[24];
            for (int i = 0; i < 24; i++) ocPre[i] = i < 2 ? "OnCall" : base.get(i);
            PATTERNS.put("OC_" + shift, Arrays.asList(ocPre));

            // Shift_OC
            String[] ocSuf = new String[24];
            for (int i = 0; i < 24; i++) ocSuf[i] = i >= 19 ? "OnCall" : base.get(i);
            PATTERNS.put(shift + "_OC", Arrays.asList(ocSuf));

            // OC_Shift_OC
            String[] ocBoth = new String[24];
            for (int i = 0; i < 24; i++) ocBoth[i] = (i < 2 || i >= 19) ? "OnCall" : base.get(i);
            PATTERNS.put("OC_" + shift + "_OC", Arrays.asList(ocBoth));
        }
    }

    private static void fill24h(String key, String activity) {
        String[] pattern = new String[24];
        Arrays.fill(pattern, activity);
        PATTERNS.put(key, Arrays.asList(pattern));
    }

    public static String findMatchingPattern(List<String> hours) {
        if (hours == null || hours.size() != 24) return null;
        for (Map.Entry<String, List<String>> entry : PATTERNS.entrySet()) {
            if (entry.getValue().equals(hours)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
