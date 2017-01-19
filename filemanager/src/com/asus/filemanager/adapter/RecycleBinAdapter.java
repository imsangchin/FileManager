package com.asus.filemanager.adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinDisplayItem;
import com.asus.filemanager.utility.FileUtility;

import java.util.List;

/**
 * Created by ChenHsin_Hsieh on 2016/2/19.
 */
public class RecycleBinAdapter extends DisplayItemAdapter<RecycleBinDisplayItem> {


    public RecycleBinAdapter(Context context, List<RecycleBinDisplayItem> files) {
        super(context, files);
    }

    @Override
    protected void updateView(ViewHolder holder, RecycleBinDisplayItem displayItem) {
        RecycleBinViewHolder recycleBinViewHolder = (RecycleBinViewHolder) holder;
        RecycleBinDisplayItem recycleBinDisplayItem = displayItem;
        recycleBinViewHolder.size.setText(FileUtility.bytes2String(context, recycleBinDisplayItem.getFileLength(), 1));
    }

    @Override
    protected View createViewAndSetViewHolder() {
        View view = LayoutInflater.from(context).inflate(R.layout.recyclebin_list_item, null);

        RecycleBinViewHolder holder = new RecycleBinViewHolder();
            holder.container = (RelativeLayout) view.findViewById(R.id.recycle_bin_list_item_root);
            holder.check = (CheckBox) view.findViewById(R.id.recycle_bin_list_item_checkbox);
            holder.name = (TextView) view.findViewById(R.id.recycle_bin_list_item_name);
            holder.icon = (ImageView) view.findViewById(R.id.recycle_bin_list_item_icon);
            holder.size = (TextView) view.findViewById(R.id.recycle_bin_list_item_size);
            holder.time = (TextView) view.findViewById(R.id.recycle_bin_list_item_time);
            holder.sdIndicator = (ImageView) view.findViewById(R.id.sd_indicator);
            view.setTag(holder);
        return view;
    }

    private class RecycleBinViewHolder extends ViewHolder {
        TextView size;
    }
}
