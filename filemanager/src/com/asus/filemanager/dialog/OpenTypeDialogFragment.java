
package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;

public class OpenTypeDialogFragment extends DialogFragment implements OnClickListener {

    private static final String TAG = "OpenTypeDialogFragment";
    private static final boolean DEBUG = Config.DEBUG;

    private static VFile mfile;

    public static OpenTypeDialogFragment newInstance(VFile file) {
    	mfile = file;
        OpenTypeDialogFragment fragment = new OpenTypeDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	String[] types = new String[] {
    			getResources().getString(R.string.type_dialog_text),
    			getResources().getString(R.string.type_dialog_image),
    			getResources().getString(R.string.type_dialog_audio),
    			getResources().getString(R.string.type_dialog_video)
    	    };

        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
        .setTitle(R.string.type_dialog_title)
        .setNegativeButton(R.string.cancel, this)
        .setAdapter(new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,types), this)
        .create();

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    	String type = null;
    	switch(which){
    	case 0:
    		type = "text/*";
    		openFile(type);
    		break;
    	case 1:
    		type = "image/*";
    		openFile(type);
    		break;
    	case 2:
    		type = "audio/*";
    		openFile(type);
    		break;
    	case 3:
    		type = "video/*";
    		openFile(type);
    		break;

    	default:
			break;
    	}
    }

    private void openFile(String type){
    	Intent intent = new Intent(Intent.ACTION_VIEW);
    	try{
    		if("video/*".equals(type)){
    			intent.putExtra(Intent.EXTRA_TITLE, mfile.getName());
    		}
    		intent.setDataAndType(FileUtility.getUri(getActivity(), mfile, type, true), type);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    		getActivity().startActivityForResult(intent, FileManagerActivity.FILE_MANAGER_NORMAL);
    	}catch (Exception e) {
    		ToastUtility.show(getActivity(), R.string.open_fail);
    	}
    }
}
