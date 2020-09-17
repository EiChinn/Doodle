package cn.hzw.doodledemo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.DoodleRotatableItemBase;
import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;

/**
 * 图例，显示在右下角
 * Created by huangziwei on 2017/3/16.
 */

public class DoodleLegendText extends DoodleRotatableItemBase {

    private final TextPaint mPaint = new TextPaint();
    private String mText;
    private StaticLayout staticLayout;
    private int maxWidth;

    public DoodleLegendText(IDoodle doodle, String text, float size, IDoodleColor color,
                            float x, float y, int maxWidth) {
        super(doodle, -doodle.getDoodleRotation(), x, y);
        this.maxWidth = maxWidth;
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
        resetBounds(getBounds());
        setPivotX(getLocation().x + getBounds().width() / 2.0f);
        setPivotY(getLocation().y + getBounds().height() / 2.0f);
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
        staticLayout = new StaticLayout(mText, mPaint, maxWidth,
                Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
        rect.top = 0;
        rect.left = 0;
        rect.right = staticLayout.getWidth();
        rect.bottom = staticLayout.getHeight();
    }

    @Override
    public void doDraw(Canvas canvas) {
        getColor().config(this, mPaint);
        mPaint.setTextSize(getSize());
        mPaint.setStyle(Paint.Style.FILL);
        // StaticLayout的绘制原点就在左上角，所以这里不用做translate
        staticLayout.draw(canvas);
    }
}


