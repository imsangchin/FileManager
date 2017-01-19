package com.asus.filemanager.ui;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

public class ActivityToast {

    private static final long LENGTH_SHORT = 2000;
    private static final long LENGTH_LONG = 5000;
    private static final int DEFAULT_ANIMATION_DURATION = 400;
    private static final String BUNDLE_IS_SHOWING = "com.android.mms.ui.ActivityToast.IS_SHOWING";

//    private final Activity mActivity;
    private FrameLayout.LayoutParams mLayoutParams;

    private Handler mHandler = new Handler();

    private ViewGroup mParent;
    private FrameLayout mToastHolder;
    private View mToastView;

    private Animation mShowAnimation;
    private Animation mCancelAnimation;

    private long mDuration = LENGTH_SHORT;

    private Animation.AnimationListener mShowAnimationListener;
    private Animation.AnimationListener mCancelAnimationListener;

    private boolean mIsAnimationRunning;
    private boolean mIsShown;

    private View.OnClickListener mOnClickListener = null;
    private View.OnClickListener mOnTimeoutListener = null;

    /**
     * @param activity Toast will be shown at top of the widow of this Activity
     */
    public ActivityToast(@NonNull Activity activity, View toastView) {
        mParent = (ViewGroup)activity.findViewById(android.R.id.content);
        mToastHolder = new FrameLayout(activity.getBaseContext());
        mLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.FILL_HORIZONTAL
        );
        mToastHolder.setLayoutParams(mLayoutParams);

        mShowAnimation = new AlphaAnimation(0.0f, 1.0f);
        mShowAnimation.setDuration(DEFAULT_ANIMATION_DURATION);
        mShowAnimation.setAnimationListener(mHiddenShowListener);

        mCancelAnimation = new AlphaAnimation(1.0f, 0.0f);
        mCancelAnimation.setDuration(DEFAULT_ANIMATION_DURATION);
        mCancelAnimation.setAnimationListener(mHiddenCancelListener);

        mToastView = toastView;
        mToastHolder.addView(mToastView);

        mToastHolder.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                cancel();
            }
        });
        mOnTimeoutListener = null;
        /*mToastHolder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    cancel();
                }
                return false;
            }
        });*/
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;

        mToastHolder.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(v);
                }
            }
        });
    }

    public void show() {
        if (!isShowing()) {
            mParent.addView(mToastHolder);
            mIsShown = true;

            if (mShowAnimation != null) {
                mToastHolder.startAnimation(mShowAnimation);
            } else {
                if (mDuration > 0) {
                    mHandler.postDelayed(mCancelTask, mDuration);
                }
            }
        }
    }

    public void cancel() {
        if (isShowing() && !mIsAnimationRunning) {
            if (mCancelAnimation != null) {
                mToastHolder.startAnimation(mCancelAnimation);
            } else {
                mParent.removeView(mToastHolder);
                mHandler.removeCallbacks(mCancelTask);
                mIsShown = false;
            }
        }
    }

    public boolean isShowing() {
        return mIsShown;
    }

    /**
     * Pay attention that Action bars is the part of Activity window
     *
     * @param gravity Position of view in Activity window
     */

    public void setGravity(int gravity) {
        mLayoutParams.gravity = gravity;

        if (isShowing()) {
            mToastHolder.requestLayout();
        }
    }

    public void setShowAnimation(Animation showAnimation) {
        mShowAnimation = showAnimation;
        mShowAnimation.setDuration(DEFAULT_ANIMATION_DURATION);
        mShowAnimation.setAnimationListener(mHiddenShowListener);
    }

    public void setCancelAnimation(Animation cancelAnimation) {
        mCancelAnimation = cancelAnimation;
        mCancelAnimation.setDuration(DEFAULT_ANIMATION_DURATION);
        mCancelAnimation.setAnimationListener(mHiddenCancelListener);
    }

    /**
     * @param cancelAnimationListener cancel toast animation. Note: you should use this instead of
     *                                Animation.setOnAnimationListener();
     */
    public void setCancelAnimationListener(Animation.AnimationListener cancelAnimationListener) {
        mCancelAnimationListener = cancelAnimationListener;
    }

    /**
     * @param showAnimationListener show toast animation. Note: you should use this instead of
     *                              Animation.setOnAnimationListener();
     */
    public void setShowAnimationListener(Animation.AnimationListener showAnimationListener) {
        mShowAnimationListener = showAnimationListener;
    }

    public void setDuration(long duration, View.OnClickListener listener) {
        mDuration = duration;
        mOnTimeoutListener = listener;
    }

    public View getView() {
        return mToastView;
    }

    public void saveInstanceState(@Nullable Bundle bundle) {
        if (bundle != null) {
            bundle.putBoolean(BUNDLE_IS_SHOWING, isShowing());
        }
    }

    public void restoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        boolean isShowing = savedInstanceState.getBoolean(BUNDLE_IS_SHOWING, false);
        if (isShowing) {
            mHandler.removeCallbacks(mCancelTask);
            mParent.addView(mToastHolder);
            mIsShown = true;
            if (mDuration > 0) {
                mHandler.postDelayed(mCancelTask, mDuration);
            }
        }
    }


    private Runnable mCancelTask = new Runnable() {
        @Override
        public void run() {
            cancel();
            if (mOnTimeoutListener != null) {
                mOnTimeoutListener.onClick(null);
            }
        }
    };

    private Animation.AnimationListener mHiddenShowListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            if (mShowAnimationListener != null) {
                mShowAnimationListener.onAnimationStart(animation);
            }

            mIsAnimationRunning = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mDuration > 0) {
                mHandler.postDelayed(mCancelTask, mDuration);
            }

            if (mShowAnimationListener != null) {
                mShowAnimationListener.onAnimationEnd(animation);
            }

            mIsAnimationRunning = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            if (mShowAnimationListener != null) {
                mShowAnimationListener.onAnimationRepeat(animation);
            }
        }
    };

    private Animation.AnimationListener mHiddenCancelListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            if (mCancelAnimationListener != null) {
                mCancelAnimationListener.onAnimationStart(animation);
            }

            mIsAnimationRunning = true;
        }

        @Override
        public void onAnimationEnd(final Animation animation) {
            // FIXME:
            // Don't remove view inside onAnimationEnd
            mParent.post(new Runnable() {
                @Override
                public void run() {
                    mParent.removeView(mToastHolder);
                    mHandler.removeCallbacks(mCancelTask);

                    if (mCancelAnimationListener != null) {
                        mCancelAnimationListener.onAnimationEnd(animation);
                    }

                    mIsAnimationRunning = false;
                    mIsShown = false;
                }
            });
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            if (mCancelAnimationListener != null) {
                mCancelAnimationListener.onAnimationRepeat(animation);
            }
        }
    };

    private static boolean hasNavigationBar(Activity activity) {
        Display d = activity.getWindowManager().getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        return (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
    }

    private static int getNavigationBarHeight(Activity activity) {
        final Display display = activity.getWindowManager().getDefaultDisplay();
        if (display != null && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return 0;
        }
        int h = 0;
        int resourceId = activity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            h = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return h;
    }
}