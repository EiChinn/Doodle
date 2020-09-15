package cn.hzw.doodle;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleItem;
import cn.hzw.doodle.core.IDoodleShape;

/**
 * 常用图形
 */
public enum DoodleShape implements IDoodleShape {
    HAND_WRITE, // 手绘
    ARROW, // 箭头
    LINE, // 直线
    HOLLOW_RECT, // 空心矩形
    HOLLOW_CIRCLE, // 空心圆
    // POLYGON
    TRIANGLE,
    PENTAGON,
    HEXAGON,
    POLYLINE,
    FILL_CIRCLE, // 实心圆
    FILL_RECT; // 实心矩形


    @Override
    public void config(IDoodleItem doodleItem, Paint paint) {
        if (doodleItem.getShape() == DoodleShape.ARROW || doodleItem.getShape() == DoodleShape.FILL_CIRCLE || doodleItem.getShape() == DoodleShape.FILL_RECT) {
            paint.setStyle(Paint.Style.FILL);
            paint.setPathEffect(null);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            if (doodleItem.getDoodle().getLineType() == IDoodle.LineType.SOLID_LINE) {
                paint.setPathEffect(null);
            } else {
                paint.setPathEffect(new DashPathEffect(new float[] {50, 25}, 0));
            }
        }
    }

    @Override
    public IDoodleShape copy() {
        return this;
    }

    @Override
    public void drawHelpers(Canvas canvas, IDoodle doodle) {

    }
}
