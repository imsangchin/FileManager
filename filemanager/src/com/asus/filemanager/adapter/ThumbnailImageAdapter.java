package com.asus.filemanager.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import uk.co.senab.photoview.PhotoView;
import android.R.integer;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.graphics.*;

import com.asus.filemanager.R;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;

import android.support.v7.widget.RecyclerView;

public class ThumbnailImageAdapter extends RecyclerView.Adapter<ThumbnailImageAdapter.ViewHolder>{
    private Cursor mCursor;
    private ArrayList<File> mFileListArray;
    //private File[] mFileList;
    private DisplayImageOptions mDisplayImgOpt_default;
    private int mFocusedPos;

    public ThumbnailImageAdapter(Cursor aCursor) {
        mCursor = aCursor;
        mDisplayImgOpt_default= new DisplayImageOptions.Builder()
        .cacheOnDisk(true)
        .considerExifParams(true)
        .imageScaleType(ImageScaleType.EXACTLY)
        .showImageOnFail(R.drawable.broken_image)
        .resetViewBeforeLoading(true)
        .displayer(new FadeInBitmapDisplayer(300))
        .build();
        mFocusedPos = 0;
    }

    public ThumbnailImageAdapter(File[] aFileList) {
        mFileListArray = new ArrayList(Arrays.asList(aFileList));
        //mFileList = aFileList;
        mDisplayImgOpt_default= new DisplayImageOptions.Builder()
        .cacheOnDisk(true)
        .considerExifParams(true)
        .imageScaleType(ImageScaleType.EXACTLY)
        .showImageOnFail(R.drawable.broken_image)
        .resetViewBeforeLoading(true)
        .displayer(new FadeInBitmapDisplayer(300))
        .build();
        mFocusedPos = 0;
    }
     
    // Create new views (invoked by the layout manager)
    @Override
    public ThumbnailImageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View itemLayoutView = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.thumbnail_item, null);
 
        // create ViewHolder
        
        ViewHolder viewHolder = new ViewHolder(itemLayoutView);
        return viewHolder;
    }
 
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if (null != mCursor){
            mCursor.moveToPosition(position);
            int id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Images.Media._ID));
            Uri uri = Uri.withAppendedPath( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(id));
            
            if (viewHolder.imgViewIcon.getTag() == null || 
                    (viewHolder.imgViewIcon.getTag() !=null && ((String)(viewHolder.imgViewIcon.getTag())).compareToIgnoreCase(uri.toString()) != 0)){
                viewHolder.imgViewIcon.setTag(uri.toString());
                ImageLoader.getInstance().displayImage(uri.toString(), viewHolder.imgViewIcon, mDisplayImgOpt_default);
            }
            if (position == mFocusedPos){
                viewHolder.borderView.setVisibility(View.VISIBLE);
            }else{
                viewHolder.borderView.setVisibility(View.GONE);
            }
        }else if (null != mFileListArray){
            Uri uri = Uri.fromFile(mFileListArray.get(position));
		    String filepath = Uri.decode(uri.toString());

            if (viewHolder.imgViewIcon.getTag() == null || 
                    (viewHolder.imgViewIcon.getTag() !=null && ((String)(viewHolder.imgViewIcon.getTag())).compareToIgnoreCase(filepath) != 0)){
                viewHolder.imgViewIcon.setTag(filepath);
                ImageLoader.getInstance().displayImage(filepath, viewHolder.imgViewIcon, mDisplayImgOpt_default);
            }
            if (position == mFocusedPos){
                viewHolder.borderView.setVisibility(View.VISIBLE);
            }else{
                viewHolder.borderView.setVisibility(View.GONE);
            }
        }
 
    }
     
    // inner class to hold a reference to each item of RecyclerView 
    public static class ViewHolder extends RecyclerView.ViewHolder {
        
        public ImageView imgViewIcon;
        public View borderView;
         
        public ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            imgViewIcon = (ImageView) itemLayoutView.findViewById(R.id.item_icon);
            borderView = itemLayoutView.findViewById(R.id.item_border);
        }
    }
 
 
    // Return the size of your itemsData (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (null != mCursor)
            return mCursor.getCount();
        else if (null != mFileListArray)
            return mFileListArray.size();
        return 0;
    }

    public void setFocusedPos(int aPos){
        if (aPos <0 || aPos >= getItemCount()){
            aPos = mFocusedPos;
        }
        int oldPos = mFocusedPos;
        mFocusedPos = aPos;
        this.notifyItemChanged(oldPos);
        this.notifyItemChanged(mFocusedPos);
    }
    public int getFocusedPos(){
        return mFocusedPos;
    }
    public Uri getFocusedUri(){
        Uri uri = null;
        if (null != mCursor) {
            mCursor.moveToPosition(mFocusedPos);
            int id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Images.Media._ID));
            uri = Uri.withAppendedPath( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(id));
        }else if (null != mFileListArray){
            uri = Uri.fromFile(mFileListArray.get(mFocusedPos));
        }
        return uri;
    }
    public void fileDeleted(String aDeletedPath) {
        if (null != mCursor){
            mCursor.requery();
        }else if (null != mFileListArray){
            for (int i=0;i<mFileListArray.size();i++){
                if (mFileListArray.get(i).getAbsolutePath().compareTo(aDeletedPath) == 0){
                    mFileListArray.remove(i);
                    break;
                }
            }
        }
        this.notifyDataSetChanged();

    }
}