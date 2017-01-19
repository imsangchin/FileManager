package com.asus.filemanager.samba;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.samba.provider.PcInfoDbHelper;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.remote.dialog.CloudStorageLoadingDialogFragment;

public class AddSambaStorageDialogFragment extends DialogFragment implements OnCheckedChangeListener{

    private static final String TAG = "AddSambaStorageDialogFragment";

    EditText domainEditText;
    EditText serverEditText;
    EditText usernameEditText;
    EditText passwordEditText;
    private static SambaItem mSelectItem = null;
    private static final String SAMBA_ITEM = "samba_item";
    private static String mSavedPc;
    private static String mSavedAccount;
    private boolean isRemember = false;

    public static AddSambaStorageDialogFragment newInstance() {
        AddSambaStorageDialogFragment fragment = new AddSambaStorageDialogFragment();
//        Bundle args = new Bundle();
//        fragment.setArguments(args);
//        args.putBoolean("edit_mode", false);
        return fragment;
    }
    
    public static void saveSelectedItem(SambaItem item){
    	mSelectItem = item;
    }

//	public static AddSambaStorageDialogFragment newInstance(String selectIp){
//		AddSambaStorageDialogFragment fragment = new AddSambaStorageDialogFragment();
//        Bundle args = new Bundle();
////        args.putBoolean("hasIp", true);
//        args.putString("selectIp", selectIp);
//        fragment.setArguments(args);
//		return fragment;
//	}
	
	public static AddSambaStorageDialogFragment newInstance(SambaItem item){
		AddSambaStorageDialogFragment fragment = new AddSambaStorageDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("edit_mode", true);
        mSelectItem = item;
        fragment.setArguments(args);
		return fragment;
	}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean isEditMode = false;
        Bundle data = getArguments();
        if(data != null){
            isEditMode = data.getBoolean("edit_mode");
        }
        
    	View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_smbserver, null);
    	
    	usernameEditText = (EditText)view.findViewById(R.id.username);
    	passwordEditText = (EditText)view.findViewById(R.id.password);
        CheckBox remembercheck = (CheckBox)view.findViewById(R.id.remember_check);
        remembercheck.setOnCheckedChangeListener(this);
        
    	if(mSelectItem != null){
    		usernameEditText.setText(mSelectItem.getAccount());
    		passwordEditText.setText(mSelectItem.getPassword());
    		mSavedPc = mSelectItem.getPcName();
    		mSavedAccount = mSelectItem.getAccount();
    		if(isEditMode || !TextUtils.isEmpty(mSavedAccount)){
    		    remembercheck.setChecked(true);
    		}
    	}

    	//AlertDialog dialog = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT)
		AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
        .setTitle(this.getResources().getString(R.string.server_title))
        .setView(view)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}
		})
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
       .create();

        return dialog;
    }

	@Override
	public void onStart() {
		super.onStart(); // super.onStart() is where dialog.show() is actually
							// called on the underlying dialog, so we have to do
							// it after this point
		AlertDialog d = (AlertDialog) getDialog();
		if (d != null) {
			Button positiveButton = (Button) d
					.getButton(Dialog.BUTTON_POSITIVE);
			positiveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Boolean wantToCloseDialog = false;
					String domain = "";
					String ip = "";
					String pcname = "";
					String username = null;
					String password = null;
					if(mSelectItem != null){
						ip = mSelectItem.getIpAddress();
						pcname = mSelectItem.getPcName();
					}
					
					if (usernameEditText.getText() != null){
						username = usernameEditText.getText().toString().trim();
					}

					if (passwordEditText.getText() != null){
						password = passwordEditText.getText().toString();
					}

					if (TextUtils.isEmpty(password)
							|| TextUtils.isEmpty(username)) {
						ToastUtility.show(AddSambaStorageDialogFragment.this
								.getActivity(), R.string.samba_input_error,
								Toast.LENGTH_SHORT);
						return;
					}

					FileListFragment fileListFragment;
					//SambaFileUtility.setFileListAdapter();
//					SambaFileUtility.updateHostIp = false;
					SambaFileUtility.getInstance(getActivity()).LoginInWindowsServer(domain, pcname,ip, username,
							password, SambaMessageHandle.MSG_SAMBA_LOGIN,isRemember);
					fileListFragment = (FileListFragment) getFragmentManager()
							.findFragmentById(R.id.filelist);
//					fileListFragment
//							.showDialog(
//									DialogType.TYPE_SAMBA_SORAGE_DIALOG,
//									CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);
					wantToCloseDialog = true;
					if (wantToCloseDialog){
						dismiss();
					}
					// else dialog stays open. Make sure you have an obvious way
					// to close the dialog especially if you set cancellable to
					// false.
				}
			});
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton button, boolean checked) {
		// TODO Auto-generated method stub
		if(!checked && !TextUtils.isEmpty(mSavedPc)){
		    PcInfoDbHelper.deleteAccountInfo(mSavedPc, mSavedAccount);
		    mSavedPc = "";
		    mSavedAccount = "";
		    
		}
		isRemember = checked;
	}
}