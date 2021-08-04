package com.example.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Debug;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.InputStream;

public class DynamicViewPager extends ViewPager {
    private int backgroundId = -1;
    private int backgroundSaveId = -1;
    private int savedWidth = -1;
    private int savedHeight = -1;
    private int savedMaxNumPages = -1;
    private Bitmap savedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_albumart);
    private boolean insufficientMemory = false;

    private int maxNumPages = 0;
    private int imageHeight;
    private float zoomLevel;
    private float overlapLevel;
    private int currentPosition = -1;
    private float currentOffset = 0.0f;
    private Rect src = new Rect();
    private Rect dst = new Rect();

    private boolean pagingEnabled = true;
    private boolean parallaxEnabled = true;

    private final static String TAG = DynamicViewPager.class.getSimpleName();

    public DynamicViewPager(Context context) {
        super(context);
    }

    public DynamicViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!insufficientMemory && parallaxEnabled)
            setNewBackground();
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
        currentPosition = position;
        currentOffset = offset;
        setCanvasSrcDst();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!insufficientMemory && parallaxEnabled) {
            canvas.drawBitmap(savedBitmap, src, dst, null);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (savedBitmap != null) {
            savedBitmap.recycle();
            savedBitmap = null;
        }
        super.onDetachedFromWindow();
    }

    private void setCanvasSrcDst(){
        src.set((int) (overlapLevel * (currentPosition + currentOffset)), 0,
                (int) (overlapLevel * (currentPosition + currentOffset) + (getWidth() * zoomLevel)), imageHeight);

        dst.set((getScrollX()), 0, (getScrollX() + getWidth()), getHeight());
    }

    private int sizeOf(Bitmap data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return data.getRowBytes() * data.getHeight();
        } else {
            return data.getByteCount();
        }
    }

    private void setNewBackground() {
        if (backgroundId == -1)
            return;

        if (maxNumPages == 0)
            return;

        if (getWidth() == 0 || getHeight() == 0)
            return;

        if ((savedHeight == getHeight()) && (savedWidth == getWidth()) && (backgroundSaveId == backgroundId) &&
                (savedMaxNumPages == maxNumPages))
            return;

        InputStream is;

        try {
            is = getContext().getResources().openRawResource(backgroundId);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            imageHeight = options.outHeight;
            int imageWidth = options.outWidth;
            Log.v(TAG, "imageHeight=" + imageHeight + ", imageWidth=" + imageWidth);

            zoomLevel = ((float) imageHeight) / getWidth();  // always in 'fitXY' mode

            options.inJustDecodeBounds = false;
            options.inSampleSize = Math.round(zoomLevel);

            if (options.inSampleSize > 1) {
                imageHeight = imageHeight / options.inSampleSize;
                imageWidth = imageWidth / options.inSampleSize;
            }
            Log.v(TAG, "imageHeight=" + imageHeight + ", imageWidth=" + imageWidth);

            double max = Runtime.getRuntime().maxMemory(); //the maximum memory the app can use
            double heapSize = Runtime.getRuntime().totalMemory(); //current heap size
            double heapRemaining = Runtime.getRuntime().freeMemory(); //amount available in heap
            double nativeUsage = Debug.getNativeHeapAllocatedSize();
            double remaining = max - (heapSize - heapRemaining) - nativeUsage;

            int freeMemory = (int) (remaining / 1024);
            int bitmapSize = imageHeight * imageWidth * 4 / 1024;
            Log.v(TAG, "freeMemory = " + freeMemory);
            Log.v(TAG, "calculated bitmap size = " + bitmapSize);

            // not going to use more than one fifth of free memory
            if (bitmapSize > freeMemory / 5) {
                insufficientMemory = true;
                return;
            }

            zoomLevel = ((float) imageHeight) / getWidth();  // always in 'fitXY' mode
            // how many pixels to shift for each panel
            overlapLevel = zoomLevel * Math.min(Math.max(imageWidth / zoomLevel - getWidth(), 0) / (maxNumPages - 1), getWidth() / 2);

            is.reset();
            savedBitmap = BitmapFactory.decodeStream(is, null, options);
            Log.i(TAG, "real bitmap size = " + sizeOf(savedBitmap) / 1024);
            Log.v(TAG, "saved_bitmap.getHeight()=" + savedBitmap.getHeight() + ", saved_bitmap.getWidth()=" + savedBitmap.getWidth());

            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Cannot decode: " + e.getMessage());
            backgroundId = -1;
            return;
        }

        savedHeight = getHeight();
        savedWidth = getWidth();
        backgroundSaveId = backgroundId;
        savedMaxNumPages = maxNumPages;
    }

    public void setMaxPages(int numMaxPages) {
        maxNumPages = numMaxPages;
        setNewBackground();
    }

    public void setBackgroundAsset(int resId) {
        if (resId == 0) {
            setParallaxEnabled(false);
        }
        else {
            setParallaxEnabled(true);
            backgroundId = resId;
            setNewBackground();
        }

        // set canvas src and dst, then invoke onDraw() to update the background
        if (currentPosition == -1) {
            currentPosition = getCurrentItem();
        }
        setCanvasSrcDst();
        this.invalidate();
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
        currentPosition = item;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.pagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isFakeDragging()) {
            return false;
        }
        return this.pagingEnabled && super.onInterceptTouchEvent(event);
    }

    public boolean isPagingEnabled() {
        return pagingEnabled;
    }

    /**
     * Enables or disables paging for this ViewPagerParallax.
     */
    public void setPagingEnabled(boolean pagingEnabled) {
        this.pagingEnabled = pagingEnabled;
    }

    public boolean isParallaxEnabled() {
        return parallaxEnabled;
    }

    /**
     * Enables or disables parallax effect for this ViewPagerParallax.
     */
    public void setParallaxEnabled(boolean parallaxEnabled) {
        this.parallaxEnabled = parallaxEnabled;
    }
}