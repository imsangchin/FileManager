
package com.asus.remote.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteAccountUtility;

import java.util.List;

public class RemoteWiFiTurnOnDialogFragment extends DialogFragment implements OnClickListener {
    private static final String TAG = "RemoteWiFiTurnOnDialog";
    private static int sConnectType;

    public static RemoteWiFiTurnOnDialogFragment newInstance(int type) {
        RemoteWiFiTurnOnDialogFragment fragment = new RemoteWiFiTurnOnDialogFragment();
        sConnectType = type;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View)inflater.inflate(R.layout.dialog_wifi_turn_on_hint, null);
        TextView viewHint = (TextView) view.findViewById(R.id.wifi_turn_on_hint);

        String dialogTitle;
        if (sConnectType == VFileType.TYPE_CLOUD_STORAGE) {
            viewHint.setText(getResources().getString(R.string.wifi_p2p_connection_msg));
            dialogTitle = getString(R.string.wifi_p2p_connection_title);
        } else if (sConnectType == VFileType.TYPE_RECOMMEND_APP) {
            viewHint.setText(getResources().getString(R.string.no_network_connection_message));
            dialogTitle = getString(R.string.wifi_p2p_connection_title);
        } else {
            viewHint.setText(getResources().getString(R.string.httpserver_no_network));
            dialogTitle = getString(R.string.wifi_p2p_turn_on_wifi_title);
        }

        return new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                .setTitle(dialogTitle)
                /*TODO: setView
                .setView(view, 32, 8, 32, 8)
                */
                .setView(view)
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //WifiDirectSearchFragment wifiDirectSearchFragment = (WifiDirectSearchFragment)this.getFragmentManager().findFragmentById(R.id.wifidirectsearchlist);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                Log.d(TAG,"turn on WiFi");

                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                List<ResolveInfo> resInfo = this.getActivity().getPackageManager().queryIntentActivities(intent,0);
                if (null != resInfo && !resInfo.isEmpty()){
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

                /*don't try to turn on wifi by our self to remove change network state permission
                WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);
                */

                if (sConnectType == VFileType.TYPE_CLOUD_STORAGE) {
                    if (((FileManagerApplication)getActivity().getApplication()).isNetworkAvailable()) {
                        RemoteAccountUtility.getInstance(getActivity()).initAccounts();
                    }
                }

                break;
            case DialogInterface.BUTTON_NEGATIVE:
                //wifiDirectSearchFragment.updateProgressBar();
                break;
            default:
                break;
        }
    }
}
