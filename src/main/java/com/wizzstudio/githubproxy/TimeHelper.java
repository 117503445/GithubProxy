package com.wizzstudio.githubproxy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeHelper {
    public static String getTimeString() {
        var formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return formatter.format(new Date());
    }
}
