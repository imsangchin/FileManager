package com.asus.filemanager.ga;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.VFile;

import android.content.Context;

public class GaOpenFile extends GoogleAnalyticsBase {

    private static final String CATEGORY_NAME = "open_file";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-10";
    public static final String KEY_ID = "GA_OPEN_FILE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_OPEN_FILE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_OPEN_FILE_SAMPLE_RATE";

    private static final String OPEN_FILE_TYPE_APK = "apk";
    private static final String OPEN_FILE_TYPE_BOOK = "book";
    private static final String OPEN_FILE_TYPE_EXCEL = "excel";
    private static final String OPEN_FILE_TYPE_VIDEO = "video";
    private static final String OPEN_FILE_TYPE_MUSIC = "music";
    private static final String OPEN_FILE_TYPE_PDF = "pdf";
    private static final String OPEN_FILE_TYPE_IMAGE = "image";
    private static final String OPEN_FILE_TYPE_PPT = "ppt";
    private static final String OPEN_FILE_TYPE_TXT = "txt";
    private static final String OPEN_FILE_TYPE_WORD = "word";
    private static final String OPEN_FILE_TYPE_ZIP = "zip";
    private static final String OPEN_FILE_TYPE_RAR = "rar";
    private static final String OPEN_FILE_TYPE_OTHERS = "others";

    private static GaOpenFile mInstance;

    public static GaOpenFile getInstance() {
        if (mInstance == null) {
            mInstance = new GaOpenFile();
        }

        return mInstance;
    }

    private GaOpenFile() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 10.0f);
    }

    public void sendEvents(Context context, VFile vFile) {

        if (vFile == null || vFile.isDirectory()) {
            return;
        }

        int res = MimeMapUtility.getIconRes(vFile);

        String action = null; /* open file type */
        String label = vFile.getExtensiontName().toLowerCase(); /* extension name */

        switch (res) {
        case R.drawable.asus_ep_ic_apk:
            action = OPEN_FILE_TYPE_APK;
            break;
        case R.drawable.asus_ep_ic_book:
            action = OPEN_FILE_TYPE_BOOK;
            break;
        case R.drawable.asus_ep_ic_excel:
            action = OPEN_FILE_TYPE_EXCEL;
            break;
        case R.drawable.asus_ep_ic_movie:
            action = OPEN_FILE_TYPE_VIDEO;
            break;
        case R.drawable.asus_ep_ic_music:
            action = OPEN_FILE_TYPE_MUSIC;
            break;
        case R.drawable.asus_ep_ic_pdf:
            action = OPEN_FILE_TYPE_PDF;
            break;
        case R.drawable.asus_ep_ic_photo:
            action = OPEN_FILE_TYPE_IMAGE;
            break;
        case R.drawable.asus_ep_ic_ppt:
            action = OPEN_FILE_TYPE_PPT;
            break;
        case R.drawable.asus_ep_ic_txt:
            action = OPEN_FILE_TYPE_TXT;
            break;
        case R.drawable.asus_ep_ic_word:
            action = OPEN_FILE_TYPE_WORD;
            break;
        case R.drawable.asus_ep_ic_zip:
            action = OPEN_FILE_TYPE_ZIP;
            break;
        case R.drawable.asus_ep_ic_rar:
            action = OPEN_FILE_TYPE_RAR;
            break;
        default:
            action = OPEN_FILE_TYPE_OTHERS;
        }

        super.sendEvents(context, CATEGORY_NAME, action, label, null);
    }
}
