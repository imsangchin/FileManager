package com.asus.filemanager.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.samba.AddSambaStorageDialogFragment;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaItem;
import com.asus.filemanager.samba.util.SambaUtils;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.NtStatus;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class DeviceListAdapter extends BaseAdapter implements OnItemClickListener,OnItemLongClickListener,OnClickListener {

    public static final String TAG = "DeviceListAdapter";

    private FileListFragment mFragment;
    private ItemIcon mItemIcon;
    private Typeface mNormalType;
    private Typeface mBoldType;
    private int mOrientation = 0;
    private ArrayList<SambaItem> mSambaItemList = new ArrayList<>();
    private VFile[] mFileArray;

    private int mDeviceType = -1;
    public static final int DEVICE_SAMAB = 0;
    public static final int DEVICE_HOMEBOX = 1;
    private final String OFFLINE_GRAY = "#dedede";

    private class ViewHolder {
        TextView name;
        RelativeLayout fileItem;
        ImageView itemIcon;
        TextView deviceStatusInfo;
//      View editAction;
        ImageView editAction;
    }

    public DeviceListAdapter(FileListFragment fragment) {
        mFragment = fragment;
        mItemIcon = new ItemIcon(fragment.getActivity().getApplicationContext(), mFragment);
        mOrientation = fragment.getResources().getConfiguration().orientation;
        mNormalType = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        mBoldType = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
    }

    @Override
    public int getCount() {

        int count =  mSambaItemList.size();
        if(getDeviceType() == DEVICE_HOMEBOX) {
            count = (mFileArray == null) ? 0 : mFileArray.length;
        }
        return count;
    }

    @Override
    public Object getItem(int position) {

        Object item;
        if(getDeviceType() == DEVICE_HOMEBOX) {
            item = (mFileArray == null) ? null : mFileArray[position];
        } else {
            item = mSambaItemList.size() == 0 ? null : mSambaItemList.get(position);
        }
        return item;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mFragment.getActivity().getLayoutInflater().inflate(R.layout.device_list_item, null);
            holder = new ViewHolder();
            //holder.icon = (ImageView) convertView.findViewById(R.id.file_list_item_icon);
            holder.name = (TextView) convertView.findViewById(R.id.file_list_item_name);
            holder.fileItem = (RelativeLayout) convertView.findViewById(R.id.file_item);
            holder.itemIcon = (ImageView)convertView.findViewById(R.id.file_list_item_icon);
            holder.deviceStatusInfo = (TextView)convertView.findViewById(R.id.device_list_item_ifo);
            holder.editAction = (ImageView)convertView.findViewById(R.id.edit_login);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.name.setTypeface(null);
        }

        if(holder.fileItem != null) {
            holder.fileItem.setTag(position);
//          holder.fileItem.setOnClickListener(this);
            holder.fileItem.setLongClickable(false);
        }
        if (holder.itemIcon != null ) {
            if (getDeviceType() == DEVICE_HOMEBOX) {
                int resId ;
                int txtId ;
                int status = ((RemoteVFile)mFileArray[position]).getmDeviceStatus();
                Log.d(TAG, "getmDeviceStatus:"+status);
                if (status == ConstantsUtil.DeviceStatus.STATE_ONLINE) {
                    resId = R.drawable.asus_ic_online;
                    txtId = R.string.cloud_homebox_status__info_online;
                } else if(status == ConstantsUtil.DeviceStatus.STATE_NIC_ALIVE) {
                    resId = R.drawable.asus_ic_sleep;
                    txtId = R.string.cloud_homebox_status__info_Sleep;
                } else {
                    resId = R.drawable.asus_ic_offline;
                    txtId = R.string.cloud_homebox_status__info_offline;
                    holder.deviceStatusInfo.setTextColor(Color.parseColor(OFFLINE_GRAY));
                    holder.name.setTextColor(Color.parseColor(OFFLINE_GRAY));
                }
                holder.deviceStatusInfo.setText(txtId);
                holder.itemIcon.setImageResource(resId);
            }
        }

        // Typeface typeface = mBoldType;
        if (holder.name != null) {
            if (!mFragment.isMovingDivider()) {
                if(getDeviceType() == DEVICE_SAMAB) {
                    holder.name.setText(mSambaItemList.get(position).getPcName());
                    holder.deviceStatusInfo.setText(mSambaItemList.get(position).getIpAddress());
                } else {
                    holder.name.setText(mFileArray[position].getName());
                }
            }
            //holder.name.setTypeface(typeface);
            holder.name.setTag(position);
        }

        if(holder.editAction != null && mFragment != null && getDeviceType() == DEVICE_SAMAB) {
            if(!TextUtils.isEmpty(((SambaItem)getItem(position)).getAccount())) {
                holder.editAction.setImageResource(R.drawable.asus_samba_edit_login_btn);
                holder.editAction.setClickable(true);
                holder.editAction.setOnClickListener(this);
                holder.editAction.setTag(position);
            } else {
                holder.editAction.setImageResource(R.drawable.asus_ic_account_d);
                holder.editAction.setClickable(false);
            }
        }

        return convertView;
    }

