package com.yalantis.phoenix.refresh_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.util.DisplayMetrics;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.yalantis.phoenix.PullToRefreshView;
import com.yalantis.phoenix.R;

/**
 * Created by Oleksii Shliama on 22/12/2014.
 * https://dribbble.com/shots/1650317-Pull-to-Refresh-Rentals
 */
public class SunRefreshView extends BaseRefreshView implements Animatable {

    private static final int ANIMATION_DURATION = 500;

    private final static float SKY_RATIO = 0.65f;
    private static final float SUN_INITIAL_ROTATE_GROWTH = 1.2f;

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Animation mAnimation;

    private int mSkyHeight;
    
    private int mTop;
    private int mScreenWidth;
    private int mScreenHight;

    private int mSunSize = 36;
    private int mEmptySize = 36;
    private int mEmptyHeight;
    private float mSunLeftOffset;
    
    private float mEmptyLeftOffset;

    private float mPercent = 0.0f;
    private float mRotate = 0.0f;

    private Bitmap mSun;
    private Bitmap mEmpty;
    
    private Paint paintText;
    private int textColor;
    private int textSize;

    private boolean isRefreshing = false;

    public SunRefreshView(Context context, PullToRefreshView parent) {
        super(context, parent);
        mParent = parent;
        mMatrix = new Matrix();
        textColor = context.getResources().getColor(R.color.color_text);
        textSize = context.getResources().getDimensionPixelOffset(R.dimen.text_size);
        createBitmaps();
        initiateDimens();
        
        setupAnimations();
    }

    private void initiateDimens() {
    	DisplayMetrics disply = getContext().getResources().getDisplayMetrics();
        mScreenWidth = disply.widthPixels;
        mScreenHight = disply.heightPixels;
        
        mEmptyLeftOffset = (mScreenWidth - mEmptySize) / 2;
        mSkyHeight = (int) (SKY_RATIO * mScreenWidth);
        mSunLeftOffset = (mScreenWidth - mSunSize) / 2;

        mTop = -mParent.getTotalDragDistance();
    }

    private void createBitmaps() {
        mSun = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.sun);
        mEmpty = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.empty_icon);
        mSunSize = mSun.getWidth();
        mEmptySize = mEmpty.getWidth();
        mEmptyHeight = mEmpty.getHeight();
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
        if (invalidate) setRotate(percent);
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        final int saveCount = canvas.save();
        canvas.translate(0, mTop);
        drawSun(canvas);
        if(mParent.isListViewisEmpty()){
        	drawEmpty(canvas);
        }
        canvas.restoreToCount(saveCount);
    }
   

    private void drawSun(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = mPercent;
        if (dragPercent > 1.0f) { // Slow down if pulling over set height
            dragPercent = (dragPercent + 9.0f) / 10;
        }

        float sunRadius = (float) mSunSize / 2.0f;
        float sunRotateGrowth = SUN_INITIAL_ROTATE_GROWTH;

        float offsetX = mSunLeftOffset;
        float offsetY = (mParent.getTotalDragDistance() / 2)  + mTop; // Depending on Canvas position

        if(offsetY > 27){
        	offsetY = 27;
        }
        
        matrix.postTranslate(offsetX, offsetY);

        offsetX += sunRadius;
        offsetY += sunRadius;

        matrix.postRotate((isRefreshing ? -360 : 360) * mRotate * (isRefreshing ? 1 : sunRotateGrowth), offsetX, offsetY);
        canvas.drawBitmap(mSun, matrix, null);
    }
    
    private void drawEmpty(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float offsetX = mEmptyLeftOffset;
        float offsetY = mScreenHight / 3 + (mParent.getTotalDragDistance() / 2); 

        matrix.postTranslate(offsetX, offsetY);

        canvas.drawBitmap(mEmpty, matrix, null);
        
        String emptyString = mParent.getEmptyString();
        if(emptyString != null){
        	if(paintText == null){
            	paintText = new Paint(Paint.ANTI_ALIAS_FLAG);  
            	paintText.setTextSize(textSize);
            	paintText.setColor(textColor);
            }
        	
        	float textLength = paintText.measureText(emptyString);  
            canvas.drawText(emptyString, (mScreenWidth - textLength) / 2 , offsetY + mEmptyHeight + textSize * 2, paintText);
        }
        
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
        invalidateSelf();
    }

    public void resetOriginals() {
        setPercent(0);
        setRotate(0);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, mSkyHeight + top);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
        resetOriginals();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setRotate(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.RESTART);
        mAnimation.setInterpolator(LINEAR_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

}
