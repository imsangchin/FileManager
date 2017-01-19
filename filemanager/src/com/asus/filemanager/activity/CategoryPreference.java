package com.asus.filemanager.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CategoryPreference {

    private static final String TAG = "CategoryPreference";

    private static final String kCategorySortingSize = "key_category_sorting_size";
    private static final String kCategorySortingIndex = "key_category_sorting_index";

    public interface OnSaveCallback {
        public void onSaveDone();
    };

    public interface OnLoadCallback {
        public void onLoadDone(List<CategoryItem> categorys);
    };

    private static class BackgroundTask extends AsyncTask<Object, Object, Object> {

        public static final int CMD_LOAD_PREF = 0;
        public static final int CMD_SAVE_PREF = CMD_LOAD_PREF+1;

        @Override
        protected Void doInBackground(Object... values) {
            int cmd = (Integer)values[0];
            Context context = (Context)values[1];
            switch (cmd) {
            case CMD_LOAD_PREF: {
                List<CategoryItem> categorys = loadFromPreferenceOnBackgroundThread((Context)values[1]);
//                Log.d(TAG, "cmd load pref");
//                dump(categorys);
                publishProgress(cmd, context, (OnLoadCallback)values[2], categorys);
            } break;
            case CMD_SAVE_PREF: {
                saveToPreferenceOnBackgroundThread(context, (List<CategoryItem>)values[3]);
//                Log.d(TAG, "cmd save pref");
//                dump((List<CategoryItem>)values[3]);
                publishProgress(values);
            } break;
            } // end of switch
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int cmd = (Integer)values[0];
            switch (cmd) {
            case CMD_LOAD_PREF: {
                OnLoadCallback callback = (OnLoadCallback)values[2];
                List<CategoryItem> categorys = (List<CategoryItem>)values[3];
                if (callback != null) {
                    callback.onLoadDone(categorys);
                }
            } break;
            case CMD_SAVE_PREF: {
                OnSaveCallback callback = (OnSaveCallback)values[2];
                if (callback != null) {
                    callback.onSaveDone();
                }
            } break;
            }
        }

        private static void dump(List<CategoryItem> categorys) {
            for (CategoryItem item : categorys) {
                Log.e(TAG, "item=" + item.name + " - " + (item.isChecked ? "O" : "X"));
            }
        }

        private List<CategoryItem> loadFromPreferenceOnBackgroundThread(Context context) {
            Log.d(TAG, "loadFromPreferenceOnBackgroundThread");
            List<CategoryItem> categorys = null;

            SharedPreferences pref = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
            int size = pref.getInt(kCategorySortingSize, 0);
            Resources res = context.getResources();
            if (0 == size) {
                // create the asus default category sorting
                categorys = createDefaultCategorys(res);
            } else {
                // load user preference
                categorys = new ArrayList<CategoryItem>();
                for (int i = 0; i < size; ++i) {
                    categorys.add(CategoryItem.create(res, pref.getString(kCategorySortingIndex + i, null)));
                }
                int listSize = categorys.size();
                if (listSize == 11) {
                    // add Game Category
                    categorys.add(CategoryItem.create(res, CategoryItem.GAME, false));
                }
            }
            return categorys;
        }

        private void saveToPreferenceOnBackgroundThread(Context context, List<CategoryItem> categorys) {
            Log.d(TAG, "saveToPreferenceOnBackgroundThread");
            SharedPreferences.Editor prefEdit = context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit();
            int size = categorys.size();
            prefEdit.putInt(kCategorySortingSize, size);
            for (int i = 0; i < size; ++i) {
                prefEdit.putString(kCategorySortingIndex + i, categorys.get(i).toPreferenceString());
            }
            prefEdit.commit();
        }
    };

    public static void postLoadFromPreference(Context context, OnLoadCallback callback) {
        new BackgroundTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
            BackgroundTask.CMD_LOAD_PREF, context, callback
        );
    }

    public static void postSaveToPreference(Context context, OnSaveCallback callback, List<CategoryItem> categorys) {
        new BackgroundTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
            BackgroundTask.CMD_SAVE_PREF, context, callback, categorys
        );
    }

    public static List<CategoryItem> createDefaultCategorys(Resources res) {
        // create the asus default category sorting
        List<CategoryItem> categorys = new ArrayList<CategoryItem>();
        categorys.add(CategoryItem.create(res, CategoryItem.IMAGE, true));
        categorys.add(CategoryItem.create(res, CategoryItem.VIDEO, true));
        categorys.add(CategoryItem.create(res, CategoryItem.MUSIC, true));
        categorys.add(CategoryItem.create(res, CategoryItem.COMPRESSED, true));
        //categorys.add(CategoryItem.create(res, CategoryItem.FAVORITE, true));
        categorys.add(CategoryItem.create(res, CategoryItem.APP, true));
        categorys.add(CategoryItem.create(res, CategoryItem.DOCUMENT, true));
        categorys.add(CategoryItem.create(res, CategoryItem.DOWNLOAD, true));
        //categorys.add(CategoryItem.create(res, CategoryItem.RECENT, true));
        //categorys.add(CategoryItem.create(res, CategoryItem.LARGE_FILE, false));
        //categorys.add(CategoryItem.create(res, CategoryItem.PDF, false));
        //categorys.add(CategoryItem.create(res, CategoryItem.GAME, false));
        categorys.add(CategoryItem.create(res, CategoryItem.QQ, true));
        categorys.add(CategoryItem.create(res, CategoryItem.WECHAT, true));
        return categorys;
    }

    public static List<CategoryItem> extractSelectedCategoryItems(List<CategoryItem> categorys) {
        List<CategoryItem> selectedCategorys = new ArrayList<CategoryItem>();
        for (CategoryItem item: categorys) {
            if (item.isChecked) {
                selectedCategorys.add(item);
            }
        }
        return selectedCategorys;
    }

}
