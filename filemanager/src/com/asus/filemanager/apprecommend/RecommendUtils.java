package com.asus.filemanager.apprecommend;

import java.util.Locale;

import android.content.Context;

public class RecommendUtils {

    private static final int INVALID_RESOURCE_ID = 0;
    private static final String CATEGORY_NAME_PREFIX = "category_name_";

    public static String getCategoryName(Context context, String categoryKey) {
        int resId = context.getResources().getIdentifier(
                CATEGORY_NAME_PREFIX + categoryKey.toLowerCase(Locale.US),
                "string", context.getPackageName());
        return INVALID_RESOURCE_ID == resId ? categoryKey : context.getString(resId);
    }
}