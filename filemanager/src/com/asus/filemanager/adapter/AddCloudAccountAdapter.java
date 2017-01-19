package com.asus.filemanager.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.remote.utility.RemoteAccountUtility;

public class AddCloudAccountAdapter extends BaseAdapter {

    private static final String TAG = AddCloudAccountAdapter.class.getSimpleName();

    private class ViewHolder {
        View container;
        ImageView icon;
        TextView name;
        TextView account_name;
        View divider;
        View addGoogleAccountPadding;
    }

    private String[] mCloudNames;
    private Drawable[] mCloudDrawables;
    private int[] mCloudTypes;

    public AddCloudAccountAdapter(Context context, String[] cloudNames) {
        mCloudNames = cloudNames;
        initCloudDrawablesAndTypes(context);
    }

    public void updateAdapter(Context context, String[] cloudNames) {
        mCloudNames = cloudNames;
        initCloudDrawablesAndTypes(context);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return (null == mCloudNames ? 0 : mCloudNames.length);
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return mCloudTypes[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.storage_list_item, null);
            holder = new ViewHolder();
            holder.container = (View) convertView
                    .findViewById(R.id.storage_list_item_container);
            holder.icon = (ImageView) convertView
                    .findViewById(R.id.storage_list_item_icon);
            holder.name = (TextView) convertView
                    .findViewById(R.id.storage_list_item_name);
            holder.account_name = (TextView) convertView
                    .findViewById(R.id.storage_list_item_account_name);
            holder.divider = (View) convertView
                    .findViewById(R.id.storage_list_item_divider);
            holder.addGoogleAccountPadding = (View) convertView
                    .findViewById(R.id.add_google_account_padding);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.container.setVisibility(View.VISIBLE);
            holder.addGoogleAccountPadding.setVisibility(View.GONE);
            // holder.name.setTypeface(null);
        }

        holder.icon.setVisibility(View.VISIBLE);
        holder.name.setVisibility(View.VISIBLE);
        holder.account_name.setVisibility(View.VISIBLE);
        holder.divider.setVisibility(View.GONE);
        ((LinearLayout) holder.container).setGravity(Gravity.CENTER);
        setItem(context, holder, mCloudNames[position], null);

        holder.icon.setImageDrawable(mCloudDrawables[position]);
//        holder.container.setTag(iconItem);
        return convertView;
    }

    private void setItem(Context context, ViewHolder holder, String cloudName, String accountName) {
        //holder.name.setTextColor(context.getResources().getColor(android.R.color.primary_text_light));
//      holder.name.setTextAppearance(android.R.attr.textAppearanceListItem);
        //holder.account_name.setTextColor(context.getResources().getColor(android.R.color.secondary_text_light));
//      holder.account_name.setTextAppearance(android.R.attr.textAppearanceListItemSecondary);
        if (accountName != null && !accountName.isEmpty()) {
            holder.account_name.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = holder.container.getLayoutParams();
            params.height = (int) context.getResources().getDimension(
                    R.dimen.storage_list_connected_height);
            holder.container.setLayoutParams(params);

            params = holder.name.getLayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            holder.name.setLayoutParams(params);

            params = holder.account_name.getLayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            holder.account_name.setLayoutParams(params);

            holder.name.setText(cloudName);
            holder.account_name.setText(accountName);

        } else {
            ViewGroup.LayoutParams params = holder.container.getLayoutParams();
            params.height = (int) context.getResources().getDimension(
                    R.dimen.file_list_height);
            holder.container.setLayoutParams(params);

            params = holder.name.getLayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            holder.name.setLayoutParams(params);

            holder.account_name.setVisibility(View.GONE);
            holder.name.setText(cloudName);

        }
    }

    private void initCloudDrawablesAndTypes(Context context) {
        if (mCloudNames != null) {
            mCloudDrawables = new Drawable[mCloudNames.length];
            for (int i = 0; i < mCloudDrawables.length; ++i) {
                mCloudDrawables[i] = RemoteAccountUtility.getInstance(null).findCloudDrawable(context, mCloudNames[i]);
            }
            mCloudTypes = new int [mCloudNames.length];
            for (int i = 0; i < mCloudTypes.length; ++i) {
                mCloudTypes[i] = RemoteAccountUtility.getInstance(null).findMsgObjTypeByCloudTitle(context, mCloudNames[i]);
            }
        }
    }
}
