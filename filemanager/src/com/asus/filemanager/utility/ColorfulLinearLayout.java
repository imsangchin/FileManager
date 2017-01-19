package com.asus.filemanager.utility;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;

public class ColorfulLinearLayout extends LinearLayout {

    private int mInsetsBottom;

    public ColorfulLinearLayout(Context context) {
        super(context);
    }

    public ColorfulLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorfulLinearLayout(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        final int vis = getWindowSystemUiVisibility();
        final boolean stable = (vis & SYSTEM_UI_FLAG_LAYOUT_STABLE) != 0;
        final int lastInsetsBottom = mInsetsBottom;
        if (!stable) {
            mInsetsBottom = 0;
        } else {
            mInsetsBottom = insets.bottom;
        }
        if (mInsetsBottom != lastInsetsBottom) {
            MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
            lp.bottomMargin = mInsetsBottom;
            requestLayout();
        }
        return stable;
    }

    public static void setContentView(Activity activity, int resActivity, int resStatusColor) {
        if (isColorfulTextViewNeeded()) {
            try {
                ColorfulLinearLayout layout = new ColorfulLinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                View view = LayoutInflater.from(activity).inflate(resActivity, layout, false);

                TextView textViewColorful = new TextView(activity);
                textViewColorful.setId(R.id.textview_colorful);
                layout.removeAllViews();

                textViewColorful.setHeight(getColorfulLayoutHeight(activity));
                textViewColorful.setBackgroundColor(ContextCompat.getColor(activity, resStatusColor));
                layout.addView(textViewColorful);
                layout.addView(view);
                activity.setContentView(layout);
            } catch (InflateException e) {
                e.getStackTrace();
                activity.setContentView(resActivity);
            }
        } else {
            activity.setContentView(resActivity);
        }
    }

    // Note:
    // this method only support on KK/L/M,
    // because ICS/JB don't support to draw the statusbar.
    public static void changeStatusbarColor(Activity activity, int resStatusColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final View decorView = activity.getWindow().getDecorView();
            int[] windowPosition = new int[2];
            decorView.getLocationOnScreen(windowPosition);
            if (windowPosition[1] > 0)
                return;
            View view = new View(activity);
            final int statusBarHeight = getStatusBarHeight(activity);
            view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight));
            view.setBackgroundColor(ContextCompat.getColor(activity, resStatusColor));
            ((ViewGroup)decorView).addView(view);
        }
    }

    private static int getStatusBarHeight(Activity activity) {
        final Display display = activity.getWindowManager().getDefaultDisplay();
        if (display != null
                && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return 0;
        }
        // ignore status bar height for api level <= 18
        if(android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
            return 0;
        }
        int h = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height",
                "dimen", "android");
        if (resourceId > 0) {
            h = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return h;
    }

    private static int getActionBarHeight(@NonNull Activity activity) {

        if(activity.getActionBar() == null)
        {
            return 0;
        }

        int h;
        TypedValue tv = new TypedValue();
        activity.getBaseContext().getTheme().resolveAttribute(
                android.R.attr.actionBarSize, tv, true);
        h = activity.getResources().getDimensionPixelSize(tv.resourceId);
        return h;
    }

    public static int getColorfulLayoutHeight(Activity activity) {
        int statusBarHeight = getStatusBarHeight(activity);
        int actionBarHeight = (activity instanceof AppCompatActivity) ?
                getSupportActionBarHeight((AppCompatActivity) activity) : getActionBarHeight(activity);

        return statusBarHeight + actionBarHeight;
    }

    /**
     * Get support action bar height in {@link AppCompatActivity} if action bar exists.
     *
     * @param activity The desired {@link AppCompatActivity}.
     * @return The value of action bar height if exists, 0 otherwise.
     */
    private static int getSupportActionBarHeight(@NonNull AppCompatActivity activity)
    {
        if(activity.getSupportActionBar() == null)
        {
            return 0;
        }

        int h;
        TypedValue tv = new TypedValue();
        activity.getBaseContext().getTheme().resolveAttribute(
                android.R.attr.actionBarSize, tv, true);
        h = activity.getResources().getDimensionPixelSize(tv.resourceId);
        return h;
    }

    public static boolean isColorfulTextViewNeeded() {

        int version = Build.VERSION.SDK_INT;
        return version < android.os.Build.VERSION_CODES.LOLLIPOP;
    }
}
