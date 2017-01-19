package com.asus.filemanager.hiddenzone.activity;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public class SetupFingerprintDialog extends DialogFragment {
    public static final String TAG = "SetupFingerprintDialog";

    private SetupFingerprintDialogListener mListener;

    public interface SetupFingerprintDialogListener {
        public void onSetupFingerprintPressed();
    }

    public static SetupFingerprintDialog newInstance(SetupFingerprintDialogListener listener) {
        SetupFingerprintDialog fragment = new SetupFingerprintDialog(listener);
        return fragment;
    }

    public SetupFingerprintDialog(SetupFingerprintDialogListener listener) {
        super();
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.hidden_zone_setup_fingerprint_dialog, null);

        AlertDialog dialog = new AlertDialog.Builder(
                getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                .setView(view)
                .setPositiveButton(R.string.setup, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // go to settings
                        Intent intent = new Intent();
                        intent.setAction("asus".equalsIgnoreCase(Build.MANUFACTURER) ?
                                "android.settings.ASUS_FINGERPRINT_SETTINGS" : "android.settings.SETTINGS");
                        try {
                            startActivity(intent);
                            if (mListener != null) {
                                mListener.onSetupFingerprintPressed();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).create();

        return dialog;
    }
}
