package com.asus.filemanager.adapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnViewTapListener;
import android.R.integer;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
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

public class ImagePagerAdapter extends PagerAdapter implements OnViewTapListener {
    private Cursor mCursor;
    //private File[]mFileList;
    private ArrayList<File> mFileListArray;
    private DisplayImageOptions mDisplayImgOpt_default;
    private float mPageWidthFract;
    private float mMaxPageWidth;
    private boolean mZoomable;

    public interface ImagePagerListener{
        public void onViewTap(View view, float x, float y);
    }
    private ImagePagerListener mListener;
   
    public void setImagePagerListener(ImagePagerListener listener){
        mListener = listener;
    }
    public ImagePagerAdapter(Cursor aCursor, boolean bZoomable) {
        mCursor = aCursor;
        mPageWidthFract = 1.0f;
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            config = config.RGB_565;
        }
        mDisplayImgOpt_default= new DisplayImageOptions.Builder()
        .cacheOnDisk(true)
        .considerExifParams(true)
        .imageScaleType(ImageScaleType.EXACTLY)
        .bitmapConfig(config)
        .showImageOnFail(R.drawable.broken_image)
        .displayer(new FadeInBitmapDisplayer(300))
        .build();
        mZoomable = bZoomable;
    }

    public ImagePagerAdapter(File[] aFileList, boolean bZoomable) {
        mFileListArray = new ArrayList(Arrays.asList(aFileList));
        mPageWidthFract = 1.0f;
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            config = config.RGB_565;
        }
        mDisplayImgOpt_default= new DisplayImageOptions.Builder()
        .cacheOnDisk(true)
        .considerExifParams(true)
        .imageScaleType(ImageScaleType.EXACTLY)
        .bitmapConfig(config)
        .showImageOnFail(R.drawable.broken_image)
        .displayer(new FadeInBitmapDisplayer(300))
        .build();
        mZoomable = bZoomable;
    }

    @Override
    public float getPageWidth(int position) {
        // TODO Auto-generated method stub
        return mPageWidthFract;
    }

    public void setPageWidth(float pageWidth, float pagerWidth){
       mPageWidthFract = pageWidth/pagerWidth; 
       mMaxPageWidth = pageWidth;

    }
	@Override
	public int getCount() {
	    if (null != mCursor)
	        return mCursor.getCount();
	    else if (null != mFileListArray)
	        return mFileListArray.size();
	    return 0;
	}

	@Override
	public View instantiateItem(ViewGroup container, int position) {
	    ImageView photoView = null;
	    if (mZoomable){
	        photoView = new PhotoView(container.getContext());
	        ((PhotoView)photoView).setOnViewTapListener(this);
	    }else{
	        photoView = new ImageView(container.getContext());
	    }
		container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		photoView.setMaxWidth((int)mMaxPageWidth);
		photoView.setMaxHeight((int)mMaxPageWidth);
		
		if (null != mCursor){
		    mCursor.moveToPosition(position);
		    int id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Images.Media._ID));
		    Uri uri = Uri.withAppendedPath( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(id));
		    ImageLoader.getInstance().displayImage(uri.toString(), photoView, mDisplayImgOpt_default);
		}else if (null != mFileListArray){
		    Uri uri = Uri.fromFile(mFileListArray.get(position));
		    String filepath = Uri.decode(uri.toString());
		    ImageLoader.getInstance().displayImage(filepath, photoView, mDisplayImgOpt_default);
		}
		
		return photoView;
	}

	@Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
	    Log.d("ImagePagerAdapter", "PrimaryItem= " + position);
        super.setPrimaryItem(container, position, object);
    }

    @Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		ImageLoader.getInstance().cancelDisplayTask((ImageView)object);;
		((ImageView)object).setImageBitmap(null);
		container.removeView((View) object);
		container = null;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View)object);
	}

    @Override
    public void onViewTap(View view, float x, float y) {
        if (null != mListener)
            mListener.onViewTap(view, x, y);
        Log.d("click", "onViewTap");
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

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}