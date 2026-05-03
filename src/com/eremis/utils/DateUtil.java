package com.eremis.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static final DateTimeFormatter DISPLAY_FORMATTER =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    public static final DateTimeFormatter DATE_ONLY =
        DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter SQL_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtil() {}

    public static String format(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(DISPLAY_FORMATTER);
    }
    public static String formatDate(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(DATE_ONLY);
    }
}
