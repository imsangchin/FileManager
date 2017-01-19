package com.asus.filemanager.utility;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class ShortCutHScrollView extends HorizontalScrollView {

    private GestureDetector mGestureDetector;
    private static final String TAG = "ShortCutHScrollView";
    private static final int ANGLE_THRESHOLD = 30;
    private static final int PI_ANGLE = 180;

    /**
     * @function ShortCutHScrollView constructor
     * @param context  Interface to global information about an application environment.
     */
    public ShortCutHScrollView(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(new HScrollDetector());
        setFadingEdgeLength(0);
    }

    /**
     * @function ShortCutHScrollView constructor
     * @param context Interface to global information about an application environment.
     * @param attrs A collection of attributes, as found associated with a tag in an XML document.
     */
    public ShortCutHScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(new HScrollDetector());
        setFadingEdgeLength(0);
    }

    /**
     * @function  ShortCutHScrollView constructor
     * @param context Interface to global information about an application environment.
     * @param attrs A collection of attributes, as found associated with a tag in an XML document.
     * @param defStyle style of view
     */
    public ShortCutHScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mGestureDetector = new GestureDetector(new HScrollDetector());
        setFadingEdgeLength(0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev) && mGestureDetector.onTouchEvent(ev);
    }

    // Return true if we're almost scrolling in the y direction, now is smaller than a 30 degree angle threshold
    class HScrollDetector extends SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if(Math.abs(Math.atan2(Math.abs(distanceY), Math.abs(distanceX)))/Math.PI * PI_ANGLE < ANGLE_THRESHOLD) {
                return true;
            }
            return false;
        }
    }
}