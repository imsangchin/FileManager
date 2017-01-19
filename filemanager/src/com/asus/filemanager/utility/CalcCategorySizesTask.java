package com.asus.filemanager.utility;

import android.content.Context;
import android.os.AsyncTask;

import com.asus.filemanager.activity.CategoryItem;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;

public class CalcCategorySizesTask extends AsyncTask<Object, Integer, Long> {

	private OnCategorySizesResultListener onCategorySizesResultListener;
	private Context context;
	private String rootPath;
	private int categoryItemId;
	private long storageTotalSpace = 0;
	public interface OnCategorySizesResultListener {
		void onCategorySizesResult(int categoryItemId, long sizes, float percentage);
	}

	public CalcCategorySizesTask(Context context,String rootPath, int categoryItemId, long storageTotalSpace, OnCategorySizesResultListener onCategorySizesResultListener)
	{
		this.context = context;
		this.rootPath = rootPath;
		this.categoryItemId = categoryItemId;
		this.storageTotalSpace = storageTotalSpace;
		this.onCategorySizesResultListener = onCategorySizesResultListener;
	}

	@Override
	public Long doInBackground(Object... objects) {
		// TODO Auto-generated method stub
		return getFilesSizes(categoryItemId);
	}

	protected void onPostExecute(Long result) {


		if (onCategorySizesResultListener != null) {
			float percentage = storageTotalSpace==0?0:result/(float)storageTotalSpace;
			onCategorySizesResultListener.onCategorySizesResult(categoryItemId,result,percentage);
		}
	}

	public long getFilesSizes(int categoryItemId)
	{
        if(context==null)
            return 0;

		switch(categoryItemId)
		{
			case CategoryItem.IMAGE:
				return MediaProviderAsyncHelper.getImageFilesSizes(context,rootPath);
			case CategoryItem.VIDEO:
				return MediaProviderAsyncHelper.getVideoFilesSizes(context,rootPath);
			case CategoryItem.MUSIC:
				return MediaProviderAsyncHelper.getMusicFilesSizes(context,rootPath);
			case CategoryItem.APP:
				return MediaProviderAsyncHelper.getFilesSizesByMimeTypeAndExtName(context, null, new String[]{"apk"},rootPath);
			case CategoryItem.DOCUMENT:
				return MediaProviderAsyncHelper.getFilesSizesByMimeTypeAndExtName(context,
						FileManagerActivity.SUPPORT_MIMETYPE_IN_DOCUMENTS_CATEGORY,
						FileManagerActivity.SUPPORT_EXTENSION_IN_PPT_CATEGORY
						,rootPath
				);
		}
		return 0;
	}
}
