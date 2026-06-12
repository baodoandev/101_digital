package com.coffee.platform.shop;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class OpeningHours {

    private OpeningHours() {}

    public static boolean isOpen(LocalTime opening, LocalTime closing, ZoneId zone, Instant now) {
        LocalTime current = ZonedDateTime.ofInstant(now, zone).toLocalTime();
        if (closing.isAfter(opening)) {
            return !current.isBefore(opening) && current.isBefore(closing);
        } else {
            return !current.isBefore(opening) || current.isBefore(closing);
        }
    }
}
