package com.penn.jba.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by raighne on 5/12/17.
 */

public class SquareIV extends android.support.v7.widget.AppCompatImageView {
    private float radiusX;
    private float radiusY;

    public SquareIV(Context context) {
        super(context);
    }

    public SquareIV(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareIV(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //传入参数widthMeasureSpec、heightMeasureSpec
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}