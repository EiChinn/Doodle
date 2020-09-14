package cn.hzw.doodledemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Display;
import android.view.WindowManager;

public class ImgUtils {
	public static Bitmap createBitmapFromScratch(Context context) {
		WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		int screenW = display.getWidth();
		int screenH = display.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(Color.WHITE);//填充颜色
		return bitmap;
	}
}
