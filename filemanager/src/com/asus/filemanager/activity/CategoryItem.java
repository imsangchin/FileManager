package com.asus.filemanager.activity;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

import java.util.List;

public class CategoryItem {

    private static final String kCategorySeparator = ":";

    public static final int IMAGE = 0;
    public static final int VIDEO = IMAGE+1;
    public static final int MUSIC = IMAGE+2;
    public static final int DOWNLOAD = IMAGE+3;
    public static final int FAVORITE = IMAGE+4;
    public static final int APP = IMAGE+5;
    public static final int DOCUMENT = IMAGE+6;
    public static final int COMPRESSED = IMAGE+7;
    public static final int RECENT = IMAGE+8;
    public static final int LARGE_FILE = IMAGE+9;
    public static final int PDF = IMAGE+10;
    public static final int GAME = IMAGE+11;
    public static final int QQ = IMAGE+12;
    public static final int WECHAT = IMAGE+13;

    public int id;
    public boolean isChecked;
    public Drawable icon;
    public String name;

    public static String findNameById(Resources res, int id) {
        switch (id) {
        case IMAGE:
            return res.getString(R.string.category_image1);
        case VIDEO:
            return res.getString(R.string.category_video1);
        case MUSIC:
            return res.getString(R.string.category_music1);
        case DOWNLOAD:
            return res.getString(R.string.category_download1);
        case FAVORITE:
            return res.getString(R.string.category_favorite1);
        case APP:
            return res.getString(R.string.category_app1);
        case DOCUMENT:
            return res.getString(R.string.category_document1);
        case COMPRESSED:
            return res.getString(R.string.category_compressed1);
        case RECENT:
            return res.getString(R.string.category_recent1);
        case LARGE_FILE:
            return res.getString(R.string.category_large_file1);
        case PDF:
            return res.getString(R.string.category_pdf1);
        case GAME:
            return res.getString(R.string.category_game);
        case QQ:
            return res.getString(R.string.category_qq1);
        case WECHAT:
                return res.getString(R.string.category_wechat1);
        }
        return res.getString(android.R.string.unknownName);
    }

    public static Drawable findSmallDrawableById(Resources res, int id) {
        switch (id) {
        case IMAGE:
            return res.getDrawable(R.drawable.ic_category_sorting_image);
        case VIDEO:
            return res.getDrawable(R.drawable.ic_category_sorting_video);
        case MUSIC:
            return res.getDrawable(R.drawable.ic_category_sorting_music);
        case DOWNLOAD:
            return res.getDrawable(R.drawable.ic_category_sorting_download);
        case FAVORITE:
            return res.getDrawable(R.drawable.ic_category_sorting_favorite);
        case APP:
            return res.getDrawable(R.drawable.ic_category_sorting_app);
        case DOCUMENT:
            return res.getDrawable(R.drawable.ic_category_sorting_document);
        case COMPRESSED:
            return res.getDrawable(R.drawable.ic_category_sorting_compressed);
        case RECENT:
            return res.getDrawable(R.drawable.ic_category_sorting_recently_added);
        case LARGE_FILE:
            return res.getDrawable(R.drawable.ic_category_sorting_large_file);
        case PDF:
            return res.getDrawable(R.drawable.ic_category_sorting_pdf);
        case GAME:
            return res.getDrawable(R.drawable.ic_category_sorting_game);
        case QQ:
            return res.getDrawable(R.drawable.icon_qq);
        case WECHAT:
            return res.getDrawable(R.drawable.icon_wechat);
        }
        return res.getDrawable(android.R.drawable.sym_def_app_icon);
    }

    public static Drawable findDrawableById(Resources res, int id) {
        switch (id) {
        case IMAGE:
            return res.getDrawable(R.drawable.icon_image);
        case VIDEO:
            return res.getDrawable(R.drawable.icon_vedio);
        case MUSIC:
            return res.getDrawable(R.drawable.icon_music);
        case DOWNLOAD:
            return res.getDrawable(R.drawable.icon_download);
        case FAVORITE:
            return res.getDrawable(R.drawable.icon_favorite);
        case APP:
            return res.getDrawable(R.drawable.icon_app);
        case DOCUMENT:
            return res.getDrawable(R.drawable.icon_document);
        case COMPRESSED:
            return res.getDrawable(R.drawable.icon_zip);
        case RECENT:
            return res.getDrawable(R.drawable.icon_recent);
        case LARGE_FILE:
            return res.getDrawable(R.drawable.icon_large);
        case PDF:
            return res.getDrawable(R.drawable.icon_pdf);
        case GAME:
            return res.getDrawable(R.drawable.icon_game);
        case QQ:
            return res.getDrawable(R.drawable.icon_qq);
        case WECHAT:
            return res.getDrawable(R.drawable.icon_wechat);
        }
        return res.getDrawable(android.R.drawable.sym_def_app_icon);
    }

