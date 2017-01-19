package com.asus.filemanager.activity;

import android.content.Context;
import android.content.res.Resources;

import com.asus.filemanager.R;
import com.asus.filemanager.wrap.WrapEnvironment;


public class ToolItem {

	public static final int HTTP_FILE_TRANSFER = 1000;
	public static final int CLEAN_MASTER = 1001;
	public static final int ZENUI_UPDATE = 1002;
	public static final int STORAGE_ANALYZER = 1003;
    public static final int RECYCLE_BIN = 1004;
    public static final int HIDDEN_ZONE = 1005;

	public int id;
	public int iconResId;
	public String name;
	public boolean showRecommend = false;

	public ToolItem(Context context,int id) {
		this.id = id;
		setIcon(id);
		setName(context.getResources(),id);
		setShowRecommend(context,id);
	}

	public void setIcon(int id) {
		switch (id) {
			case HTTP_FILE_TRANSFER:
				iconResId = R.drawable.ic_tool_transfer;
				break;
			case CLEAN_MASTER:
				iconResId = R.drawable.ic_tool_clean;
				break;
			case ZENUI_UPDATE:
				iconResId = R.drawable.ic_tool_update;
				break;
			case STORAGE_ANALYZER:
				iconResId = R.drawable.ic_tool_analyzer;
				break;
            case RECYCLE_BIN:
                iconResId = R.drawable.ic_tool_recycle;
                break;
            case HIDDEN_ZONE:
                iconResId = R.drawable.ic_tool_hiddenzone;
                break;
			default:
				break;
		}
	}
	
	public void setName(Resources res,int id)
	{
		switch (id) {
			case HTTP_FILE_TRANSFER:
				name = res.getString(R.string.tools_file_transfer);
				break;
			case CLEAN_MASTER:
				name = res.getString(R.string.tools_clean_master);
				break;
			case ZENUI_UPDATE:
				name = res.getString(R.string.tools_zenui_instant_update);
				break;
			case STORAGE_ANALYZER:
				name = res.getString(R.string.tools_storage_analyzer);
				break;
            case RECYCLE_BIN:
                name = res.getString(R.string.tools_recycle_bin);
                break;
            case HIDDEN_ZONE:
                name = res.getString(R.string.tools_hidden_zone);
                break;
			default:
				break;
		}
	}
	
	public void setShowRecommend(Context context,int id)
	{
		switch (id) {
			case CLEAN_MASTER:
				if (WrapEnvironment.isCMPackagePreloadedAndExist(context))
					showRecommend = false;
				else
					showRecommend = true;
				break;
			default:
				showRecommend = false;
				break;
		}
	}

}
