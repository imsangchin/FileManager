package com.asus.filemanager.dialog.delete;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

public class NoSpaceFragment extends RecycleConfirmFragment {

    @Override
    public Dialog createDialog(Bundle savedInstanceState) {
        isPermanentlyDelete = true;
        Dialog dialog;

        String msg;

        msg = getString(R.string.msg_too_big_to_recycle);

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
