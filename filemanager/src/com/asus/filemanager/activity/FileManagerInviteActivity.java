package com.asus.filemanager.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.filemanager.ga.GaMenuItem;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.InviteHelper;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class FileManagerInviteActivity extends BaseActivity {

	private final int GOOGLE_INVITE_REQUEST_CODE = 1000;

	private Button googleInviteButton, facebookInviteButton, shareMoreButton;
	private ImageView qrcode;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ColorfulLinearLayout.setContentView(this,R.layout.activity_filemanger_invite, R.color.theme_color);
		initActionBar();
		findViews();
		setListener();
		checkVersionCN();
	}

	private void findViews() {
		googleInviteButton = (Button) findViewById(R.id.activity_filemanager_invite_google);
		facebookInviteButton = (Button) findViewById(R.id.activity_filemanager_invite_fb);
		shareMoreButton = (Button) findViewById(R.id.activity_filemanager_invite_more);
		qrcode = (ImageView) findViewById(R.id.activity_filemanager_invite_qrcode);
		
	}

	private void setListener() {
		googleInviteButton.setOnClickListener(onClickListener);
		facebookInviteButton.setOnClickListener(onClickListener);
		shareMoreButton.setOnClickListener(onClickListener);
	}

	private void checkVersionCN() {
		// TODO Auto-generated method stub
        if (WrapEnvironment.IS_CN_DEVICE) {
        	//set wandojia qr code
        	qrcode.setImageResource(R.drawable.asus_invite_qrcode_cn);
        	
        	//set google facebook button gone
        	googleInviteButton.setVisibility(View.GONE);
        	facebookInviteButton.setVisibility(View.GONE);
        }

		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status == ConnectionResult.SERVICE_INVALID || !InviteHelper.isGoogleInviteAvailable(this)) {
			googleInviteButton.setVisibility(View.GONE);
		}
	}
	
	private OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.activity_filemanager_invite_google:
				InviteHelper.inviteGoogle(FileManagerInviteActivity.this,GOOGLE_INVITE_REQUEST_CODE);
				GaMenuItem.getInstance().sendEvents(FileManagerInviteActivity.this, GaMenuItem.CATEGORY_NAME,
		        GaMenuItem.ACTION_INVITE_GOOGLE, null, null);
				break;
				
			case R.id.activity_filemanager_invite_fb:
				InviteHelper.inviteFB(FileManagerInviteActivity.this);
	            GaMenuItem.getInstance().sendEvents(FileManagerInviteActivity.this, GaMenuItem.CATEGORY_NAME,
	            GaMenuItem.ACTION_INVITEFB, null, null);
				break;
				
			case R.id.activity_filemanager_invite_more:
				InviteHelper.invite(FileManagerInviteActivity.this, "");
	            GaMenuItem.getInstance().sendEvents(FileManagerInviteActivity.this, GaMenuItem.CATEGORY_NAME,
	            GaMenuItem.ACTION_TELL_FRIENDS, null, null);
				break;
				
			default:
				break;
			}
		}
	};

	/** set ActionBar background and invisible title */
	private void initActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(this.getResources().getString(R.string.invite_friends));
		actionBar.setIcon(android.R.color.transparent);
		actionBar.setDisplayShowCustomEnabled(false);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
//		if (requestCode == GOOGLE_INVITE_REQUEST_CODE) {
//			if (resultCode == RESULT_OK) {
//				String[] ids = AppInviteInvitation.getInvitationIds(resultCode,
//						data);
//				Toast.makeText(getApplicationContext(),
//						R.string.google_invite_success_hint, Toast.LENGTH_SHORT)
//						.show();
//			} else {
//				Toast.makeText(getApplicationContext(),
//						R.string.google_invite_fail_hint, Toast.LENGTH_SHORT)
//						.show();
//			}
//		}
	}
}
