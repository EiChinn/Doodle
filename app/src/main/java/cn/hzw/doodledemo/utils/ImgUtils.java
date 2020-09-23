package cn.hzw.doodledemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.view.Display;
import android.view.WindowManager;

import static cn.forward.androids.utils.ImageUtils.computeBitmapSimple;
import static cn.forward.androids.utils.ImageUtils.rotateBitmapByExif;

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

	public static final Bitmap createBitmapFromPath(String path, int maxWidth, int maxHeight, Context context) {
		Bitmap bitmap = null;
		BitmapFactory.Options options = null;
		if (path.endsWith(".3gp")) {
			return ThumbnailUtils.createVideoThumbnail(path, 1);
		} else {
			try {
				options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				if (path.startsWith("assets/")) {
					BitmapFactory.decodeStream(context.getAssets().open(path.split("/")[1]), null, options);
				} else {
					BitmapFactory.decodeFile(path, options);
				}

				int width = options.outWidth;
				int height = options.outHeight;
				options.inSampleSize = computeBitmapSimple(width * height, maxWidth * maxHeight * 2);
				options.inPurgeable = true;
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				options.inDither = false;
				options.inJustDecodeBounds = false;
				if (path.startsWith("assets/")) {
					bitmap = BitmapFactory.decodeStream(context.getAssets().open(path.split("/")[1]), null, options);
				} else {
					bitmap = BitmapFactory.decodeFile(path, options);
				}
				return rotateBitmapByExif(bitmap, path, true);
			} catch (OutOfMemoryError var7) {
				options.inSampleSize *= 2;
				bitmap = BitmapFactory.decodeFile(path, options);
				return rotateBitmapByExif(bitmap, path, true);
			} catch (Exception var8) {
				var8.printStackTrace();
				return null;
			}
		}
	}
}
