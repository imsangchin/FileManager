package com.asus.filemanager.utility;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Yenju_Lai on 2016/3/25.
 */
public class DateUtility {
    public static String formatShortDateAndTime(Context context, long time) {
        java.text.DateFormat shortDateFormat = getDateFormat(context);
        java.text.DateFormat shortTimeFormat = getTimeFormat(context);
        Date date = new Date(time);
        String shortDate = shortDateFormat.format(date);
        if (shortDate.length() != 10) {
            shortDate = fixedLengthShortDate(shortDate, "/");
            shortDate = fixedLengthShortDate(shortDate, "-");
        }
        return shortDate + " " + shortTimeFormat.format(date);
    }

    private static java.text.DateFormat getDateFormat(Context context) {
        return context == null?
                new SimpleDateFormat("yyyy/MM/dd"): DateFormat.getDateFormat(context);
    }

    private static java.text.DateFormat getTimeFormat(Context context) {
        return context == null?
                new SimpleDateFormat("HH:mm") : DateFormat.getTimeFormat(context);
    }

    public static String formatLongDateAndTime(Context context, long time) {
        java.text.DateFormat shortDateFormat = getDateFormat(context);
        java.text.DateFormat shortTimeFormat = getTimeFormat(context);
        Date date = new Date(time);
        String shortDate = shortDateFormat.format(date);
        return android.text.format.DateFormat.format("EEEE", date)+ " " + shortDate + " " + shortTimeFormat.format(date);
    }

    private static String fixedLengthShortDate(String shortDate, String splitStr) {
        String[] dateVal = shortDate.split(splitStr);
        if (dateVal != null && dateVal.length != 1) {
            shortDate = "";
            for (String val: dateVal) {
                if (1 == val.length()) {
                    shortDate += "0" + val;
                } else {
                    shortDate += val;
                }
                shortDate += splitStr;
            }
            shortDate = shortDate.substring(0, shortDate.length() - 1);
        }
        return shortDate;
    }
}
