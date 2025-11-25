package com.example.English.Center.Data.util;

import java.util.*;

/**
 * Utilities to convert a Vietnamese-style daysOfWeek string (e.g. "2,4,6")
 * into Java DayOfWeek numeric values (1=Monday..7=Sunday).
 *
 * VN convention (commonly used in centers): 2=Mon,3=Tue,4=Wed,5=Thu,6=Fri,7=Sat, CN/8/0=Sun.
 * This util detects and maps safely. If the input already uses Java mapping (1..7), it returns as-is.
 */
public final class DaysOfWeekUtil {
    private DaysOfWeekUtil() {}

    public static Set<Integer> vnStringToJavaDows(String daysOfWeek) {
        Set<Integer> result = new HashSet<>();
        if (daysOfWeek == null || daysOfWeek.trim().isEmpty()) return result;
        String[] parts = daysOfWeek.split(",");
        List<String> tokens = new ArrayList<>();
        boolean hasOne = false; boolean hasCnOr08 = false; boolean hasNumGe2 = false;
        for (String p : parts) {
            String t = p.trim(); if (t.isEmpty()) continue; tokens.add(t);
            if (t.equalsIgnoreCase("CN")) hasCnOr08 = true;
            try {
                int v = Integer.parseInt(t);
                if (v == 1) hasOne = true;
                if (v == 0 || v == 8) hasCnOr08 = true;
                if (v >= 2) hasNumGe2 = true;
            } catch (NumberFormatException ignored) {}
        }
        boolean treatAsVn = !tokens.isEmpty() && (!hasOne) && (hasNumGe2 || hasCnOr08);
        for (String t : tokens) {
            Integer mapped = mapToken(t, treatAsVn);
            if (mapped != null && mapped >= 1 && mapped <= 7) result.add(mapped);
        }
        return result;
    }

    private static Integer mapToken(String token, boolean treatAsVn) {
        String t = token.trim().toUpperCase(Locale.ROOT);
        if (t.equals("CN") || t.equals("CHUNHAT")) return 7;
        try {
            int v = Integer.parseInt(t);
            if (!treatAsVn) {
                // Assume Java mapping already (1..7)
                if (v == 0 || v == 8) return 7; // be forgiving
                return clampJavaDow(v);
            }
            // VN mapping (2..7 => 1..6; 8/0 => 7)
            if (v >= 2 && v <= 7) return v - 1; // 2->1(Mon),...,7->6(Sat)
            if (v == 8 || v == 0) return 7;    // Sun
            if (v == 1) return 1; // edge-case: treat 1 as Monday if appears
        } catch (NumberFormatException ex) {
            // ignore
        }
        return null;
    }

    private static int clampJavaDow(int v) {
        if (v < 1) return 1; if (v > 7) return 7; return v;
    }
}

