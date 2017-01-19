package com.asus.filemanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneDisplayItem;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.VFile;

import java.util.List;

/**
 * Created by Yenju_Lai on 2016/5/12.
 */
public class HiddenZoneAdapter extends DisplayItemAdapter<HiddenZoneDisplayItem> {
    public HiddenZoneAdapter(Context context, List<HiddenZoneDisplayItem> files) {
        super(context, files);
    }

    @Override
    protected void updateView(ViewHolder holder, HiddenZoneDisplayItem displayItem) {
        HiddenZoneViewHolder hiddenZoneViewHolder = (HiddenZoneViewHolder) holder;
        VFile file = displayItem.getCurrentVFile();
        if (file.isDirectory()) {
            int fileCount = file.listFiles() == null? 0 : file.listFiles().length;
            String itemString = context.getString(fileCount > 1? R.string.items : R.string.item);
            hiddenZoneViewHolder.description.setText(String.format("%s %s", String.valueOf(fileCount), itemString));
        }
        else
            hiddenZoneViewHolder.description.setText(FileUtility.bytes2String(context, file.length(), 1));
    }

    @Override
    protected View createViewAndSetViewHolder() {
        View view = LayoutInflater.from(context).inflate(R.layout.hiddenzone_list_item, null);

        HiddenZoneViewHolder holder = new HiddenZoneViewHolder();
        holder.container = (RelativeLayout) view.findViewById(R.id.hidden_zone_list_item_root);
        holder.check = (CheckBox) view.findViewById(R.id.hidden_zone_list_item_checkbox);
        holder.name = (TextView) view.findViewById(R.id.hidden_zone_list_item_name);
        holder.icon = (ImageView) view.findViewById(R.id.hidden_zone_list_item_icon);
        holder.description = (TextView) view.findViewById(R.id.hidden_zone_list_item_desc);
        holder.time = (TextView) view.findViewById(R.id.hidden_zone_list_item_time);
        holder.sdIndicator = (ImageView) view.findViewById(R.id.sd_indicator);
        view.setTag(holder);
        return view;
    }

    private class HiddenZoneViewHolder extends ViewHolder {
        TextView description;
    }
}
