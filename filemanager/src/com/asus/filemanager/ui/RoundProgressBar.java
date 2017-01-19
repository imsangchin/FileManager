package com.asus.filemanager.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.View;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.utility.ThemeUtility;

public class RoundProgressBar extends View {

    private Paint paint;
    private int roundColor;
    private int roundOverlayColor;
    private int roundProgressColor;
    private int roundEndColor;
    private int usedTextColor;
    private int totalTextColor;
    private float usedTextSize;
    private float totalTextSize;
    private float roundWidth;
    private int max;
    private int progress;
    private boolean textIsDisplayable;
    private int style;
    private float textMargin;

    private Bitmap bitmap;
    private String usedText;
    private String totalText;

    public static final int STROKE = 0;
    public static final int FILL = 1;

    private boolean enable = true;

    public RoundProgressBar(Context context) {
        this(context, null);
    }

    public RoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context){
        setEnabled(context, true);

        usedTextSize = context.getResources().getDimension(R.dimen.category_local_storage_grid_item_round_progress_bar_used_text_size);
        totalTextSize = context.getResources().getDimension(R.dimen.category_local_storage_grid_item_round_progress_bar_total_text_size);
        roundWidth = context.getResources().getDimension(R.dimen.category_local_storage_grid_item_round_progress_bar_round_width);
        textMargin = context.getResources().getDimension(R.dimen.category_local_storage_grid_item_round_progress_bar_text_margin);
        roundOverlayColor = ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK ?
                context.getResources().getColor(R.color.dark_theme_circle_lightblue) :
                context.getResources().getColor(R.color.storage_list_item_bg);

        max = 100;
        textIsDisplayable = true;
        style = STROKE;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint = new Paint();

        //draw circle
        int centre = getWidth()/2;
        int radius = (int) (centre - roundWidth/2);
        paint.setColor(roundColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(roundWidth);
        paint.setAntiAlias(true);
        canvas.drawCircle(centre, centre, radius, paint);

        //draw a overlay circle when user clicked
        if (isEnabled() && isPressed()) {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(roundWidth);
            paint.setAntiAlias(true);
            if (VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                paint.setColor(roundOverlayColor);    // show ripple effect on LOLLIPOP and above
            }
            paint.setAlpha(65);
            canvas.drawCircle(centre, centre, radius, paint);
        }

        //draw progress Arc
        if(enable){
            float arc = 360 * progress / max;
            paint.setStrokeWidth(roundWidth);
            paint.setColor(roundProgressColor);
            RectF oval = new RectF(centre - radius, centre - radius, centre + radius, centre + radius);
            switch (style) {
                case STROKE:
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawArc(oval, -90, arc, false, paint);
                    break;
                case FILL:
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    if(progress !=0)
                        canvas.drawArc(oval, -90, arc, true, paint);
                    break;
            }

            //draw end
            paint.setColor(roundEndColor);
            canvas.drawArc(oval, -90 + arc, 5, false, paint);
        }

        //draw type image
        if(bitmap != null){
            float left = centre - bitmap.getWidth() / 2;
            float top = centre - bitmap.getHeight() - textMargin;
            canvas.drawBitmap(bitmap, left, top, paint);
        }

        //draw text
        if(usedText != null){
            paint.setStrokeWidth(0);
            paint.setColor(usedTextColor);
            paint.setTextSize(usedTextSize);
//          paint.setTypeface(Typeface.DEFAULT_BOLD);

            float textWidth = paint.measureText(usedText);
            if(textIsDisplayable && style == STROKE){
                canvas.drawText(usedText, centre - textWidth / 2, centre + 2*textMargin, paint);
            }
        }
        if(totalText != null){
            paint.setStrokeWidth(0);
            paint.setColor(totalTextColor);
            paint.setTextSize(totalTextSize);
//          paint.setTypeface(Typeface.DEFAULT_BOLD);

            float textWidth = paint.measureText(totalText);
            if(textIsDisplayable && style == STROKE){
                canvas.drawText(totalText, centre - textWidth / 2, centre + usedTextSize + 2*textMargin, paint);
            }
        }
    }


    public synchronized int getMax() {
        return max;
    }

    public synchronized void setMax(int max) {
        if(max < 0){
            this.max = 0;
        }
        this.max = max;
    }

    public synchronized int getProgress() {
        return progress;
    }

    /**
     *  set usedText, totalText, bitmap before this method
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        if(progress < 0){
            progress = 0;
        }
        if(progress > max){
            progress = max;
        }
        if(progress <= max){
            this.progress = progress;
            postInvalidate();
        }
    }

    public String getUsedText() {
        return usedText;
    }

    public void setUsedText(String usedText) {
        this.usedText = usedText;
    }

    public String getTotalText() {
        return totalText;
    }

    public void setTotalText(String totalText) {
        this.totalText = totalText;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getCricleColor() {
        return roundColor;
    }

    public void setCricleColor(int cricleColor) {
        this.roundColor = cricleColor;
    }

    public int getCricleProgressColor() {
        return roundProgressColor;
    }

    public void setCricleProgressColor(int cricleProgressColor) {
        this.roundProgressColor = cricleProgressColor;
    }

    public float getRoundWidth() {
        return roundWidth;
    }

    public void setRoundWidth(float roundWidth) {
        this.roundWidth = roundWidth;
    }

    public void setEnabled(Context context, boolean enable){
        super.setEnabled(enable);
        this.enable = enable;

        if(enable){
            if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
                roundColor = context.getResources().getColor(R.color.dark_theme_storage_progress_background);
                roundProgressColor = context.getResources().getColor(R.color.dark_theme_circle_lightblue);
                roundEndColor = context.getResources().getColor(R.color.dark_theme_circle_deepblue);
            }else {
                roundColor = context.getResources().getColor(R.color.category_local_storage_progress_background);
                roundProgressColor = context.getResources().getColor(R.color.category_local_storage_progress);
                roundEndColor = context.getResources().getColor(R.color.category_local_storage_progress_end);
            }

            usedTextColor = context.getResources().getColor(R.color.category_local_storage_text);
            totalTextColor = context.getResources().getColor(R.color.category_local_storage_text);

        } else{
            roundColor = context.getResources().getColor(R.color.category_local_storage_disable);
            roundProgressColor = roundColor;
            roundEndColor = roundColor;
            usedTextColor = roundColor;
            totalTextColor = roundColor;
        }
    }
}
