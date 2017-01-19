
package com.asus.filemanager.adapter;

import com.asus.filemanager.R;
import com.asus.filemanager.dialog.UnZipDialogFragment;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.UnZipPreviewData;

import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class UnZipPreviewAdapter extends BaseAdapter implements OnClickListener{

    private static final String TAG = "ZipInfoAdapter";
    private static final boolean DEBUG = false;

    private class ViewHolder {
        View container;
        ImageView icon;
        TextView name;
        TextView size;
    }

    private UnZipDialogFragment mFragment;
    private ArrayList<UnZipPreviewData> mUnZipFileList;
    private UnZipPreviewData mCurrentFolder;

    public UnZipPreviewAdapter(UnZipDialogFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public int getCount() {
        return (mUnZipFileList == null) ? 0 : mUnZipFileList.size();
    }

    @Override
    public Object getItem(int position) {
        return (mUnZipFileList == null) ? null : mUnZipFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.unzip_preview_list_item, null);
            holder = new ViewHolder();
            holder.container = (View) convertView.findViewById(R.id.unzip_preview_container);
            holder.icon = (ImageView) convertView.findViewById(R.id.unzip_preview_list_item_icon);
            holder.name = (TextView) convertView.findViewById(R.id.unzip_preview_list_item_name);
            holder.size = (TextView) convertView.findViewById(R.id.unzip_preview_list_item_size);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mUnZipFileList != null && position < mUnZipFileList.size()) {
            boolean isFolder = (mUnZipFileList.get(position).isFolder()) ? true : false;

            if (holder.container != null) {
                holder.container.setTag(position);
                holder.container.setOnClickListener(this);
            }

            if (holder.icon != null) {
                holder.icon.setTag(position);
                holder.icon.setImageResource(MimeMapUtility.getIconRes(mUnZipFileList.get(position)));
            }

            if (holder.name != null) {
                holder.name.setTag(position);
                holder.name.setText(mUnZipFileList.get(position).getName());
            }

            if (holder.size != null) {
                holder.size.setTag(position);
                if (!isFolder) {
                    holder.size.setText(Formatter.formatFileSize(
                            mFragment.getActivity().getApplicationContext(),
                            mUnZipFileList.get(position).getSize()));
                } else {
                    holder.size.setText(null);
                }
            }
        }

        return convertView;
    }

    @Override
    public void onClick(View v) {
        int checkPosition;
        switch (v.getId()) {
        case R.id.unzip_preview_container:
            checkPosition = (Integer) v.getTag();

            if (DEBUG)
                Log.d(TAG, "isLoading? " + mFragment.getLoadingState());

            if (!mFragment.getLoadingState()) {
                if (mUnZipFileList.get(checkPosition).isFolder()) {
                    mFragment.updateDialog(mUnZipFileList.get(checkPosition));

                    if(mUnZipFileList.get(checkPosition).getChild() != null) {
                        updateAdapter(mUnZipFileList.get(checkPosition));
                    }
                } else {
                    mFragment.singlePreview(mUnZipFileList.get(checkPosition));
                }
            }
            break;
        default:
            break;
        }
    }

    public void updateAdapter(UnZipPreviewData folder) {
        if (!folder.isSorted()) {
            folder.sortChild();
            folder.setSort(true);
        }

        mUnZipFileList = folder.getChild();
        mCurrentFolder = folder;
        notifyDataSetInvalidated();
    }

    public UnZipPreviewData getCurrentFolder() {
        return mCurrentFolder;
    }
}
