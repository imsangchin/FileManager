package com.asus.filemanager.activity;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class HandleSearchIntentActivity extends BaseActivity {

	public static final String HANDLE_QSB_RESULT_ACTION = "asus.intent.action.show.result";
	public static final String OPEN_FM_FOLDER_ACTION = "open.file.folder.action";
	public static final String FILE_PATH = "path";

    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	Intent intent = getIntent();
    	Log.d("HandleSearchIntentActivity", "getAction "+ intent.getAction());
        if (HANDLE_QSB_RESULT_ACTION.equals(intent.getAction())){
            handleIntent(intent);
        } else{
        	ToastUtility.show(this, R.string.open_fail);
        	finish();
        }
    }


    private void handleIntent(Intent intent){
    	String filePath = intent.getDataString();
    	if(filePath != null){
    	    VFile file = new VFile(filePath);
    	    if(file.isDirectory()){
                Intent mIntent = new Intent(this,FileManagerActivity.class);
                mIntent.setAction(OPEN_FM_FOLDER_ACTION);
                mIntent.putExtra(FILE_PATH, file.getAbsolutePath());
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mIntent);
    	    }else{
                FileUtility.openFile(this, file, false, false);
    	    }
    	}
        finish();
    }
}
