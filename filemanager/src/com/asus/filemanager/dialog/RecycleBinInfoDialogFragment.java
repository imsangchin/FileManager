
package com.asus.filemanager.dialog;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinDisplayItem;
import com.asus.filemanager.utility.DateUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;

public class RecycleBinInfoDialogFragment extends DialogFragment implements OnClickListener {


    public static final String TAG = "RecycleBinInfoDialog";
    public static final boolean DEBUG = false;
    private static final String KEY_FILE_NAME = "fileName";
    private static final String KEY_FILE_PATH = "filePath";
    private static final String KEY_FILE_LENGTH = "fileLength";
    private static final String KEY_FILE_DELETED_DATE = "fileDeletedTime";

    private View mInfoView;
    private boolean cancel = false;

    public static RecycleBinInfoDialogFragment newInstance(Context context, RecycleBinDisplayItem recycleBinItem) {
        RecycleBinInfoDialogFragment fragment =  new RecycleBinInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_FILE_NAME, recycleBinItem.getName());
        args.putString(KEY_FILE_PATH, FunctionalDirectoryUtility.getInstance().getLocalizedPath(
                context, recycleBinItem.getOriginalPath()));
        args.putLong(KEY_FILE_LENGTH, recycleBinItem.getFileLength());
        args.putLong(KEY_FILE_DELETED_DATE, recycleBinItem.getDisplayTime());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate");
        setRetainInstance(true);

    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            Log.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            Log.d(TAG, "onPause");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (DEBUG)
            Log.d(TAG, "onDismiss");
        if (getDialog() != null && getRetainInstance() && !cancel)
            return;
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (DEBUG)
            Log.d(TAG, "onCancel");
        cancel = true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) {
            Log.d(TAG, "onActivityCreate");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInfoView = inflater.inflate(R.layout.dialog_recyclebin_info, null);

        Bundle args = getArguments();
        int titleId = R.string.info;

        TextView fileNameView = (TextView) mInfoView.findViewById(R.id.info_file_name);
        fileNameView.setText(args.getString(KEY_FILE_NAME));

        TextView filePathView = (TextView) mInfoView.findViewById(R.id.info_file_path);
        filePathView.setText(FileUtility.changeToSdcardPath(args.getString(KEY_FILE_PATH)));

        TextView fileModifiedView = (TextView) mInfoView.findViewById(R.id.info_file_modified);
        fileModifiedView.setText(DateUtility.formatLongDateAndTime(getActivity(), args.getLong(KEY_FILE_DELETED_DATE)));

        TextView fileSizeView = (TextView) mInfoView.findViewById(R.id.info_file_size);
        fileSizeView.setText(FileUtility.bytes2String(getActivity(), args.getLong(KEY_FILE_LENGTH), 0));

        LinearLayout dialogContainer = (LinearLayout)mInfoView.findViewById(R.id.info_dialog_container);
        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

        dialogContainer.setPadding(spacing_left, spacing_top, spacing_right, spacing_bottom);


        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                //.setIcon(MimeMapUtility.getIconRes(mFile))
                .setTitle(titleId)
                .setPositiveButton(android.R.string.ok, this).create();

        //dialog.setView(mInfoView, spacing_left, spacing_top, spacing_right, spacing_bottom);
        dialog.setView(mInfoView);

//        GaAccessFile.getInstance(getActivity()).sendEvents(getActivity(),
//                GaAccessFile.ACTION_INFORMATION, mFile.getVFieType(), -1, 1);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        onCancel(dialog);

    }

}
