package cn.hzw.doodledemo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;

import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.DoodleRotatableItemBase;
import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;

/**
 * 文字item
 * Created by huangziwei on 2017/3/16.
 */

public class DoodleLegendText extends DoodleRotatableItemBase {

    private Rect mRect = new Rect();
    private final Paint mPaint = new Paint();
    private String mText;
    private Legend legend;
    private boolean textLeftAlign = true;

    public DoodleLegendText(IDoodle doodle, String text, float size, IDoodleColor color, float x, float y) {
        super(doodle, -doodle.getDoodleRotation(), x, y);
        setPen(DoodlePen.TEXT);
        mText = text;
        setSize(size);
        setColor(color);
        setLocation(x, y);
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
        resetBounds(mRect);
        setPivotX(getLocation().x + mRect.width() / 2);
        setPivotY(getLocation().y + mRect.height() / 2);
        resetBoundsScaled(getBounds());

        refresh();
    }

    @Override
    public void resetBounds(Rect rect) {
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        mPaint.setTextSize(getSize());
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.getTextBounds(mText, 0, mText.length(), rect);
        rect.offset(0, rect.height());
    }

    @Override
    public void doDraw(Canvas canvas) {
        getColor().config(this, mPaint);
        mPaint.setTextSize(getSize());
        if (textLeftAlign) {
            mPaint.setTextAlign(Paint.Align.LEFT);
        } else {
            mPaint.setTextAlign(Paint.Align.RIGHT);
        }
        mPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.translate(0, getBounds().height() / getScale());
        canvas.drawText(mText, 0, 0, mPaint);
        canvas.restore();
    }

    public Legend getLegend() {
        return legend;
    }

    public void setLegend(Legend legend) {
        this.legend = legend;
    }

    public void setTextLeftAlign(boolean textLeftAlign) {
        this.textLeftAlign = textLeftAlign;
    }
}


