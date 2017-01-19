package com.asus.remote.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.service.cloudstorage.common.MsgObj;


public class ChooseCloudStorageDialogFragment extends DialogFragment{

    private static final String TAG = "chooseCloudStorageDialogFragment";
    private String[] mItems;
    private TypedArray mIcons;
    private ArrayAdapter<String> mAdapter;

    public static ChooseCloudStorageDialogFragment newInstance() {
        ChooseCloudStorageDialogFragment fragment = new ChooseCloudStorageDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // +++ initialize mItems and adapter
        mItems = this.getResources().getStringArray(R.array.support_cloud_storages);

/*        AccountInfo[] skydriveAccount = RemoteAccount.getAccount(this.getActivity().getContentResolver(), MsgObj.TYPE_SKYDRIVE_STORAGE);
        if (skydriveAccount != null && skydriveAccount.length > 0) {
            String[] temp = new String[mItems.length - 1];
            for (int i=0 ; i<temp.length ; i++) {
                temp[i] = mItems[i];
            }
            mItems = null;
            mItems = temp;
        }*/
     /*   AccountManager accountManager = AccountManager.get(getActivity());
        Account[] accounts = accountManager.getAccounts();
        ArrayList<String> accountList =  (ArrayList<String>) Arrays.asList(mItems);
        if (accounts!=null) {
            for (Account account : accounts) {
                account.name.equals(arg0)
            }
        }*/

        mIcons = this.getResources().obtainTypedArray(R.array.support_cloud_storages_icon);

        mAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.choose_cloud_storage_list, mItems){

            ViewHolder holder;

            class ViewHolder {
                ImageView icon;
                TextView storage_type;
            }

            public View getView(int position, View converView, ViewGroup parent) {

                if (converView == null) {
                    converView = getActivity().getLayoutInflater().inflate(R.layout.choose_cloud_storage_list, null);
                    holder = new ViewHolder();
                    holder.icon = (ImageView) converView.findViewById(R.id.cloud_icon);
                    holder.storage_type = (TextView) converView.findViewById(R.id.cloud_storage_type);
                    converView.setTag(holder);
                } else {
                    holder = (ViewHolder) converView.getTag();
                }

                if (holder.icon != null) {
                    holder.icon.setImageDrawable(mIcons.getDrawable(position));
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
                FileListFragment fileListFragment;
                switch (which) {
                    case 0:
                        //((FileManagerActivity) getActivity()).triggerGoogleAccount();
                        RemoteAccountUtility.getInstance(getActivity()).addAccount(MsgObj.TYPE_GOOGLE_DRIVE_STORAGE);
                        break;
                    case 1:
                        RemoteAccountUtility.getInstance(getActivity()).addAccount(MsgObj.TYPE_DROPBOX_STORAGE);
                       /* RemoteFileUtility.sendCloudStorageMsg("", null, null, MsgObj.TYPE_DROPBOX_STORAGE, RemoteStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE);
                        fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                        fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOGGING_DIALOG);
                       */
                        break;
                    case 2:
                        RemoteAccountUtility.getInstance(getActivity()).addAccount(MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE);
                       /* RemoteFileUtility.sendCloudStorageMsg("", null, null, MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE, RemoteStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE);
                        fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                        fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOGGING_DIALOG);*/
                        break;
                    case 3:
                        RemoteAccountUtility.getInstance(getActivity()).addAccount(MsgObj.TYPE_SKYDRIVE_STORAGE);
                       /* RemoteFileUtility.sendCloudStorageMsg("", null, null, MsgObj.TYPE_SKYDRIVE_STORAGE, RemoteStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE);
                        fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                        fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOGGING_DIALOG);*/
                        break;
                    case 4:
                        RemoteAccountUtility.getInstance(getActivity()).addAccount(MsgObj.TYPE_HOMECLOUD_STORAGE);
                        /* RemoteFileUtility.sendCloudStorageMsg("", null, null, MsgObj.TYPE_SKYDRIVE_STORAGE, RemoteStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE);
                        fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                        fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOGGING_DIALOG);*/
                        break;
                    case 5:
                        RemoteAccountUtility.getInstance(getActivity()).addAccount(9);
                        /* RemoteFileUtility.sendCloudStorageMsg("", null, null, MsgObj.TYPE_SKYDRIVE_STORAGE, RemoteStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE);
                        fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                        fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOGGING_DIALOG);*/
                        break;
                }
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
       .create();
    }
}