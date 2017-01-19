package com.asus.filemanager.dialog.delete;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.WindowManager;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

public class DeleteProgressFragment extends DeleteDialogFragment {

    @Override
    public Dialog createDialog(Bundle savedInstanceState) {
        Type type = (Type)getArguments().getSerializable("type");
        Dialog dialog;
        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        dialog = new ProgressDialog(getActivity(),
                themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);
        ((ProgressDialog) dialog).setMessage(getResources().getString(
                type == DeleteDialogFragment.Type.TYPE_PROGRESS_DIALOG ? R.string.delete_progress : R.string.move_to_trash_progress));
        ((ProgressDialog) dialog).setIndeterminate(true);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setCancelable(false);

        return dialog;
    }
}
