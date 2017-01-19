package com.asus.filemanager.activity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;

import com.asus.filemanager.R;
import com.asus.filemanager.dialog.RequestSDPermissionDialogFragment;
import com.asus.filemanager.editor.Editable;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToolbarUtil;

public class AnalyzerAllFilesActivity extends BaseAppCompatActivity implements RequestSDPermissionDialogFragment.OnRequestSDPermissionFragmentListener, Editable {

    public static final String TAG = "AnalyzerAllFilesAct";

    public static final String TITLE_KEY = "TITLE_KEY";
    public static final String TOTAL_STORAGE_KEY = "TOTAL_STORAGE_KEY";
    public static final String ROOT_PATH_KEY = "ROOT_PATH_KEY";


    protected static final int EXTERNAL_STORAGE_PERMISSION_REQ = 1;
    protected static final int REQUEST_SDTUTORIAL = 2;

    private AnalyzerAllFilesFragment analyzerAllFilesFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "AnalyzerAllFilesActivity onCreate");
        super.onCreate(savedInstanceState);
        ColorfulLinearLayout.setContentView(this, R.layout.activity_analyzer_allfiles, R.color.theme_color);
        ColorfulLinearLayout.changeStatusbarColor(this, R.color.theme_color);
        findViews();
        initial();
//        initActionBar();
    }

    private void initial()
    {
        Log.i(TAG,"AnalyzerAllFilesActivity initial");
        Intent intent = getIntent();
        if(intent!=null) {
            String rootPath = intent.getStringExtra(ROOT_PATH_KEY);
            if(rootPath!=null) {
                String title = intent.getStringExtra(TITLE_KEY);
                if(title==null) {
                    title = this.getResources().getString(R.string.tools_storage_analyzer);
                }else{
                    if(title.equals(getString(R.string.internal_storage_title)))//internal storage force show sdcard
                        analyzerAllFilesFragment.setRootName(getString(R.string.device_root_path),"sdcard");
                    else
                        analyzerAllFilesFragment.setRootName(getString(R.string.device_root_path),title);
                }
                analyzerAllFilesFragment.setTitle(title);
                analyzerAllFilesFragment.initial(rootPath, intent.getLongExtra(TOTAL_STORAGE_KEY, 0));
            }
        }
    }

    private void findViews()
    {
        analyzerAllFilesFragment = (AnalyzerAllFilesFragment) getFragmentManager().findFragmentById(R.id.activity_analyzer_allfiles_fragment);
    }


    public void callSafChoose(int action){
        Log.i(TAG,"AnalyzerAllFilesActivity callSafChoose");
//        if(FileUtility.isFirstSDPermission(this)){
            RequestSDPermissionDialogFragment fragment = RequestSDPermissionDialogFragment.newInstance(action);
            fragment.setStyle(RequestSDPermissionDialogFragment.STYLE_NORMAL, R.style.FMAlertDialogStyle);
            fragment.show(getFragmentManager(), RequestSDPermissionDialogFragment.DIALOG_TAG);
//        }else{
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
//            SafOperationUtility.getInstance(this).setCallSafAction(action);
//        }
    }

    @Override
    public void onRequestConfirmed(int action, String deskLabel) {
        Log.i(TAG,"AnalyzerAllFilesActivity onRequestConfirmed");
        Intent tutorialIntent = new Intent();
        tutorialIntent.setClass(this, TutorialActivity.class);
        tutorialIntent.putExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, true);
        startActivityForResult(tutorialIntent, REQUEST_SDTUTORIAL);
        SafOperationUtility.getInstance(this).setCallSafAction(action);
    }

    @Override
    public void onRequestDenied() {
        //do nothing here, just cancel action
    }

    @Override
    public boolean onKeyDown(int keyCode,android.view.KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK)
            return analyzerAllFilesFragment.onBackPressed();
        return super.onKeyDown(keyCode,event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQ) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG,"AnalyzerAllFilesActivity EXTERNAL_STORAGE_PERMISSION_REQ");
                Uri treeUri = data.getData();
                DocumentFile rootFile = DocumentFile.fromTreeUri(this, treeUri);
                if(rootFile != null) {
                    getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    SafOperationUtility safOperationUtility = SafOperationUtility.getInstance(this);
                    //request saf action file != null
                    if (safOperationUtility.getChoosedFile() != null) {
                        //check permission again
                        if (safOperationUtility.isNeedToShowSafDialog(safOperationUtility.getChoosedFile().getAbsolutePath())) {
                            //no permission callSafChoose again
                            callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
                        } else {
                            //had permission
                            SafOperationUtility.getInstance(this).clearWriteCapMap();
                            analyzerAllFilesFragment.deleteFileInPopup();
                        }
                    } else {
                        //request saf action file = null(usually not happen) callSafChoose again
                        callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
                    }
                }
            }
        } else if (requestCode == REQUEST_SDTUTORIAL && resultCode == Activity.RESULT_OK) {
            Log.i(TAG,"AnalyzerAllFilesActivity REQUEST_SDTUTORIAL");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
        }
    }

    @Override
    public Handler getEditHandler() {
        AnalyzerAllFilesFragment analyzerAllFilesFragment = (AnalyzerAllFilesFragment) getFragmentManager().findFragmentById(R.id.activity_analyzer_allfiles_fragment);
        return analyzerAllFilesFragment == null? null : analyzerAllFilesFragment.getHandler();
    }

    @Override
    public EditorUtility.RequestFrom getRequester() {
        return EditorUtility.RequestFrom.StorageAnalyzer;
    }
}
