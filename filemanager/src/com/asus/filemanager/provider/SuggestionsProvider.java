package com.asus.filemanager.provider;

import java.io.File;
import java.util.ArrayList;

import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.wrap.WrapEnvironment;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class SuggestionsProvider extends SearchRecentSuggestionsProvider{
    public final static String AUTHORITY = "com.asus.filemanager.SuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    private static final int SEARCH_SUGGEST = 0;
    private static final int SHORTCUT_REFRESH = 1;
    private static final UriMatcher sURIMatcher = buildUriMatcher();
    
    private static final String INTERNAL_HEAD = "/sdcard";
    private static final String SD_HEAD = "/Removable/MicroSD";
    private static final String USB_HEAD = "/Removable/USBdisk1";
    private static final String USB_KEY = "usbdisk";

    
    /**
     * Sets up a uri matcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SHORTCUT_REFRESH);
        return matcher;
    }
    
    
    /**
     * The columns we'll include in our search suggestions.  There are others that could be used
     * to further customize the suggestions, see the docs in {@link SearchManager} for the details
     * on additional columns that are supported.
     */
    private static final String[] COLUMNS = {
            "_id",  // must include this column
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            };
    

    
    @Override 
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder){
        if (!TextUtils.isEmpty(selection)) {
            throw new IllegalArgumentException("selection not allowed for " + uri);
        }
        if (selectionArgs != null && selectionArgs.length != 0) {
            throw new IllegalArgumentException("selectionArgs not allowed for " + uri);
        }
        if (!TextUtils.isEmpty(sortOrder)) {
            throw new IllegalArgumentException("sortOrder not allowed for " + uri);
        }

        switch (sURIMatcher.match(uri)) {
        case SEARCH_SUGGEST:
            String query = null;
            if (uri.getPathSegments().size() > 1) {
                query = uri.getLastPathSegment().toLowerCase();
            }
            return getQuickSeachResult(query, projection);
        case SHORTCUT_REFRESH:
            String shortcutId = null;
            if (uri.getPathSegments().size() > 1) {
                shortcutId = uri.getLastPathSegment();
            }
            return refreshShortcut(shortcutId, projection);
            
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    
    private Cursor refreshShortcut(String shortcutId, String[] projection) {
        return null;
    }
    
    private Cursor getQuickSeachResult(String query, String[] projection){
    	String processedQuery = query == null ? "" : query.toLowerCase();
    	Context mContext = getContext();
    	ArrayList<VFile> results = MediaProviderAsyncHelper.queryLocalDb(mContext, processedQuery);
    	MatrixCursor cursor = new MatrixCursor(COLUMNS);
    	for(int i = 0; i < results.size() ; i ++){
    		
    		VFile file = results.get(i);
        	cursor.addRow(columnValuesOfFile(file));
    	}

    	return cursor;
    }
    
    private Object[] columnValuesOfFile(VFile file) {
    	String id = "";
    	String name = "";
    	String filePath = "";
    	String icon = "";
    	String intent_data = "";
    	
    	id = convertAbsPath2Relative(file.getAbsolutePath());
    	name = file.getName();
    	filePath = id;
    	int res = MimeMapUtility.getIconRes(file);
    	icon = String.valueOf(res);
    	intent_data = id;
    	
    	
        return new String[] {
        		id,           // _id
        		name,         // text1
        		filePath,     // text2
        		icon,         // icon
        		intent_data,  // intent_data (included when clicking on item)
        };
    }
    
    /**
     * All queries for this provider are for the search suggestion and shortcut refresh mime type.
     */
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case SHORTCUT_REFRESH:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
    
    private String convertAbsPath2Relative(String path){
    	String ok_path = path;

    	String InternalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    	if(path.startsWith(InternalPath)){   		
    		ok_path = path.replaceFirst(InternalPath, INTERNAL_HEAD);
    	}else if(WrapEnvironment.SUPPORT_REMOVABLE){
    		
    		StringBuilder newPath = null;
    		String[] pathArray = path.split(File.separator);
    		int len = pathArray.length;
    		
    		if(pathArray[2].contains(USB_KEY)){
    			newPath = new StringBuilder(USB_HEAD);
    		}else{
        		newPath = new StringBuilder(SD_HEAD);
    		}

    		for(int i = 3;i < len; i++){
    			newPath.append(File.separator + pathArray[i]);
    		}
    		
    		ok_path = newPath.toString();
    	}
    	return ok_path;
    }
}
