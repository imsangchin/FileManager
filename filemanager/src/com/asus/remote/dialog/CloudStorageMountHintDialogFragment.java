package com.asus.remote.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.service.cloudstorage.common.MsgObj;

public class CloudStorageMountHintDialogFragment extends DialogFragment implements OnClickListener, OnItemClickListener{
    private static final String TAG = "CloudStorageMountHintDialogFragment";
    private int mCloudType;
    private List<String> mItems = new ArrayList<String>();

    public static CloudStorageMountHintDialogFragment newInstance(int cloudType) {
        CloudStorageMountHintDialogFragment fragment = new CloudStorageMountHintDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("cloudType", cloudType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");
        String cloudname = "";

        mCloudType = getArguments().getInt("cloudType");
        if (mCloudType > 0) {
            cloudname = RemoteAccountUtility.getInstance(getActivity()).findCloudTitleByMsgObjType(getActivity(), mCloudType);
            mItems = RemoteAccountUtility.getInstance(getActivity()).getUnmountedAccounts(mCloudType, getActivity());
        } else {
            Log.d(TAG, "cannot get the accout name");
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_cloud_storage_mount_hint, null);
        TextView viewHint = (TextView) view.findViewById(R.id.mount_hint);
        viewHint.setText(getResources().getString(R.string.cloud_storage_sign_in));

        //add google account
        if(mCloudType == MsgObj.TYPE_GOOGLE_DRIVE_STORAGE ){
            LinearLayout add_container = (LinearLayout)view.findViewById(R.id.add_account_linearlayout);
            add_container.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub

                    RemoteAccountUtility.getInstance(getActivity()).addGoogleAccount();
                    dismiss();
                }

            });
            add_container.setVisibility(View.VISIBLE);
        }

        if(mItems != null && mItems.size() > 0){
            ListView list = (ListView)view.findViewById(R.id.listview);
            list.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.cloud_storage_mount_list_item,mItems));
            list.setOnItemClickListener(this);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(this.getResources().getString(R.string.cloud_storage_sign_in_title, cloudname))
            /*TODO: setView
            .setView(view, 32, 8, 32, 8)
            */
            .setView(view)
            .setNegativeButton(android.R.string.cancel, this);
            //.setSingleChoiceItems(mItems, 0, this)
        if(mItems != null && mItems.size() == 1){
            builder.setPositiveButton(android.R.string.ok, this);
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == AlertDialog.BUTTON_POSITIVE) {
            mountAccount(0);
            dismiss();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
        mountAccount(arg2);
        dismiss();
    }

    private void mountAccount(int which){
        if(mItems != null && which < mItems.size()){
            String account_name = mItems.get(which);
            //startScanRoot(mCloudType, account_name);
            RemoteAccountUtility.getInstance(getActivity()).mountAccount(mCloudType, account_name, getActivity());
        }
    }

}
