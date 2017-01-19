package com.asus.filemanager.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.utility.ThemeUtility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;



public class CircleChartView extends View {

    //text hint
    private String textHintTop ;
	private String textHintBottom ;
	private int textHintColor;
	
	//percentage hint
	private int percentageHintColor;
	private float percentage;
    private DecimalFormat percentageFormat;

	//circle
    private int padding = 1;
	private int circleStartDegree = 0;
	private int circleBaseColor;
	private int circleFadeOutColor;
    private int animationMaskColor;
	private int circleLineStrokeWidth;
	private boolean showAnimationMask;
	private int currentAnimationDegree = 0 ;

	private final RectF mRectF;
    private final Paint mPaint;
    private ArrayList<ChartData> datas;
    
    //animation
    private Handler animationHandler;
    private long animationSpeed;

	//tmp value;
	private int tmpPercentageHintColor = percentageHintColor;
	private float tmpPercentage;
	private String tmpTextHintTop ;
	private String tmpTextHintBottom ;

    private HintTextAdapter percentageHintTextAdapter;
    private HintTextAdapter percentageLabelHintTextAdapter;
    private HintTextAdapter topHintTextAdapter;
    private HintTextAdapter bottomHintTextAdapter;

    public CircleChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRectF = new RectF();
        mPaint = new Paint();
        datas = new ArrayList<ChartData>();
        animationHandler = new Handler();
		percentageFormat =new DecimalFormat("#.##");

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleChartView);
        textHintColor = typedArray.getColor(R.styleable.CircleChartView_textHintColor, 0XFFAAAAAA);
        percentageHintColor = typedArray.getColor(R.styleable.CircleChartView_textHintColor, 0XFF888888);
        circleStartDegree = typedArray.getInt(R.styleable.CircleChartView_circleStartDegree, 0);
        circleFadeOutColor = typedArray.getColor(R.styleable.CircleChartView_circleFadeOutColor, 0X88ffffff);
        showAnimationMask = typedArray.getBoolean(R.styleable.CircleChartView_showAnimationMask, false);
        if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
            circleBaseColor = typedArray.getColor(R.styleable.CircleChartView_circleBaseColor, 0XFF9d9d9d);
            animationMaskColor = typedArray.getColor(R.styleable.CircleChartView_animationMaskColor, getResources().getColor(R.color.dark_theme_card_bg));

        }else {
            circleBaseColor = typedArray.getColor(R.styleable.CircleChartView_circleBaseColor, 0XFFD0D1D5);
            animationMaskColor = typedArray.getColor(R.styleable.CircleChartView_animationMaskColor, 0XFFffffff);
        }
        typedArray.recycle();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();
        
        if (width != height) {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }
        drawInitial(canvas,width,height);
        drawCircle(canvas, width, height);
        drawFadeOutCircle(canvas,width,height);
        if(showAnimationMask)
        {
        	drawInitial(canvas,width,height);
        	drawAnimationMask(canvas);
        }
        drawTextHints(canvas, width, height);
        
        
    }

    private void drawInitial(Canvas canvas,int width,int height)
    {
        circleLineStrokeWidth = width/6;
    	// initial draw property
        mPaint.setAntiAlias(true);
        canvas.drawColor(Color.TRANSPARENT);
        mPaint.setStrokeWidth(circleLineStrokeWidth);
        mPaint.setStyle(Style.STROKE);
//		mPaint.setStrokeCap(Paint.Cap.SQUARE);
//		mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        // initial draw circle range 
        float diff = circleLineStrokeWidth / 2 ;

        mRectF.left = diff +padding;
        mRectF.top = diff +padding ;
        mRectF.right = width - diff -padding;
        mRectF.bottom = height - diff -padding;
    }
    
    private void drawCircle(Canvas canvas,int width,int height)
    {
//    	float start = circleStartDegree;
//    	for(int i = 0 ; i<datas.size() ; i++)
//    	{
//    		float sweepAngle = datas.get(i).getPercentage()*360;
//    		mPaint.setColor(datas.get(i).getColor());
//			canvas.drawArc(mRectF,start,sweepAngle,false,mPaint);
//            start= start + sweepAngle;
//    	}
//    	float leftEnd = (360+circleStartDegree) - start;
//   		mPaint.setColor(circleBaseColor);
//        canvas.drawArc(mRectF, start, leftEnd, false, mPaint);
        //drawbase
        mPaint.setColor(circleBaseColor);
        canvas.drawArc(mRectF, 0, 360, false, mPaint);
        float sweepAngle = 0;

        for(int i = 0 ; i<datas.size() ; i++)
        {
            sweepAngle += datas.get(i).getInCircleAngle();
            datas.get(i).setSwapAngle(sweepAngle);
        }

        for(int i = datas.size()-1 ; i> -1 ; i--)
        {
            mPaint.setColor(datas.get(i).getColor());
            canvas.drawArc(mRectF, circleStartDegree, datas.get(i).getSwapAngle(), false, mPaint);
        }
    }
    
    private void drawFadeOutCircle(Canvas canvas,int width,int height)
    {
    	mPaint.setStrokeWidth(circleLineStrokeWidth/2);
    	float diff = circleLineStrokeWidth*3/4 ; 
    	mRectF.left = diff + padding;
        mRectF.top = diff +padding;
        mRectF.right = width - diff -padding;
        mRectF.bottom = height - diff -padding;
        mPaint.setColor(circleFadeOutColor);
		canvas.drawArc(mRectF, 0, 360, false, mPaint);
    }
    
    private void drawAnimationMask(Canvas canvas)
    {
    	
    	mPaint.setStrokeWidth(circleLineStrokeWidth+2);
    	mPaint.setColor(animationMaskColor);
		canvas.drawArc(mRectF, currentAnimationDegree, (360+circleStartDegree)-currentAnimationDegree, false, mPaint);
    }
    
    private void drawTextHints(Canvas canvas,int width,int height)
    {
        int centerWidth = width/2;
        int centerHeight = height/2;
        int adjust = 5;

        if(percentageHintTextAdapter ==null) percentageHintTextAdapter = new HintTextAdapter(percentageFormat.format(percentage),((width - (circleLineStrokeWidth*2)) / 3));
        if(percentageLabelHintTextAdapter ==null) percentageLabelHintTextAdapter = new HintTextAdapter("%",((width - (circleLineStrokeWidth*2)) / 5));
    	// draw center progress
        percentageHintTextAdapter.update(percentageFormat.format(percentage));
        mPaint.setTextSize(percentageHintTextAdapter.getTextSizes());
        mPaint.setColor(percentageHintColor);
        mPaint.setStyle(Style.FILL);
        canvas.drawText(percentageHintTextAdapter.getText(),
                centerWidth - percentageHintTextAdapter.getTextBounds().centerX() - adjust,
//                centerWidth - percentageHintTextAdapter.getTextBounds().centerX(),
                centerHeight , mPaint);

        // draw % need to smaller
        mPaint.setTextSize(percentageLabelHintTextAdapter.getTextSizes());
        mPaint.setColor(percentageHintColor);
        mPaint.setStyle(Style.FILL);
        canvas.drawText(percentageLabelHintTextAdapter.getText(),
//                centerWidth + percentageHintTextAdapter.getTextBounds().centerX() - percentageLabelHintTextAdapter.getTextBounds().centerX(),
                centerWidth + percentageHintTextAdapter.getTextBounds().centerX() - adjust,
                centerHeight , mPaint);

    	 if (!TextUtils.isEmpty(textHintTop)) {
             if(topHintTextAdapter ==null) topHintTextAdapter = new HintTextAdapter(textHintTop,((width - (circleLineStrokeWidth*2)) / 8));
             topHintTextAdapter.update(textHintTop);

             mPaint.setTextSize(topHintTextAdapter.getTextSizes());
             mPaint.setColor(textHintColor);
             mPaint.setStyle(Style.FILL);
             canvas.drawText(textHintTop,
                     centerWidth - topHintTextAdapter.getTextBounds().centerX(),
                     centerHeight/2 + topHintTextAdapter.getTextBounds().centerY(), mPaint);
         }

         if (!TextUtils.isEmpty(textHintBottom)) {
             if(bottomHintTextAdapter ==null) bottomHintTextAdapter = new HintTextAdapter(textHintBottom,((width - (circleLineStrokeWidth*2)) / 8));
             bottomHintTextAdapter.update(textHintBottom);

             mPaint.setTextSize(bottomHintTextAdapter.getTextSizes());
             mPaint.setColor(textHintColor);
             mPaint.setStyle(Style.FILL);
             canvas.drawText(textHintBottom,
                     centerWidth - bottomHintTextAdapter.getTextBounds().centerX(),
                     centerHeight + (bottomHintTextAdapter.getTextBounds().height() *2) , mPaint);
         }
    }



    public void addChartData(float percentage)
    {
    	Random r = new Random();
    	datas.add(new ChartData(percentage,Color.rgb(r.nextInt(255),r.nextInt(255),r.nextInt(255))));
    }
    
    public void addChartData(float percentage,int color)
    {
    	datas.add(new ChartData(percentage,color));
    }

	public void clearChartDatas()
	{
		datas.clear();
	}

    public void setPercentage(float percentage) {
        this.percentage = percentage;
        this.invalidate();
    }

    public String getTextHintTop() {
 		return textHintTop;
 	}

 	public void setTextHintTop(String textHintTop) {
 		this.textHintTop = textHintTop;
 	}
 	
    public String getTextHintBottom() {
		return textHintBottom;
	}

	public void setTextHintBottom(String textHintBottom) {
		this.textHintBottom = textHintBottom;
	}
	
	public int getTextHintColor() {
		return textHintColor;
	}

	public void setTextHintColor(int textHintColor) {
		this.textHintColor = textHintColor;
	}

	public void setPercentageHintColor(int percentageHintColor) {this.percentageHintColor = percentageHintColor;}

    public void setPercentageFormat(String pattern)
    {
        percentageFormat.applyPattern(pattern);
    }

	public void recordDefault()
	{
		tmpPercentageHintColor = percentageHintColor;
		tmpPercentage = percentage;
		tmpTextHintTop = textHintTop;
		tmpTextHintBottom = textHintBottom;
	}

	public void restoreDefault()
	{
		percentageHintColor = tmpPercentageHintColor ;
		percentage = tmpPercentage ;
		textHintTop =  tmpTextHintTop;
		textHintBottom = tmpTextHintBottom;
	}

	public void sortDatas()
	{
		Collections.sort(datas, new Comparator<ChartData>() {
			@Override
			public int compare(ChartData chartData1, ChartData chartData2) {

				return Float.compare(chartData2.getPercentage(), chartData1.getPercentage());
			}
		});
	}

	public void setShowAnimationMask(boolean showAnimationMask) {
		this.showAnimationMask = showAnimationMask;
	}
	
	public synchronized void showAnimationMaskDegree(int degree)
    {
    	currentAnimationDegree = degree;
    }
	/**
	 * 
	 * @param duration milliseconds
	 */
	public void startAnimation(long duration)
	{
		startAnimation(duration,0);
	}
	/**
	 * 
	 * @param duration milliseconds
	 * @param deleyDuration milliseconds
	 */
	public void startAnimation(long duration,long deleyDuration)
	{
		stopAnimation();
		animationSpeed = (long) (duration/36.0f);
		showAnimationMask = true;
		currentAnimationDegree = circleStartDegree;
		animationHandler.postDelayed(animationRunnable,deleyDuration);
	}
	public void stopAnimation()
	{
		animationHandler.removeCallbacks(animationRunnable);
		showAnimationMask = false;
		invalidate();
	}
	
	private Runnable animationRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			currentAnimationDegree+=10;
			if(currentAnimationDegree<(360+circleStartDegree))
				animationHandler.postDelayed(this, animationSpeed);
			else
				showAnimationMask = false;
			invalidate();
		}
	};
	
	private class ChartData{
		private float percentage;
        private float inCircleAngle;
        private int color;
        private float swapAnagle;
		public ChartData(float percentage,int color)
		{
			this.percentage = percentage;
			this.color = color;
            this.inCircleAngle = percentage * 360;
		}
		
		public float getPercentage() {
			return percentage;
		}
		public int getColor() {
			return color;
		}
        public float getInCircleAngle()
        {
            return inCircleAngle;
        }

        public void setSwapAngle(float swapAnagle)
        {
            this.swapAnagle = swapAnagle;
        }

        public float getSwapAngle()
        {
            return this.swapAnagle;
        }
	}

    private class HintTextAdapter {
        private Rect bounds;
        private float textSizes;
        private String text;

        public HintTextAdapter(String text, float textSizes)
        {
            this.text = text;
            this.textSizes = textSizes;
            setTextSizesBounds();
        }

        public void update(String text)
        {
            if(!this.text.equals(text))
            {
                this.text= text;
                setTextSizesBounds();
            }
        }

        public float getTextSizes()
        {
            return textSizes;
        }

        public Rect getTextBounds()
        {
            return bounds;
        }

        public String getText()
        {
            return text;
        }
        /**
         *
         * set word's bounds
         */
        private void setTextSizesBounds()
        {
            bounds = new Rect();
            Paint mPaint = new Paint();
            mPaint.setTextSize(textSizes);
            mPaint.getTextBounds(text, 0, text.length() , bounds);
        }
    }

}
