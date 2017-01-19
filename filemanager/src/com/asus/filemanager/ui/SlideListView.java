package com.asus.filemanager.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Scroller;

import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.adapter.DeviceListAdapter;
import com.asus.remote.utility.RemoteFileUtility;

/**
 * @blog http://blog.csdn.net/xiaanming
 *
 * @author xiaanming
 *
 */
public class SlideListView extends ListView {

    public int slidePosition;

    private int downY;

    private int downX;

    private int screenWidth;

    private View itemView;

    private Scroller scroller;
    private static final int SNAP_VELOCITY = 600;

    private VelocityTracker velocityTracker;

    private boolean isSlide = false;
    private boolean mIsSliding = false;

    private int mTouchSlop;

    private SlideOutListener mSlideOutListener;

    private RemoveDirection removeDirection;

    private boolean mIsUp = true;

    private FileListFragment mFragment = null;


    public enum RemoveDirection {
        RIGHT, LEFT;
    }



    public SlideListView(Context context) {
        this(context, null);
    }

    public SlideListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        screenWidth = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
        scroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }


    /**
     *
     * @param removeListener
     */
    public void setSlideOutListener(SlideOutListener slideOutListener) {
        this.mSlideOutListener = slideOutListener;
    }

    public void setFileListFrament(FileListFragment fileListFrament){
        mFragment = fileListFrament;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(mFragment == null || mFragment.mActivity == null
                || mFragment.mActivity.CATEGORY_IMAGE_FILE.equals(mFragment.getmIndicatorFile())
                || mFragment.mActivity.CATEGORY_MUSIC_FILE.equals(mFragment.getmIndicatorFile())
                || mFragment.mActivity.CATEGORY_VIDEO_FILE.equals(mFragment.getmIndicatorFile())
                || mFragment.getListAdapter() instanceof DeviceListAdapter
                || mFragment.getmIndicatorFile().getAbsolutePath().equals(RemoteFileUtility.getInstance(mFragment.getActivity()).getHomeCloudRootPath())
                )
            return super.dispatchTouchEvent(event);

        if(mFragment.isItemsSelected()){
            return super.dispatchTouchEvent(event);
        }
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN: {
            addVelocityTracker(event);


            if (!scroller.isFinished()) {
                return super.dispatchTouchEvent(event);
            }
            downX = (int) event.getX();
            downY = (int) event.getY();

            slidePosition = pointToPosition(downX, downY);


            if (slidePosition == AdapterView.INVALID_POSITION) {
                return super.dispatchTouchEvent(event);
            }


            itemView = getChildAt(slidePosition - getFirstVisiblePosition());
            //new Thread(new LongPressTimerThread()).start();
            mIsUp = false;
            Log.d("Jack", "****** start thread******");
            break;
        }
        case MotionEvent.ACTION_MOVE: {

            if (Math.abs(getScrollVelocity()) > SNAP_VELOCITY
                    || (Math.abs(event.getX() - downX) > mTouchSlop && Math
                            .abs(event.getY() - downY) < mTouchSlop)) {
                isSlide = true;
                mIsSliding = true;
                Log.d("Jack", "*****set isSlide true******");


            }
            break;
        }
        case MotionEvent.ACTION_UP:
            recycleVelocityTracker();
            break;
        }

        return super.dispatchTouchEvent(event);
    }


    private void scrollRight() {
        removeDirection = RemoveDirection.RIGHT;
        final int delta = (screenWidth + itemView.getScrollX());

        scroller.startScroll(itemView.getScrollX(), 0, -delta, 0,
                Math.abs(delta));
        Log.d("Jack", "****** scrollRight ****");
        postInvalidate(); //
    }


    private void scrollLeft() {
        Log.d("Jack", "itemView.getScrollX() = " + itemView.getScrollX());

            removeDirection = RemoveDirection.LEFT;
            final int delta = (screenWidth - itemView.getScrollX());

            scroller.startScroll(itemView.getScrollX(), 0, delta, 0,
                    Math.abs(delta));
            postInvalidate(); //


    }


    private void scrollByDistanceX() {

        if (itemView.getScrollX() >= screenWidth / 50) {
            scrollLeft();
        } else if (itemView.getScrollX() <= -screenWidth / 50) {
            scrollRight();
        } else {

            itemView.scrollTo(0, 0);
            mIsSliding = false;
            Log.d("Jack", "***** is Slide = false ***");
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Log.d("Jack", "onTouchEvent isSlide: " + isSlide + "; mSliding is " + mIsSliding);

        // FIXME:
        // disable swipe to show menu
//        if (isSlide && slidePosition != AdapterView.INVALID_POSITION) {
//            addVelocityTracker(ev);
//            final int action = ev.getAction();
//            int x = (int) ev.getX();
//            switch (action) {
//            case MotionEvent.ACTION_MOVE:
//                int deltaX = downX - x;
//                downX = x;
//                itemView.scrollBy(deltaX, 0);
//
//                break;
//            case MotionEvent.ACTION_UP:
//                int velocityX = getScrollVelocity();
//                if (velocityX > SNAP_VELOCITY) {
//                    scrollRight();
//
//                } else if (velocityX < -SNAP_VELOCITY) {
//                    scrollLeft();
//                } else {
//                    scrollByDistanceX();
//                }
//                Log.d("Jack", "MotionEvent.ACTION_UP");
//
//                mIsUp = true;
//                Log.d("Jack", "Math.abs(getScrollVelocity()) = " + Math.abs(getScrollVelocity()));
//
//                isSlide = false;
//                recycleVelocityTracker();
//
//                break;
//            }
//
//
//            return true;
//        }

        return super.onTouchEvent(ev);
    }

    @Override
    public void computeScroll() {

        if (scroller.computeScrollOffset()) {

            itemView.scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();

            if (scroller.isFinished()) {
                if (mSlideOutListener == null) {
                    throw new NullPointerException("RemoveListener is null, we should called setRemoveListener()");
                }

                itemView.scrollTo(0, 0);
                //mIsSliding = false;
                mSlideOutListener.SlideOutItem(removeDirection, slidePosition, itemView);
            }
        }
    }

    /**
     *
     *
     * @param event
     */
    private void addVelocityTracker(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        velocityTracker.addMovement(event);
    }


    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    /**
     *
     *
     * @return
     */
    private int getScrollVelocity() {
        if (null == velocityTracker)
            return 0;
        velocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) velocityTracker.getXVelocity();
        return velocity;
    }


    public interface SlideOutListener {
        public void SlideOutItem(RemoveDirection direction, int position, View itemView);
    }

    public boolean isItemMoving(){
        Log.d("Jack", "********** isSlide: " + isSlide);
        return mIsSliding;
    }


    public void setSliding(boolean slide){
        mIsSliding = slide;
    }

}
