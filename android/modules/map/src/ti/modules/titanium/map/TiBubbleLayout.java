/*
 * Copyright 2011 SOFTEC sa. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ti.modules.titanium.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.RelativeLayout;

/**
 * Created by IntelliJ IDEA. User: DenisG Date: 19/10/11 Time: 18:26 To change this template use File | Settings | File
 * Templates.
 */
public class TiBubbleLayout extends RelativeLayout
{
    private int arrowWidth = 10; // half value !!
    private int arrowHeight = 10;
    private int backgroundColor = Color.argb(220, 15, 15, 15);
    private int borderColor = Color.argb(255, 0, 0, 0);
    private int borderRadius = 6;

    public TiBubbleLayout(Context context) {
        super(context);
        setGravity(Gravity.NO_GRAVITY);
        initialize();
    }

    public TiBubbleLayout(Context context, AttributeSet attrs) {
        super (context, attrs);
        this.backgroundColor = attrs.getAttributeIntValue("android","backgroundColor",this.backgroundColor);
        initialize();
    }

    private void initialize() {
        super.setBackgroundColor(Color.argb(0, 0, 0, 0));
        setPadding(getPaddingLeft(), getPaddingTop(),
            getPaddingRight(), getPaddingBottom());
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom)
    {
        super.setPadding(left, top, right, bottom + arrowHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int minSize = (borderRadius*2) + 20;
        int measuredWidth = Math.max(getMeasuredWidth(),arrowWidth + minSize);
        int measuredHeight =  Math.max(getMeasuredHeight(),minSize);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBorderColor(int borderColor)
    {
        this.borderColor = borderColor;
    }

    public int getBorderColor()
    {
        return borderColor;
    }

    public int getArrowHeight()
    {
        return arrowHeight;
    }

    public void setArrowHeight(int arrowHeight)
    {
        this.arrowHeight = arrowHeight;
    }

    public int getArrowWidth()
    {
        return arrowWidth*2;
    }

    public void setArrowWidth(int arrowWidth)
    {
        this.arrowWidth = arrowWidth/2;
    }

    public int getBorderRadius()
    {
        return borderRadius;
    }

    public void setBorderRadius(int borderRadius)
    {
        this.borderRadius = borderRadius;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        Paint contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setColor(getBackgroundColor());

        Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        effectPaint.setColor(Color.argb(10,255,255,255));

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(getBorderColor());
        borderPaint.setStyle(Paint.Style.STROKE);

        int width = getMeasuredWidth();
        int halfWidth = width / 2;
        int height = getMeasuredHeight();
        int bubbleHeight = height - arrowHeight;
        float borderDiff = borderRadius*1.5f;

        // Draw bubble
        Path p = new Path();
        p.moveTo(0,borderDiff);
        p.lineTo(0,bubbleHeight-borderDiff);
        p.quadTo(0,bubbleHeight,borderDiff,bubbleHeight);
        p.lineTo(halfWidth-arrowWidth,bubbleHeight);
        p.lineTo(halfWidth,height);
        p.lineTo(halfWidth+arrowWidth,bubbleHeight);
        p.lineTo(width-borderDiff,bubbleHeight);
        p.quadTo(width,bubbleHeight,width,bubbleHeight-borderDiff);
        p.lineTo(width,borderDiff);
        p.quadTo(width,0,width-borderDiff,0);
        p.lineTo(borderDiff,0);
        p.quadTo(0,0,0,borderDiff);
        canvas.drawPath(p, contentPaint);
        canvas.drawPath(p, borderPaint);

        canvas.drawRoundRect(new RectF(1, 1, width-2, bubbleHeight/2 + 1), getBorderRadius(), getBorderRadius(), effectPaint);

        super.dispatchDraw(canvas);
    }
}
