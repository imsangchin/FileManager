package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.WindowManager;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

public class RestoreDialogFragment extends DialogFragment {

    private static final String TAG = "RestoreDialogFragment";

    public static RestoreDialogFragment newInstance() {
        RestoreDialogFragment fragment= new RestoreDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog;

        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        dialog = new ProgressDialog(getActivity(),
                themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);
        ((ProgressDialog) dialog).setMessage(getResources().getString(R.string.restore_progress));
        ((ProgressDialog) dialog).setIndeterminate(true);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setCancelable(false);

        return dialog;

    }
}
