package com.asus.remote.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.service.AccountAuthenticator.helper.CloudsProvider;
import com.asus.service.AccountAuthenticator.helper.IAsusAccountCallback;
import com.asus.service.AccountAuthenticator.helper.IAsusAccountHelper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.asus.filemanager.activity.FileManagerActivity;

public class AsusAccountHelper {

	private String TAG = "AsusAccountHelper";
	public static String KEY_MESSENGER_REPLYTO = "asus_account_messenger_replyTo";

	public static final String TOKEN_HELPER_SERVICE_ACTION = "com.asus.service.AccountAuthenticator.helper.IAsusAccountHelper";

	public static final String ASUS_SERVICE_ACCOUNT_TYPE = "com.asus.account.asusservice";
	public static final String TOKEN_TYPE_AAE = "com.asus.asusservice.aae";
	public static final String TOKEN_TYPE_AWS = "com.asus.asusservice.aws";

	public final static String ASUS_SKYDRIVE_ACCOUNT_TYPE = "com.asus.account.skydriver";
	public final static String ASUS_SKYDRIVE_AUTHTOKEN_TYPE = "com.asus.service.authentication.sd";

	public final static String ASUS_DROPBOX_ACCOUNT_TYPE = "com.dropbox.android.account";
	public final static String ASUS_DROPBOX_AUTHTOKEN_TYPE = "";


	public static final String ASUS_BAIDU_ACCOUNT_TYPE = "com.asus.account.baidupcs";
	public static final String ASUS_BAIDU_AUTHTOKEN_TYPE = "com.asus.service.authentication.bd";

	public static final String ASUS_YANDEX_ACCOUNT_TYPE = "com.asus.account.ydcloud";
	public static final String ASUS_YANDEX_AUTHTOKEN_TYPE = "com.asus.service.authentication.yd";

	public static final String ASUS_GOOGLE_ACCOUNT_TYPE = "com.google";
	public static final String ASUS_GOOGLE_AUTHTOKEN_TYPE = "oauth2: https://www.googleapis.com/auth/drive";

	public static final String ASUS_GOOGLE_DRIVE_VERIFY_URL = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=";

	public static final String INENT_ACTION_TOKEN_HELPER = "com.asus.service.tokenhelper";
	public static final String INENT_TOKEN_HELPER_PACKAGE = "com.asus.service.AccountAuthenticator";
	public static final String TOKEN_ACTION_COMMAND = "TOKEN_ACTION_COMMAND";

	public static final String KEY_TOKEN_ACTION_COMMAND = "TOKEN_ACTION_COMMAND";
	public static final String KEY_TOKEN_ACTION_RESULT = "KEY_TOKEN_ACTION_RESULT";

	public static final String TOKEN_ACTION_GET = "token_action_get";
	public static final String TOKEN_ACTION_INVALIDATE = "token_action_invalidate";
	public static final String TOKEN_ACTION_LOGIN = "token_action_login";
	public static final String TOKEN_ACTION_ISLOGIN = "token_action_is_login";
	public static final String TOKEN_ACTION_REFRESH = "token_action_refresh";
	public static final String TOKEN_ACTION_RESULT_CANCEL = "token_action_result_cancel";

	protected Context mContext;

	IAsusAccountHelper mService;

	public AsusAccountHelper(Context ctx) {
		mContext = ctx;
	}

	private IAsusAccountCallback mCallback = new IAsusAccountCallback.Stub() {
		/**
		 * This is called by the remote service regularly to tell us about new
		 * values. Note that IPC calls are dispatched through a thread pool
		 * running in each process, so the code executing here will NOT be
		 * running in our main thread like most other things -- so, to update
		 * the UI, we need to use a Handler to hop over there.
		 */
		public void onTokenResult(Map data) {
			Log.d(TAG, "onTokenResult");
			Bundle bundle = new Bundle();
			for (Object key : data.keySet()) {
				bundle.putString(key.toString(), data.get(key).toString());
			}

			Message msg = Message.obtain();
			msg.setData(bundle);
			msg.what = 1;
			handler.sendMessage(msg);

		}
	};

