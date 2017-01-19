package com.asus.filemanager.activity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.VFile;

import java.util.List;

/**
 * Created by Wesley_Lee on 2016/12/12.
 */

public class ShoppingCartListAdapter extends BaseAdapter {
    private Context mContext;
    private List<VFile> mData;

    private class ViewHolder {
        TextView title;
        TextView description;
    }

    public ShoppingCartListAdapter(Context context, List<VFile> list) {
        mContext = context;
        mData = list;
    }

    @Override
    public int getCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return mData != null ? mData.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_shopping_cart, null);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) view.findViewById(R.id.title);
            viewHolder.description = (TextView) view.findViewById(R.id.description);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        bindDataToViewHolder(viewHolder, i);
        return view;
    }

    private void bindDataToViewHolder(ViewHolder viewHolder, int position) {
        VFile vFile = mData.get(position);
        viewHolder.title.setText(vFile.getName());
        viewHolder.description.setText(vFile.getPath());
    }
}
