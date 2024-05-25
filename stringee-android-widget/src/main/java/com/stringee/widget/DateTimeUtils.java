package com.stringee.widget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtils {

    /**
     * Get free call time
     *
     * @param currentTime
     * @param startTime
     * @return
     */
    public static String getCallTime(long currentTime, long startTime) {
        long time = currentTime - startTime;
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date(time));
    }
}
