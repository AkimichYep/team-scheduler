package com.scheduler.constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftPatterns {
    public static final Map<String, List<String>> PATTERNS = new HashMap<>();

    static {
        // ShiftA-E Support variants (S)
        PATTERNS.put("ShiftA_S", Arrays.asList("Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftB_S", Arrays.asList("Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftC_S", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftD_S", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftE_S", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","S","S","S","S","Off","Off","Off","Off","Off"));

        // ShiftA-E Development variants (D)
        PATTERNS.put("ShiftA_D", Arrays.asList("Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftB_D", Arrays.asList("Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftC_D", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftD_D", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftE_D", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","D","D","D","D","D","D","D","D","D","Off","Off","Off","Off","Off"));

        // ShiftA-E Mixed variants (S then D)
        PATTERNS.put("ShiftA_M", Arrays.asList("Off","Off","S","S","S","S","S","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftB_M", Arrays.asList("Off","Off","Off","S","S","S","S","S","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftC_M", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","S","S","S","S","S","D","D","D","D","Off","Off","Off","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftD_M", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","Off","S","S","S","S","D","D","D","D","S","Off","Off","Off","Off","Off"));
        PATTERNS.put("ShiftE_M", Arrays.asList("Off","Off","Off","Off","Off","Off","Off","Off","Off","D","D","D","D","S","S","S","S","S","Off","Off","Off","Off","Off","Off"));

        // 24-hour patterns
        PATTERNS.put("D", Arrays.asList(new String[24]));
        PATTERNS.put("S", Arrays.asList(new String[24]));
        PATTERNS.put("OnCall", Arrays.asList(new String[24]));
        PATTERNS.put("Leave", Arrays.asList(new String[24]));
        PATTERNS.put("H", Arrays.asList(new String[24]));
        PATTERNS.put("V", Arrays.asList(new String[24]));
        PATTERNS.put("Off", Arrays.asList(new String[24]));

        // Helper to populate 24-hour constants correctly
        fill24h("D", "D");
        fill24h("S", "S");
        fill24h("OnCall", "OnCall");
        fill24h("Leave", "Leave");
        fill24h("H", "H");
        fill24h("V", "V");
        fill24h("Off", "Off");

        // Generate OnCall combinations for all variants
        List<String> baseShifts = Arrays.asList(
            "ShiftA_M", "ShiftB_M", "ShiftC_M", "ShiftD_M", "ShiftE_M",
            "ShiftA_S", "ShiftB_S", "ShiftC_S", "ShiftD_S", "ShiftE_S",
            "ShiftA_D", "ShiftB_D", "ShiftC_D", "ShiftD_D", "ShiftE_D"
        );
        for (String shift : baseShifts) {
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
