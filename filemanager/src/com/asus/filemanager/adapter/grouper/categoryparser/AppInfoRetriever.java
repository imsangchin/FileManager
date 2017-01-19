package com.asus.filemanager.adapter.grouper.categoryparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

public class AppInfoRetriever {
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String TAG = "[AppInfoRetriever]";
    private static final boolean DEBUG = true;

    // ------------------------------------------------------------------------
    // STATIC INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    private Context mContext;
    private List<String> mNames;
    private boolean mFetchCoverImages = false;
    private boolean mParseGooglePlay = true;
    private boolean mParseWDJ = false;
    private boolean mUseWifiOnly = false;

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------
    private AppInfoRetriever(Context context, List<String> names, boolean fetchCoverImages, boolean parseGooglePlay, boolean parseWDJ, boolean useWifiOnly) {
        this.mContext = context;
        this.mNames = names;
        this.mFetchCoverImages = fetchCoverImages;
        this.mParseGooglePlay = parseGooglePlay;
        this.mParseWDJ = parseWDJ;
        this.mUseWifiOnly = useWifiOnly;
    }

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    public static AppInfoRetriever getAppInfoRetriever(Context context, String[] names, boolean fetchCoverImages, boolean parseGooglePlay, boolean parseWDJ, boolean useWifiOnly) {
        return new AppInfoRetriever(context, Arrays.asList(names), fetchCoverImages, parseGooglePlay, parseWDJ, useWifiOnly);
    }
/*
    public static AppInfoRetriever getAppInfoRetriever(Context context, String[] names) {
        boolean enableWDJCategory = LauncherApplication.checkEnableWDJCategory(context);
        return new AppInfoRetriever(context, Arrays.asList(names), false, !enableWDJCategory, enableWDJCategory, false);
    }
*/
    public List<AppInfo> getAppInfo() {
        if (mContext == null) {
            Log.d(TAG, "[getAppInfo] get null context, return");
            return new ArrayList<AppInfo>();
        }

        List<AppInfo> results = new ArrayList<AppInfo>();
        long startTime;

        // check if all names are parsed
        List<String> remainingNames = getRemainingNames(mNames, results);

        // parse Google Play pages if we get partial results
        boolean isPartialResult = remainingNames.size() > 0;
        if (isPartialResult && isQueryGooglePlayEnabled()) {
            startTime = SystemClock.uptimeMillis();
            results.addAll(queryAppInfoFromGooglePlay(remainingNames));
            Log.v(TAG, "QueryAppInfoFromGooglePlay took " + (SystemClock.uptimeMillis() - startTime) + " ms");
        }

        // update remaining items
        remainingNames = getRemainingNames(mNames, results);

        // parse WanDouJia if we get partial results
        isPartialResult = remainingNames.size() > 0;
        if (isPartialResult && isQueryWDJEnabled()) {
            startTime = SystemClock.uptimeMillis();
            results.addAll(queryAppInfoFromWDJ(remainingNames));
            Log.v(TAG, "queryAppInfoFromWDJ took " + (SystemClock.uptimeMillis() - startTime) + " ms");
        }

        return results;
    }

    private List<String> getRemainingNames(List<String> allNames, List<AppInfo> results) {
        List<String> ret = new ArrayList<String>();

        Set<String> diff = new HashSet<String>(allNames);
        Set<String> parsedNames = new HashSet<String>();
        for (AppInfo info : results) {
            parsedNames.add(info.getName());
        }
        diff.removeAll(parsedNames);

        ret.addAll(diff);
        return ret;
    }

    private boolean isQueryGooglePlayEnabled() {
        return mParseGooglePlay;
    }

    private boolean isQueryWDJEnabled() {
        return mParseWDJ;
    }

    private List<AppInfo> queryAppInfoFromGooglePlay(final List<String> names) {
        List<AppInfo> results = new ArrayList<AppInfo>();

        List<AsyncTask<Void, Integer, AppInfo>> parseTasks = new ArrayList<AsyncTask<Void, Integer, AppInfo>>();

        for (String name : names) {
            final GooglePlayParser parser = new GooglePlayParser(null);
            parser.setPackage(name);

            parseTasks.add(new AsyncTask<Void, Integer, AppInfo>(){
                @Override
                protected AppInfo doInBackground(Void... params) {
                    AppInfo info = null;
                    if (!blockedDueToNoWifiConnection()) {
                        info = parser.parse();
                        if (mFetchCoverImages) {
                            info.fetchCoverImage();
                        }
                    } else {
                        Log.d(TAG, "[queryAppInfoFromGooglePlay] skip package due to no wifi:" + parser.getPackage());
                    }
                    return info;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
        }

        for (AsyncTask<Void, Integer, AppInfo> t : parseTasks) {
            try {
                AppInfo result = t.get();
                if (result != null)
                    results.add(result);
            } catch (InterruptedException e) {
                Log.d(TAG, "[queryAppInfoFromGooglePlay] exception: " + e.toString());
            } catch (ExecutionException e) {
                Log.d(TAG, "[queryAppInfoFromGooglePlay] exception: " + e.toString());
            } catch (CancellationException e) {
                Log.d(TAG, "[queryAppInfoFromGooglePlay] exception: " + e.toString());
            }
        }

        return results;
    }

    private List<AppInfo> queryAppInfoFromWDJ(final List<String> names) {
        List<AppInfo> results = new ArrayList<AppInfo>();

        List<AsyncTask<Void, Integer, AppInfo>> parseTasks = new ArrayList<AsyncTask<Void, Integer, AppInfo>>();

        for (String name : names) {
            final WanDouJiaParser parser = new WanDouJiaParser(null);
            parser.setPackage(name);

            parseTasks.add(new AsyncTask<Void, Integer, AppInfo>(){
                @Override
                protected AppInfo doInBackground(Void... params) {
                    AppInfo info = null;
                    if (!blockedDueToNoWifiConnection()) {
                        info = parser.parse();
                        if (mFetchCoverImages) {
                            info.fetchCoverImage();
                        }
                    } else {
                        Log.d(TAG, "[queryAppInfoFromWDJ] skip package due to no wifi:" + parser.getPackage());
                    }
                    return info;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
        }

        for (AsyncTask<Void, Integer, AppInfo> t : parseTasks) {
            try {
                AppInfo result = t.get();
                if (result != null)
                    results.add(result);
            } catch (InterruptedException e) {
                Log.d(TAG, "[queryAppInfoFromWDJ] exception: " + e.toString());
            } catch (ExecutionException e) {
                Log.d(TAG, "[queryAppInfoFromWDJ] exception: " + e.toString());
            } catch (CancellationException e) {
                Log.d(TAG, "[queryAppInfoFromWDJ] exception: " + e.toString());
            }
        }

        return results;
    }

    private boolean blockedDueToNoWifiConnection() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return mUseWifiOnly && !info.isConnected();
    }
}
