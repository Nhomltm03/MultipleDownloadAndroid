package com.esasyassistivetouch.demomvp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.Random;

/**
 * a music visualizer sort of animation (with random data)
 */
public class MusicVisualizer extends View {

    Random random = new Random();

    Paint paint = new Paint();
    private Runnable animateView = new Runnable() {
        @Override
        public void run() {

            //run every 100 ms
            postDelayed(this, 120);

            invalidate();
        }
    };

    public MusicVisualizer(Context context) {
        super(context);
        new MusicVisualizer(context, null);
    }

    public MusicVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        //start runnable
        this.removeCallbacks(this.animateView);
        this.post(this.animateView);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //set paint style, Style.FILL will fill the color, Style.STROKE will stroke the color
        this.paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(this.getDimensionInPixel(0),
                this.getHeight() - (40 + this.random.nextInt((int) (this.getHeight() / 1.5f) - 25)),
                this.getDimensionInPixel(7),
                this.getHeight() - 15,
                this.paint);

        canvas.drawRect(this.getDimensionInPixel(10),
                this.getHeight() - (40 + this.random.nextInt((int) (this.getHeight() / 1.5f) - 25)),
                this.getDimensionInPixel(17),
                this.getHeight() - 15,
                this.paint);

        canvas.drawRect(this.getDimensionInPixel(20),
                this.getHeight() - (40 + this.random.nextInt((int) (this.getHeight() / 1.5f) - 25)),
                this.getDimensionInPixel(27),
                this.getHeight() - 15,
                this.paint);
    }

    public void setColor(int color) {
        this.paint.setColor(color);
        this.invalidate();
    }

    //get all dimensions in dp so that views behaves properly on different screen resolutions
    private int getDimensionInPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            this.removeCallbacks(animateView);
            this.post(animateView);
        } else if (visibility == GONE) {
            this.removeCallbacks(animateView);
        }
    }
}