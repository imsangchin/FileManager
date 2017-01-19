package com.asus.filemanager.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;

public class GmsAlertDialogFragment extends DialogFragment implements OnClickListener {

    public static final int TYPE_DELETE_DIALOG = 0;
    private static final String TAG = "GmsAlertDialogFragment";

    public static GmsAlertDialogFragment newInstance(int errorMsg) {
        GmsAlertDialogFragment fragment= new GmsAlertDialogFragment();
        Bundle args = new Bundle();
        args.putInt("errorMsg", errorMsg);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int errorMsg = getArguments().getInt("errorMsg");
        Log.e(TAG, "errorMsg = "+errorMsg);

//        int themeAsusLightDialogAlertId = ThemeUtility.sThemeAsusLightDialogAlertId;

        Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), errorMsg, 0);

//            dialog = new AlertDialog.Builder(getActivity(),
//                    themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId)
//                    //.setIconAttribute(android.R.attr.alertDialogIcon)
//                    .setTitle(R.string.remove_favorite)
//                    .setMessage(msg)
//                    .setPositiveButton(android.R.string.ok, this)
//                    .create();


        return dialog;

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }

}
