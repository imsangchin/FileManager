package com.asus.filemanager.activity;

import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.ui.AutoResizeTextView;
import com.asus.filemanager.utility.ConstantsUtil;

import java.util.List;

public class CategoryTableLayout {

    private static final String TAG = CategoryTableLayout.class.getSimpleName();

    public static final String kCountTag = "count";

    public static void setTableLayoutContentView(Activity activity, Fragment fragment, int layoutId, List<CategoryItem> categorys, int rowSize, OnClickListener onClickListener) {
        Resources res = activity.getResources();
        TableLayout tableLayout = (TableLayout) activity.findViewById(layoutId);
        TableRow row = null;
        int index = 0;
        tableLayout.removeAllViews();
        for (CategoryItem itemData: categorys) {
            if (0 == index % rowSize) {
                row = new TableRow(activity);
                tableLayout.addView(row);
            }
            if (itemData.isChecked) {
                RelativeLayout itemView = createTableItem(activity, row);
                ImageView icon = (ImageView)itemView.findViewById(R.id.icon);

                Drawable background = icon.getBackground();
                CategoryItem.setBackgroundColorAndRetainShape(activity.getResources().getColor(CategoryItem.findColorIdById(itemData.id)),background);

                icon.setTag(ConstantsUtil.ICON_TAG + itemData.id);
                AutoResizeTextView count = (AutoResizeTextView)itemView.findViewById(R.id.count);
                TextView name = (TextView)itemView.findViewById(R.id.name);
                itemView.setTag(itemData.id);
                itemView.setOnClickListener(onClickListener);
                fragment.registerForContextMenu(itemView);  // ++Hank: For creating shortcut
                icon.setImageDrawable(CategoryItem.findDrawableById(res, itemData.id));
                count.setTag(kCountTag + itemData.id);
                name.setText(itemData.name);
                row.addView(itemView,new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                ++index;
            }
        }
    }

    private static RelativeLayout createTableItem(Activity activity, TableRow row) {
        return (RelativeLayout)activity.getLayoutInflater().inflate(R.layout.category_item, row, false);
    }

    public static View findCountViewByTag(Activity activity, int layoutId, int id) {
        // check the rule from method setTableLayoutContentView
        TableLayout tableLayout = (TableLayout) activity.findViewById(layoutId);
        return tableLayout.findViewWithTag(kCountTag + id);
    }

}
