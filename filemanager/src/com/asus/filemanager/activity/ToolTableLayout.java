package com.asus.filemanager.activity;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatDrawableManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

import java.util.List;


public class ToolTableLayout {

	public static final String TAG_RECOMMEND = "TAG_RECOMMEND";

	public static void setTableLayoutContentView(Activity activity,int layoutId, List<ToolItem> toolItems, int rowSize,OnClickListener onClickListener) {
		TableLayout tableLayout = (TableLayout) activity.findViewById(layoutId);
		TableRow row = null;
		int index = 0;
		tableLayout.removeAllViews();
		//add items
		for (ToolItem itemData : toolItems) {
			if (0 == index % rowSize) {
				row = new TableRow(activity);
                row.setWeightSum(2.0f);
				tableLayout.addView(row);
			}
			FrameLayout itemView = addTableItemToRow(activity, row, itemData, rowSize, onClickListener);
			index++;
		}
		//add space
		for(int i = 0;i<(toolItems.size()%rowSize);i++)
		{
            FrameLayout itemView = addTableItemToRow(activity,row,new ToolItem(activity,-1),rowSize,null);
		}
	}

	private static FrameLayout addTableItemToRow(Activity activity, TableRow row, ToolItem itemData, int rowSize, OnClickListener onClickListener)
	{
        FrameLayout itemView = createTableItem(activity, row);
		Button button = (Button) itemView.findViewById(R.id.tool_button_enter);
        RelativeLayout container = (RelativeLayout) itemView.findViewById(R.id.tool_button_container);
		button.setTag(itemData.id);
		button.setText(itemData.name);

		Drawable icon = null;
		if(itemData.iconResId != 0) {
			icon = AppCompatDrawableManager.get().getDrawable(activity, itemData.iconResId);
			// set icon color
			icon.mutate();
			icon.setColorFilter(activity.getResources().getColor(R.color.toolitem_color), PorterDuff.Mode.SRC_ATOP);
		}
		button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
		// retain 20dp space between icon and text
		button.setCompoundDrawablePadding(20);

		ImageView recommend = (ImageView) itemView.findViewById(R.id.tools_button_recommend);
		recommend.setTag(TAG_RECOMMEND + itemData.id);
		if(itemData.id==-1) {
            container.setVisibility(View.INVISIBLE);
        }
		if (itemData.showRecommend) {
			recommend.setVisibility(View.VISIBLE);
		}
		button.setOnClickListener(onClickListener);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT, 1f);
        int margin_left, margin_right;
        margin_left =px(activity,3);
        margin_right=px(activity,3);
        if (row.getChildCount() == 0){
            margin_left = 0;
        }else if (row.getChildCount() +1 == rowSize){
            margin_right = 0;
        }
        params.setMargins(margin_left,0,margin_right,px(activity,8));
		row.addView(itemView, params);
        return itemView;
	}

    private static int px(Activity activity, float dips)
    {
        float DP = activity.getResources().getDisplayMetrics().density;
        return Math.round(dips * DP);
    }

	private static FrameLayout createTableItem(Activity activity, TableRow row) {
		return (FrameLayout) activity.getLayoutInflater().inflate(R.layout.tool_button, row, false);
	}

	public static View findRecommendViewByTag(Activity activity, int layoutId, int id) {
		// check the rule from method setTableLayoutContentView
		TableLayout tableLayout = (TableLayout) activity.findViewById(layoutId);
		return tableLayout.findViewWithTag(TAG_RECOMMEND + id);
	}
	public static View findViewByTag(Activity activity, int layoutId, int id) {
		// check the rule from method setTableLayoutContentView
		TableLayout tableLayout = (TableLayout) activity.findViewById(layoutId);
		return tableLayout.findViewWithTag(TAG_RECOMMEND + id);
	}
}
