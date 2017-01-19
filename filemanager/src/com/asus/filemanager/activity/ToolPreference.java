package com.asus.filemanager.activity;

import android.content.Context;

import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.util.ArrayList;
import java.util.List;

public class ToolPreference {

	public static List<ToolItem> createDefaultTools(Context context) {
		ArrayList<ToolItem> toolItems = new ArrayList<ToolItem>();
		toolItems.add(new ToolItem(context,ToolItem.STORAGE_ANALYZER));
        //toolItems.add(new ToolItem(context, ToolItem.RECYCLE_BIN));
		toolItems.add(new ToolItem(context,ToolItem.HTTP_FILE_TRANSFER));
        //addCMTool(context, toolItems);
		toolItems.add(new ToolItem(context,ToolItem.CLEAN_MASTER));
        //addZenUITool(context,toolItems);
        toolItems.add(new ToolItem(context, ToolItem.HIDDEN_ZONE));
		return toolItems;
    }

	private static void addCMTool(Context context, ArrayList<ToolItem> toolItems)
	{
		if(ToolPreference.showCleanMaster(context))
			toolItems.add(new ToolItem(context,ToolItem.CLEAN_MASTER));
	}

	private static void addZenUITool(Context context,ArrayList<ToolItem> toolItems)
	{
		if (!ItemOperationUtility.getInstance().enableCtaCheck() && !WrapEnvironment.IS_VERIZON) {
			toolItems.add(new ToolItem(context, ToolItem.ZENUI_UPDATE));
		}
	}

    public static boolean showCleanMaster(Context context)
    {
        boolean showCM = true;
        if (WrapEnvironment.IS_VERIZON) {
            showCM = false;
        } else if (!WrapEnvironment.isCMPackagePreloadedAndExist(context)) {
            showCM = false;
        }
        return showCM;
    }
}