//  @Override
//  public void onClick(View v) {
//      int selectPosition = (Integer) v.getTag();
//      switch(getDeviceType()){
//          case DEVICE_SAMAB:
//              String selectIp = mSambaItemList.get(selectPosition).getIpAddress();
//              if(SambaFileUtility.LastTimeLoginSuccess(selectIp)){
//                  SambaFileUtility.hideSambaDeviceListView();
//                  //RemoteFileUtility.isShowDevicesList = false;
//                  SambaFileUtility.startScanSambaServerFile();
//              }else{
//                  String pcName = mSambaItemList.get(selectPosition).getPcName();
//                  SambaFileUtility.tryToLoginWithoutPassword(selectIp, pcName);
//              }
//          break;
//          case DEVICE_HOMEBOX:
//
//              RemoteVFile deviceRemoteFile = (RemoteVFile)mFileArray[selectPosition];
//              int status = deviceRemoteFile.getmDeviceStatus();
//              if (status == ConstantsUtil.DeviceStatus.STATE_ONLINE || status == ConstantsUtil.DeviceStatus.STATE_NIC_ALIVE) {
//                  if (deviceRemoteFile != null && deviceRemoteFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
//                      // check network status
//                      if(!((FileManagerApplication)mFragment.getActivity().getApplication()).isNetworkAvailable()) {
//                          ((FileManagerActivity) mFragment.getActivity()).displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
//                          return;
//                      }
//                      ((RemoteVFile)deviceRemoteFile).setFromFileListItenClick(true);
//
//                  }
//                  RemoteFileUtility.isShowDevicesList = false;
//                  RemoteFileUtility.setListUIAction(-1);
//                  RemoteFileUtility.scanHomeCloudDevicesFile(deviceRemoteFile);
//              }else {
//                  ToastUtility.show(mFragment.getActivity(), R.string.cloud_homebox_status_offline);
//              }
//          break;
//          default :
//            break;
//      }
//  }

    public void updateSambaHostAdapter(ArrayList<SambaItem> item) {
        setDeviceType(DEVICE_SAMAB);
        mSambaItemList = item;
        notifyDataSetChanged();
    }

    public void updateHomeBoxDeviceAdapter(VFile[] deviceArray) {
        SambaFileUtility.updateHostIp = false;
        setDeviceType(DEVICE_HOMEBOX);
        mFileArray = deviceArray;
        notifyDataSetChanged();
    }

    public void setDeviceType(int type) {
        mDeviceType = type;
    }

    public int getDeviceType() {
        return mDeviceType;
    }

    @Override
    public void onItemClick(AdapterView<?> AdapterView, View view, int position, long arg3) {

        switch(getDeviceType()) {
            case DEVICE_SAMAB:
                SambaItem item = mSambaItemList.get(position);
                AddSambaStorageDialogFragment.saveSelectedItem(item);

                // Move logic into SambaItemClickTask.
                // new SambaItemClickTask(item, DeviceListAdapter.this).execute();

                SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
                if(sambaFileUtility.LastTimeLoginSuccess(item)){
                    sambaFileUtility.checkSavedMsgIfChanged();
                }else{
                    sambaFileUtility.tryToLoginWithoutPassword(item);
                }

                break;
            case DEVICE_HOMEBOX:
                RemoteVFile deviceRemoteFile = (RemoteVFile)mFileArray[position];
                int status = deviceRemoteFile.getmDeviceStatus();
                if (status == ConstantsUtil.DeviceStatus.STATE_ONLINE || status == ConstantsUtil.DeviceStatus.STATE_NIC_ALIVE) {
                    if (deviceRemoteFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                        // check network status
                        if(!((FileManagerApplication)mFragment.getActivity().getApplication()).isNetworkAvailable()) {
                            ((FileManagerActivity) mFragment.getActivity()).displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
                            return;
                        }
                        deviceRemoteFile.setFromFileListItenClick(true);

                    }
                    RemoteFileUtility.isShowDevicesList = false;
                    RemoteFileUtility.getInstance(mFragment.getActivity()).setListUIAction(-1);
                    RemoteFileUtility.getInstance(mFragment.getActivity()).scanHomeCloudDevicesFile(deviceRemoteFile);
                } else {
                    ToastUtility.show(mFragment.getActivity(), R.string.cloud_homebox_status_offline);
                }
                break;
            default :
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
            long arg3) {

        return true;
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.edit_login:
                int position = (Integer)v.getTag();
                AddSambaStorageDialogFragment loginDialog = AddSambaStorageDialogFragment.newInstance((SambaItem)getItem(position));
                loginDialog.show(mFragment.getActivity().getFragmentManager(), "AddSambaStorageDialogFragment");
                break;
            default:
                break;
        }
    }

    /**
     * Background task to handle list item click, to determine to surf workgroup or
     * start to login a server.
     */
    static class SambaItemClickTask extends AsyncTask<Void, Void, ArrayList<SambaItem>> {

        private SambaItem mSelectItem;
        private WeakReference<DeviceListAdapter> mAdapterRef;

        public SambaItemClickTask(@NonNull SambaItem item, @NonNull DeviceListAdapter adapter)
        {
            mSelectItem = item;
            mAdapterRef = new WeakReference<>(adapter);
        }

        @Override
        protected void onPreExecute()
        {
            DeviceListAdapter adapter = mAdapterRef.get();
            if(adapter != null && adapter.mFragment != null)
            {
                adapter.mFragment.setListShown(false);
            }
        }

        @Override
        protected ArrayList<SambaItem> doInBackground(Void... voids)
        {
            String url = mSelectItem.getIpAddress();
            SmbFile smbFile;
            SmbFile[] smbFiles;
            ArrayList<SambaItem> sambaItemList = new ArrayList<>();

            try
            {
                smbFile = new SmbFile(url, NtlmPasswordAuthentication.ANONYMOUS);

                Log.i(TAG, "SambaItemClickTask, SmbFile type: " + smbFile.getType());
                if(smbFile.getType() == SmbFile.TYPE_WORKGROUP)
                {
                    smbFiles = smbFile.listFiles();

                    if (smbFiles != null && smbFiles.length > 0)
                    {
                        for (SmbFile file : smbFiles)
                        {
                            String name = SambaUtils.getServerNameWithoutLastSlash(file.getName());
                            SambaItem item = new SambaItem(name, file.getURL().toString());
                            sambaItemList.add(item);
                        }
                    }
                }
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
                return null;
            }
            catch (SmbException e)
            {
                e.printStackTrace();
                Log.w(TAG, "SambaItemClickTask, exception, error: " + SambaUtils.convertSmbNtStatus(e.getNtStatus()));
                try
                {
                    // Try to do the same task again, but not use anonymous login.
                    smbFile = new SmbFile(url);
                    smbFiles = smbFile.listFiles();

                    if(smbFiles != null && smbFiles.length > 0)
                    {
                        for(SmbFile file : smbFiles)
                        {
                            String name = SambaUtils.getServerNameWithoutLastSlash(file.getName());
                            SambaItem item = new SambaItem(name, file.getURL().toString());
                            sambaItemList.add(item);
                        }
                    }
                }
                catch (SmbException e1)
                {
                    e1.printStackTrace();
                    int ntStatus = e.getNtStatus();
                    Log.w(TAG, "SambaItemClickTask, exception again, error: " + SambaUtils.convertSmbNtStatus(ntStatus));
                    if(ntStatus == NtStatus.NT_STATUS_UNSUCCESSFUL)
                    {
                        return null;
                    }
                }
                catch (MalformedURLException e1)
                {
                    e1.printStackTrace();
                    return null;
                }
            }
            return sambaItemList;
        }

        @Override
        protected void onPostExecute(ArrayList<SambaItem> smbItems)
        {
            SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);

            // Exception happen when connect to remote workgroup.
            if(smbItems == null)
            {
                DeviceListAdapter adapter = mAdapterRef.get();
                if(adapter != null)
                {
                    ToastUtility.show(adapter.mFragment.getActivity(), "Connection failed, please try again");
                    adapter.mFragment.setListShown(true);
                }
                return;
            }

            // In case the current url refer to a server, not a workgroup.
            if(smbItems.isEmpty())
            {
                if (sambaFileUtility.LastTimeLoginSuccess(mSelectItem))
                {
                    sambaFileUtility.checkSavedMsgIfChanged();
                }
                else
                {
                    sambaFileUtility.tryToLoginWithoutPassword(mSelectItem);
                }
            }
            // In case the current url refer to a workgroup, update list again.
            else
            {
                DeviceListAdapter adapter = mAdapterRef.get();
                if(adapter != null)
                {
                    adapter.updateSambaHostAdapter(smbItems);
                    adapter.mFragment.setListShown(true);
                }
            }
        }
    }
}
