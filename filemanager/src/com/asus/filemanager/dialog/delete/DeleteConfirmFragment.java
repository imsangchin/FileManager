package com.asus.filemanager.dialog.delete;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import com.asus.filemanager.R;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.utility.ThemeUtility;

public class DeleteConfirmFragment extends DeleteDialogFragment {

    @Override
    public Dialog createDialog(Bundle savedInstanceState) {
        isPermanentlyDelete = true;
        EditPool editpool = (EditPool) getArguments().getSerializable("editpool");
        Dialog dialog;

        String msg;

        if (editpool.getFiles().length == 1){
            if(editpool.getFiles()[0].isDirectory()){
                msg = getString(R.string.delete_one_directory);
            }else{
                msg = getString(R.string.delete_one_files);
            }
            msg = msg + "\n" + editpool.getFiles()[0].getName();
        } else {
            msg = getString(R.string.delete_more_files, editpool.getFiles().length);
        }

        dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                //.setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.delete_dialog)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();

        return dialog;
    }
}
