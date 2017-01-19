package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;

public class FavoriteRemoveDialogFragment extends DialogFragment implements OnClickListener {

    public static final int TYPE_DELETE_DIALOG = 0;
    private static final String TAG = "FavoriteRemoveDialogFragment";

    public static FavoriteRemoveDialogFragment newInstance(EditPool arg, int typeDialog) {
        FavoriteRemoveDialogFragment fragment= new FavoriteRemoveDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("editpool", arg);
        args.putInt("type", typeDialog);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        EditPool editpool = (EditPool) getArguments().getSerializable("editpool");
        int type = getArguments().getInt("type");
        Dialog dialog;


            String msg;

            if (editpool.getFiles().length == 1) {
                String display_name;
                if (editpool.getFiles()[0].isFavoriteFile())
                    display_name = editpool.getFiles()[0].getFavoriteName();
                else
                    display_name = editpool.getFiles()[0].getName();
                msg = getString(R.string.remove_favorite_notice_one, display_name);
            } else {
                msg = getString(R.string.remove_favorite_notice_more, editpool.getFiles().length);
            }

            dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    //.setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.remove_favorite)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();


        return dialog;

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            EditPool editpool = (EditPool) getArguments().getSerializable("editpool");
            if (editpool != null && editpool.getSize() > 0) {
                int result = ProviderUtility.FavoriteFiles.removeFiles(getActivity().getContentResolver(), editpool.getFiles());
                if (result > 0) {
                    FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                    if (fileListFragment != null) {
                        FileManagerActivity activity = (FileManagerActivity) getActivity();
                        if (activity != null && activity.getIsShowSearchFragment()) {
                            //activity.reSearch(activity.getSearchQueryKey());
                            fileListFragment.loadCategoryFiles(activity.CATEGORY_FAVORITE_FILE, true);
                        } else {
                            fileListFragment.reScanFile();
                        }
                    }
                    ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.remove_favorite_success));

                    GaAccessFile.getInstance().sendEvents(getActivity(),
                            GaAccessFile.ACTION_REMOVE_FROM_FAVORITE, editpool.getFile().getVFieType(), -1, editpool.getSize());
                }
            }
        }
    }

}
