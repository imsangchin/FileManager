package com.asus.remote.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

public class AddCloudStorageDialogFragment extends DialogFragment{

    private static final String TAG = "AddCloudStorageDialogFragment";
    private String[] mItems;
    private ArrayAdapter<String> mAdapter;

    public static AddCloudStorageDialogFragment newInstance() {
        AddCloudStorageDialogFragment fragment = new AddCloudStorageDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // +++ initialize mItems and adapter
        mItems = this.getResources().getStringArray(R.array.support_cloud_storages);

        mAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.add_cloud_storage_list, mItems){

            ViewHolder holder;

            class ViewHolder {
                ImageView icon;
                TextView storage_type;
            }

            public View getView(int position, View converView, ViewGroup parent) {

                if (converView == null) {
                    converView = getActivity().getLayoutInflater().inflate(R.layout.add_cloud_storage_list, null);
                    holder = new ViewHolder();
                    holder.icon = (ImageView) converView.findViewById(R.id.cloud_icon);
                    holder.storage_type = (TextView) converView.findViewById(R.id.cloud_storage_type);
                    converView.setTag(holder);
                } else {
                    holder = (ViewHolder) converView.getTag();
                }

                if (holder.icon != null) {
                }

                if (holder.storage_type != null) {
                    holder.storage_type.setText(mItems[position]);
                }

                return converView;
            }
        };
        // ---


        return new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
        .setTitle(this.getResources().getString(R.string.add_cloud_storage_dialog_title))
        .setAdapter(mAdapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
       .create();
    }
}