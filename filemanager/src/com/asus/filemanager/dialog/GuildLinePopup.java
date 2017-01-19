package com.asus.filemanager.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionProvider;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

public class GuildLinePopup {
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------
    public static class PolitCanvas extends FrameLayout {
        private static final int LINE_COLOR = 0xff02c0fe;
        private static final int LINE_STROKE_WIDTH = 2;
        private static final int CIRCLE_COLOR = 0xff02c0fe;
        private static final int CIRCLE_STROKE_WIDTH = 2;
        private static final int CIRCLE_RADIUS = 7;
        private static final float CAPACITY = 0.35f;

        private float mLineStartX;
        private float mLineStartY;
        private float mLineEndX;
        private float mLineEndY;

        private Paint mLinePaint;
        private Paint mCirclePaint;

        private int mCircleRadius = 7;

        private boolean mLineVisible = false;
        private Context mContext;

        public PolitCanvas(Context context) {
            super(context);
            this.mContext = context;

            // tricky
            setBackgroundColor(Color.TRANSPARENT);

            // Paint for drawing line
            mLinePaint = new Paint();
            mLinePaint.setColor(LINE_COLOR);
            mLinePaint.setAlpha((int)(255*CAPACITY));
            mLinePaint.setStrokeWidth(convertDPtoPX(LINE_STROKE_WIDTH, null));
            mLinePaint.setAntiAlias(true);
            mLinePaint.setDither(true);
            mLinePaint.setStyle(Paint.Style.FILL);

            // Paint for drawing circle
            mCirclePaint = new Paint();
            mCirclePaint.setColor(CIRCLE_COLOR);
            mCirclePaint.setAlpha((int)(255*CAPACITY));
            mCirclePaint.setStrokeWidth(convertDPtoPX(CIRCLE_STROKE_WIDTH, null));
            mCirclePaint.setAlpha((int)(255*CAPACITY));
            mCirclePaint.setAntiAlias(true);
            mCirclePaint.setDither(true);
            mCirclePaint.setStyle(Paint.Style.FILL);

            //
            mCircleRadius = convertDPtoPX(CIRCLE_RADIUS, null);
        }

        // this received position of the hover point.
        public void setGuideLine(float startX, float startY, float endX, float endY) {
            mLineStartX = startX;
            mLineStartY = startY;
            mLineEndX = endX;
            mLineEndY = endY;
            postInvalidateOnAnimation();
        }

        public void setGuideLineEndPoint(float endX, float endY) {
            mLineEndX = endX;
            mLineEndY = endY;
            postInvalidateOnAnimation();
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);

            //Log.v(TAG, "GuideLineContainer.draw(): "
            //        + "Line (" + mLineStartX + "," + mLineStartY + ") to (" + mLineEndX + "," + mLineEndY + ")");

            if (!isShowing()) {
                return;
            }

            canvas.save();
            canvas.drawLine(mLineStartX, mLineStartY, mLineEndX, mLineEndY, mLinePaint);
            mLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawCircle(mLineEndX, mLineEndY, mCircleRadius, mLinePaint);
            mLinePaint.setXfermode(null);
            canvas.drawCircle(mLineEndX, mLineEndY, mCircleRadius, mCirclePaint);
            canvas.restore();
        }

        public void setLineVisible(boolean visible) {
            mLineVisible = visible;
        }

        private boolean isShowing() {
            return mLineVisible;
        }

        protected int convertDPtoPX(int dp, DisplayMetrics displayMetrics) {
            if (displayMetrics == null) {
                displayMetrics = mContext.getResources().getDisplayMetrics();
            }
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
        }

        public float getCircleRadius() {
            return mCircleRadius;
        }
    }

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    static final String TAG = "GuildLinePopup";

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    PopupWindow mPopup;
    View mAnchorView = null;
    int mPolitX = 0;
    int mPolitY = 0;

    Rect mAnchorViewRect = new Rect();
    Rect mSeaRect = new Rect();
    PolitCanvas mSea;

    // record the gap from display to window
    protected int mWindowGapX;
    protected int mWindowGapY;

    // record the popup position of content view with background
    protected int mPopupPosX;
    protected int mPopupPosY;

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------
    public GuildLinePopup(Context ctx) {
        mPopup = new PopupWindow();
        mPopup.setTouchable(false);
        mPopup.setClippingEnabled(false);
        mPopup.setBackgroundDrawable(null);
        mSea = new PolitCanvas(ctx);
        mSea.setLayoutParams(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT
                        , FrameLayout.LayoutParams.MATCH_PARENT));
    }

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    public void setAnchorView(View anchorView) {
        mAnchorView = anchorView;
    }

    public void setPilotPoint(int x, int y) {
        mPolitX = x;
        mPolitY = y;
    }

    public void setLineVisible(boolean visible) {
        mSea.setLineVisible(visible);
    }

    public void preparePopup() {
        //
        View anchorView = mAnchorView;
      
        // get the position of anchor view
        int[] anchorLocInWindow = new int[2];
        int[] anchorLocOnScr = new int[2];
        anchorView.getLocationInWindow(anchorLocInWindow);
        anchorView.getLocationOnScreen(anchorLocOnScr);

        // use a location that in window
        mWindowGapX = anchorLocOnScr[0] - anchorLocInWindow[0];
        mWindowGapY = anchorLocOnScr[1] - anchorLocInWindow[1];

        // get anchorRect
        final Rect anchorRect = mAnchorViewRect;
        final int radius = (int) mSea.getCircleRadius() + 1;
        anchorRect.set(anchorLocInWindow[0] - radius
                , anchorLocInWindow[1] - radius
                , (anchorLocInWindow[0] + anchorView.getWidth() + radius)
                , (anchorLocInWindow[1] + anchorView.getHeight() + radius));

        // measure the range of sea
        Rect sea = mSeaRect;
        sea.set(anchorRect);
        sea.union(mPolitX, mPolitY);

        mSea.setGuideLine(mPolitX - mSeaRect.left
                , mPolitY - mSeaRect.top
                , mPolitX - mSeaRect.left
                , mPolitY - mSeaRect.top);
    }

    public void showPopup() {
        mPopup.setHeight(mSeaRect.bottom - mSeaRect.top);
        mPopup.setWidth(mSeaRect.right - mSeaRect.left);
        mPopup.setContentView(mSea);
        mPopup.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, mSeaRect.left, mSeaRect.top);
    }

    public void dismissPopup() {
        mPopup.dismiss();
    }

    public void hover(MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_HOVER_MOVE) {
            mSea.setGuideLineEndPoint(event.getRawX() - mSeaRect.left - mWindowGapX, event.getRawY() - mSeaRect.top - mWindowGapY);
            // Log.v(TAG, "hover x = " + (event.getRawX() - mSeaRect.left - mWindowGapX)
            //        + ", y = " + (event.getRawY() - mSeaRect.top - mWindowGapY));
        }
    }

    /*
    public void onHover(View anchor, MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_HOVER_ENTER) {
            setAnchorView(v);
            preparePopup();
            setLineVisible(true);
            showPopup();
            mSea.setLineVisible(true);
        } else if (action == MotionEvent.ACTION_HOVER_MOVE) {
            mSea.setGuideLineEndPoint(event.getRawX() - mSeaRect.left - mWindowGapX, event.getRawY() - mSeaRect.top - mWindowGapY);
        } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
            mSea.setLineVisible(false);
            dismissPopup();
        }
    }
    */
}
