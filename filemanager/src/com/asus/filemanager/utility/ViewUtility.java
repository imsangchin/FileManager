package com.asus.filemanager.utility;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.asus.filemanager.R;

import java.util.ArrayList;

public class ViewUtility {
    public static void calucateGridViewPadding(Point outPadding, Context context, int viewWidth, int colWidth) {
        // all in px
//      Log.v(TAG, "calucateGridViewPadding viewWidth=" + viewWidth);

        int size1 = viewWidth / colWidth;
        int paddingSum1 = (viewWidth - (colWidth * size1));
        int horizontalSpacing1 = paddingSum1 / (size1 + 1);
        int viewPadding1 = (paddingSum1 - (horizontalSpacing1 * (size1 - 1))) / 2;

//      int size2 = size1 - 1;
//      int paddingSum2 = (viewWidth - (colWidth * size2));
//      int horizontalSpacing2 = paddingSum2 / (size2 + 1);
//      int viewPadding2 = (paddingSum2 - (horizontalSpacing2 * (size2 - 1))) / 2;

//      Log.v(TAG, "size1=" + size1 + ", horizontalSpacing1=" + horizontalSpacing1 + ", viewPadding1=" + viewPadding1);
//      Log.v(TAG, "size2=" + size2 + ", horizontalSpacing2=" + horizontalSpacing2 + ", viewPadding2=" + viewPadding2);

        // default is result1
        outPadding.x = horizontalSpacing1;
        outPadding.y = viewPadding1;
    }

    public static int px2dp(Context context, int px) {
        return (int)(1.0 * px / context.getResources().getDisplayMetrics().density);
    }
    public static int dp2px(Context context, int dp) {
        return (int)(1.0 * dp * context.getResources().getDisplayMetrics().density);
    }
    public static int getGridWidth(Context context, int screenWidth, int horizontal_space, int mode) {
        int orientation = context.getResources().getConfiguration().orientation;
        int columnNum = 2;
        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            switch(mode) {
                case ConstantsUtil.MODE_PHONE:
                    columnNum = 2;
                    break;
                case ConstantsUtil.MODE_TABLET_SEVEN_INCH:
                    columnNum = 3;
                    break;
                case ConstantsUtil.MODE_TABLET:
                    columnNum = 4;
                    break;
            }
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switch(mode) {
                case ConstantsUtil.MODE_PHONE:
                    columnNum = 3;
                    break;
                case ConstantsUtil.MODE_TABLET_SEVEN_INCH:
                    columnNum = 4;
                    break;
                case ConstantsUtil.MODE_TABLET:
                    columnNum = 5;
                    break;
            }
        }
        return (screenWidth-(columnNum+1)*horizontal_space )/columnNum;
    }
    private static int minDip;

    public static int getMode(Context context) {
        DisplayMetrics mDm = new DisplayMetrics();
        WindowManager mWm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWm.getDefaultDisplay().getMetrics(mDm);
        int widthDip = (int) (mDm.widthPixels / mDm.scaledDensity);
        int heightDip = (int) (mDm.heightPixels / mDm.scaledDensity);
        minDip = widthDip;
        if (heightDip < minDip) minDip = heightDip;

        if (minDip <= 360) {
            return ConstantsUtil.MODE_PHONE;
        } else if (minDip <= 600) {
            return ConstantsUtil.MODE_TABLET_SEVEN_INCH;    //phone and 7" pad have same behavior
        } else {
            return ConstantsUtil.MODE_TABLET;
        }
    }
    public static void setOverflowButton(final Activity activity, final int resId, final boolean hasNewN) {
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if(decorView == null) return;
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        if(viewTreeObserver == null) return;
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                AppCompatImageView overflow=(AppCompatImageView)decorView.findViewById(R.id.overflow_menu_id);
                if (overflow == null) {
                    return;
                }
                if (hasNewN) {
                    Bitmap resBmp = combineImages(
                        BitmapFactory.decodeResource(activity.getResources(), resId),
                        BitmapFactory.decodeResource(activity.getResources(), R.drawable.asus_new_feature_icon)
                    );
                    overflow.setImageBitmap(resBmp);
                } else {
                    overflow.setImageResource(resId);
                }
                removeOnGlobalLayoutListener(decorView,this);
            }
        });

    }

    public static Bitmap combineImages(Bitmap left, Bitmap right) {
        Bitmap cs = null;
        int width, height = 0;
        width = left.getWidth();
        height = left.getHeight();
        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(cs);
        comboImage.drawBitmap(left, 0f, 0f, null);
        comboImage.drawBitmap(right, 0.56f*width, 0f, null);
        return cs;
    }

    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
    }

    public static void addNewIcon(Context context, MenuItem mi){
        SpannableStringBuilder s = new SpannableStringBuilder(mi.getTitle()+" ");
        s = ViewUtility.addNewIcon(context, s);
        mi.setTitle(s);
    }

    //hsinlin++
    public static SpannableStringBuilder addNewIcon(Context context, SpannableStringBuilder s){
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.asus_new_feature_icon);
        s.setSpan(new ImageSpan(context, bm, DynamicDrawableSpan.ALIGN_BASELINE) {

            @SuppressWarnings("deprecation")
            @Override
            public void draw(Canvas canvas, CharSequence text, int start,
                             int end, float x, int top, int y, int bottom, Paint paint) {
                // TODO Auto-generated method stub
                Drawable drawable = getDrawable();
                canvas.save();

                int transY = bottom - drawable.getBounds().bottom;
                transY -= paint.getFontMetricsInt().descent;

                canvas.translate(x, transY);
                drawable.draw(canvas);
                canvas.restore();
            }

        }, s.length() - 1, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return s;
    }
    public static void hideMenuItem(Menu menu,int menuid){
        MenuItem item= menu.findItem(menuid);
        if (null != item){
            item.setVisible(false);
        }
    }

    public static PopupWindow showTooltip(String text, View anchor, Context aContext, PopupWindow aPopWindow){
        LayoutInflater inflater = (LayoutInflater)aContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout= inflater.inflate(R.layout.popup_action_menu, null);
        TextView tv = (TextView)layout.findViewById(R.id.popview_content);
        tv.setText(text);
        if (aPopWindow == null){
            aPopWindow = new PopupWindow(layout);
            aPopWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            aPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            aPopWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            aPopWindow.setOutsideTouchable(true);
            aPopWindow.showAsDropDown(anchor);
        }else{
            if (!aPopWindow.isShowing()){
                aPopWindow.setContentView(layout);
                aPopWindow.showAsDropDown(anchor);
            }else{
                aPopWindow.dismiss();
                aPopWindow.setContentView(layout);
                aPopWindow.showAsDropDown(anchor);
            }
        }
        return aPopWindow;
    }

}