    public static int findColorIdById(int id) {
        if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
            switch (id) {
                case IMAGE:
                    return R.color.dark_theme_category_image;
                case VIDEO:
                    return R.color.dark_theme_category_video;
                case MUSIC:
                    return R.color.dark_theme_category_music;
                case DOWNLOAD:
                    return R.color.dark_theme_category_download;
                case FAVORITE:
                    return R.color.dark_theme_category_favorite;
                case APP:
                    return R.color.dark_theme_category_apps;
                case DOCUMENT:
                    return R.color.dark_theme_category_documents;
                case COMPRESSED:
                    return R.color.dark_theme_category_compressed;
                case RECENT:
                    return R.color.dark_theme_category_recentadd;
                case LARGE_FILE:
                    return R.color.dark_theme_category_largefiles;
                case PDF:
                    return R.color.dark_theme_category_pdf;
                case GAME:
                    return R.color.dark_theme_category_game;
                case QQ:
                    return R.color.dark_theme_category_qq;
                case WECHAT:
                    return R.color.dark_theme_category_wechat;
            }
            return R.color.dark_theme_category_image;
        }

        switch (id) {
            case IMAGE:
                return R.color.category_image_color;
            case VIDEO:
                return R.color.category_video_color;
            case MUSIC:
                return R.color.category_music_color;
            case DOWNLOAD:
                return R.color.category_download_color;
            case FAVORITE:
                return R.color.category_favorite_color;
            case APP:
                return R.color.category_app_color;
            case DOCUMENT:
                return R.color.category_document_color;
            case COMPRESSED:
                return R.color.category_compressed_color;
            case RECENT:
                return R.color.category_recent_color;
            case LARGE_FILE:
                return R.color.category_large_color;
            case PDF:
                return R.color.category_pdf_color;
            case GAME:
                return R.color.category_game_color;
            case QQ:
                return R.color.category_pdf_color;
            case WECHAT:
                return R.color.category_game_color;
        }
        return R.color.category_image_color;
    }

    public static int findDrawableIdById(int id) {
        switch (id) {
        case IMAGE:
            return R.drawable.icon_image;
        case VIDEO:
            return R.drawable.icon_vedio;
        case MUSIC:
            return R.drawable.icon_music;
        case DOWNLOAD:
            return R.drawable.icon_download;
        case FAVORITE:
            return R.drawable.icon_favorite;
        case APP:
            return R.drawable.icon_app;
        case DOCUMENT:
            return R.drawable.icon_document;
        case COMPRESSED:
            return R.drawable.icon_zip;
        case RECENT:
            return R.drawable.icon_recent;
        case LARGE_FILE:
            return R.drawable.icon_large;
        case PDF:
            return R.drawable.icon_pdf;
        case GAME:
            return R.drawable.icon_game;
        case QQ:
            return R.drawable.icon_qq;
        case WECHAT:
            return R.drawable.icon_wechat;
        }
        return android.R.drawable.sym_def_app_icon;
    }

    public static void setBackgroundColorAndRetainShape(final int color, final Drawable background) {

        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable) background.mutate()).getPaint().setColor(color);
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable) background.mutate()).setColor(color);
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable) background.mutate()).setColor(color);
        }else{
            Log.w("CategoryItem","Not a valid background type");
        }

    }
    public static CategoryItem create(Resources res, int id, boolean isChecked) {
        return new CategoryItem(id, isChecked,
            CategoryItem.findNameById(res, id)
        );
    }

    public static CategoryItem create(Resources res, String preferenceString) {
        if (null == preferenceString) {
            return null;
        }
        String[] values = preferenceString.split(kCategorySeparator);
        // check split rule from method toPreferenceString
        if (values != null && 2 == values.length) {
            int id = Integer.parseInt(values[0]);
            return new CategoryItem(
                id,
                Boolean.parseBoolean(values[1]),
                CategoryItem.findNameById(res, id)
            );
        }
        return null;
    }

    public static void fillSmallDrawable(List<CategoryItem> categorys, Resources res) {
        for (CategoryItem item: categorys) {
            item.icon = CategoryItem.findSmallDrawableById(res, item.id);
        }
    }

    public static void fillDrawable(List<CategoryItem> categorys, Resources res) {
        for (CategoryItem item: categorys) {
            item.icon = CategoryItem.findDrawableById(res, item.id);
        }
    }

    public CategoryItem(CategoryItem item) {
        this.id = item.id;
        this.isChecked = item.isChecked;
        this.icon = item.icon;
        this.name = item.name;
    }

    private CategoryItem(int id, boolean isChecked, String name) {
        this.id = id;
        this.isChecked = isChecked;
        this.icon = null;
        this.name = name;
    }

    public String toPreferenceString() {
        return "" + id + kCategorySeparator + isChecked;
    }
};