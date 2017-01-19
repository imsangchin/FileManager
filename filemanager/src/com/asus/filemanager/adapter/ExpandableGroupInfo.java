package com.asus.filemanager.adapter;

public class ExpandableGroupInfo implements Comparable<ExpandableGroupInfo>{

    public static final String TAG_SUB_CATEGORY_PREFERENCE = "SubCategoryPreference";
    public static String KEY_HAS_EXPAND_RECOMMEND_SECTION = "HAS_EXPAND_RECOMMEND_SECTION";
    public static String KEY_HAS_EXPAND_RECOMMEND_SECTION_GAME = "HAS_EXPAND_RECOMMEND_SECTION_GAME";

    public class TitleType {
        public static final int EXCEL = 1;
        public static final int PDF = 2;
        public static final int POWERPOINT = 3;
        public static final int TXT = 4;
        public static final int WORD = 5;
        public static final int INSTALLED = 6;
        public static final int NOT_INSTALLED = 7;
        public static final int RECOMMENDED = 8;
        public static final int OTHERS = 9;
        public static final int INSTALLED_GAME = 10;
        public static final int RECOMMENDED_GAME = 11;
    }

    private String mTitle;
    private int mId;

    public ExpandableGroupInfo(String title, int id) {
        mTitle = title;
        mId = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getId() {
        return mId;
    }

    @Override
    public int compareTo(ExpandableGroupInfo another) {
        return this.mId -  another.mId;
    }
}