	private IAsusAccountCallback getCallback(){
		return new IAsusAccountCallback.Stub() {
			/**
			 * This is called by the remote service regularly to tell us about new
			 * values. Note that IPC calls are dispatched through a thread pool
			 * running in each process, so the code executing here will NOT be
			 * running in our main thread like most other things -- so, to update
			 * the UI, we need to use a Handler to hop over there.
			 */
			public void onTokenResult(Map data) {
				Log.d(TAG, "onTokenResult");
				Bundle bundle = new Bundle();
				for (Object key : data.keySet()) {
					bundle.putString(key.toString(), data.get(key).toString());
				}

				Message msg = Message.obtain();
				msg.setData(bundle);
				msg.what = 1;
				handler.sendMessage(msg);

			}
		};
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private  ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = IAsusAccountHelper.Stub.asInterface(service);
            RemoteAccountUtility.getInstance(null).initAvaliableCloudsInfo();
			Log.d(TAG, "ServiceConnection");
			// We want to monitor the service for as long as we are
			// connected to it.
//			try {
//				mService.setRemoteCallBack(mCallback);
//			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
//			}

			// As part of the sample, tell the user what happened.
			// Toast.makeText(mContext, "remote_service_connected",
			// Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;

			// As part of the sample, tell the user what happened.
			// Toast.makeText(mContext, "remote_service_disconnected",
			// Toast.LENGTH_SHORT).show();

		}
	};

	public void onActivityCreated() {
		Intent bindIntent = new Intent();

		if(WrapEnvironment.isAZSEnable(mContext)){
			bindIntent.setClassName("com.asus.server.azs", "com.asus.service.AccountAuthenticator.helper.TokenHelperService");
		}else {
			bindIntent.setClassName(mContext.getPackageName(), "com.asus.service.AccountAuthenticator.helper.TokenHelperService");
		}
		mContext.bindService(bindIntent,
				mConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
		Log.d(TAG, "oncreate mConnection:"+mConnection.hashCode());
	}

	public void onDestroy() {
		if(mConnection != null){
			mContext.unbindService(mConnection);
			Log.d(TAG, "destory mConnection:"+mConnection.hashCode());
		}
	}

	public void login(final String accountType, final String authTokenType) {
		Log.d(TAG, "login");
		// to do
		try {
			if(mService != null){
				mService.login(accountType, authTokenType, getCallback());
			}else {
				Log.d(TAG,"mService is null, waiting 2 seconds for ASUSAccount ini...");
				WaiteASUSAccountTask waiteASUSAccountTaskTask = new WaiteASUSAccountTask(accountType, authTokenType);
				waiteASUSAccountTaskTask.execute();
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	//use TokenUtils method instead of adil
	public void logout(final String accountType, final String authTokenType) {
		Log.d(TAG, "logout");
		// to do
		try {
			if(mService != null){
				mService.logout(accountType, authTokenType);
			}else {
				Log.d(TAG, "mService is null:logout");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

    class WaiteASUSAccountTask extends AsyncTask<Void,Integer,Integer>{
    	private String accountType1;
    	private String authTokenType1;
    	WaiteASUSAccountTask(String accountType, String authTokenType) {
    		accountType1 = accountType;
    		authTokenType1 = authTokenType;
        }
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Integer doInBackground(Void... params) {
            int i = 0;
            while(i < 20 && mService == null){
                try {
                    Thread.sleep(100);
                    i ++;
                    Log.d(TAG, "Waiting time  = " + i * 100 + "ms");
                } catch (InterruptedException e) {
                	e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
        	try {
	        	if(mService != null){
	        		mService.login(accountType1, authTokenType1, getCallback());
	        	}else{
	        		Log.e(TAG,"Time out, mService is null");
	        	}

        	} catch (RemoteException e) {
                e.printStackTrace();
            }catch (Exception e) {
				e.printStackTrace();
			}
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

	public Boolean isLogin(String accountType) {
		try {
			if(mService != null){
				return mService.isLogin(accountType);
			}else {
				Log.d(TAG, "mService is null:isLogin");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public void getAuthToken(String accountType, String authTokenType) {
		Log.d(TAG, "getAuthToken");
		try {
			if(mService != null){
				mService.getAuthToken(accountType, authTokenType, getCallback());
			}else {
				Log.d(TAG, "mService is null:getAuthToken");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void invalidateAuthToken(final String accountType,
			final String authTokenType) {
		Log.d(TAG, "invalidateAuthToken");
		try {
			if (mService != null) {
				mService.invalidateAuthToken(accountType, authTokenType);
			}else {
				Log.d(TAG, "mService is null:invalidateAuthToken");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void refreshAuthToken(final String accountType,
			final String authTokenType) {
		Log.d(TAG, "refreshAuthToken");
		try {
			if (mService != null) {
				mService.refreshAuthToken(accountType, authTokenType, getCallback());
				Log.d(TAG, "refreshAuthToken: Refresh token");
			}else {
				Log.d(TAG, "mService is null:refreshAuthToken");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void getAuthTokenByName(String accountName, final String accountType,
			final String authTokenType){
		Log.d(TAG, "getAuthTokenByName");
		try {
			if (mService != null) {
				mService.getAuthTokenByName(accountName, accountType, authTokenType, getCallback());
			}else {
				Log.d(TAG, "mService is null:getAuthTokenByName");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void refreshAuthTokenByName(String accountName, final String accountType,
			final String authTokenType) {
		try {
			if (mService != null) {
				mService.refreshAuthTokenByName(accountName, accountType, authTokenType, getCallback());
				Log.d(TAG, "refreshAuthTokenByName");
			}else {
				Log.d(TAG, "mService is null:refreshAuthTokenByName");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<String> getAvailableClouds(){
		List<String> avalilablecloudList = new ArrayList<String>();
		StringBuffer ret = new StringBuffer();
		try {
			ret.append("");
			//get list from service
			/*if (mService != null) {
				avalilablecloudList = mService.getAvailableCloud();
			}else {
				Log.d(TAG, "mService is null:getAvailableClouds");
			}*/

			if(!ConstantsUtil.IS_AT_AND_T){
			//get list from database
			avalilablecloudList = getAvailableCloudsFromDB();
			}

			if (avalilablecloudList != null && avalilablecloudList.size() > 0){
				for(String s : avalilablecloudList){
					ret.append(s).append("\n");
				}
				Log.d(TAG, ret.toString());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//return avalilablecloudList.subList(2, 4);
		return avalilablecloudList;
	}

	//caller_app_name could be filemanager,gallery,music
    public List<String> getAvailableCloudsFromDB(){
        List<String> list = new ArrayList<String>();
        Cursor cursor;

         Uri uri = Uri.parse("content://com.asus.service.account.clouds/available");

         if(WrapEnvironment.isAZSEnable(mContext)){
         	cursor = mContext.getContentResolver().query(uri, new String[]{"filemanager"}, null, null, null);
         }else{
         	//use databasehelper instead of contentprovider
         	CloudsProvider cp= CloudsProvider.getInstance(mContext);
         	cursor = cp.query(uri, new String[]{"filemanager"}, null, null, null);
         }

         if(cursor != null){
             while(cursor.moveToNext()){
                 String name = cursor.getString(cursor.getColumnIndex("name"));
                 list.add(name);
              }
             cursor.close();
         }
         return list;
    }

	// to be override in sub class
	protected void HandleTokenRequest(Bundle bundle) {

	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			Bundle bnd = msg.getData();

			HandleTokenRequest(bnd);

		}
	};

}
