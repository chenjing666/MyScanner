/**
 * APICloud Modules
 * Copyright (c) 2014-2015 by APICloud, Inc. All Rights Reserved.
 * Licensed under the terms of the The MIT License (MIT).
 * Please see the license.html included with this distribution for details.
 */
package com.jiashizhan.myscanner.utlis;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.jiashizhan.myscanner.Zxing.decoding.RGBLuminanceSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Hashtable;

public class ScanUtil {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;
	public static String ALBUM_IMG_PATH;

	public static String scanResult2img(String content, String savePath, int w,
                                        int h, boolean isSaveToAlbum, boolean isBar, Context context) {
		ALBUM_IMG_PATH = null;
		FileOutputStream fileOutputStream = null;
		Bitmap bitmap = null;
		if (!isBar) {
			bitmap = scanResult2bitmap(content, BarcodeFormat.QR_CODE, w, h);
		} else {
			bitmap = creatBarcode(context, content, w, h, true);
		}
		if (null != bitmap) {
			if (isSaveToAlbum) {
				File file = makeSaveFile(albumPath());
//				storeToPath(fileOutputStream, bitmap, albumPath(), file);
				showInCamera(context, bitmap, file);
			}
			if (!TextUtils.isEmpty(savePath)) {
				storeToPath(fileOutputStream, bitmap, savePath,
						makeSaveFile(savePath));
				File file = new File(savePath);
				return file.getAbsolutePath();
			}
		}
		return null;
	}

	public static void showInCamera(Context context, Bitmap bitmap, File file) {
		MediaStore.Images.Media.insertImage(context.getContentResolver(),
				bitmap, file.getName(), "");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Intent mediaScanIntent = new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			Uri contentUri = Uri.fromFile(file);
			mediaScanIntent.setData(contentUri);
			context.sendBroadcast(mediaScanIntent);
		} else {
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
					.parse("file://"
							+ Environment.getExternalStorageDirectory())));
		}
	}

	private static void storeToPath(FileOutputStream fileOutputStream,
                                    Bitmap bitmap, String savePath, File saveFile) {
		try {
			fileOutputStream = new FileOutputStream(saveFile);
			storeBitmap(bitmap, fileOutputStream, savePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			closeFos(fileOutputStream);
		}
	}

	private static void closeFos(FileOutputStream fileOutputStream) {
		if (fileOutputStream != null) {
			try {
				fileOutputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void storeBitmap(Bitmap bitmap, FileOutputStream fos,
                                    String savePath) {
		if (savePath.endsWith(".png")) {
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		} else if (savePath.endsWith(".jpg")) {
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
		}
	}

	public static Bitmap scanResult2bitmap(String contents,
                                           BarcodeFormat format, int desiredWidth, int desiredHeight) {
		BitMatrix result = createBitMatrix(contents, format, desiredWidth,
				desiredHeight);
		int width = result.getWidth();
		int height = result.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(createPixels(result), 0, width, 0, 0, width, height);
		return bitmap;
	}

	public static Bitmap creatBarcode(Context context, String contents,
                                      int desiredWidth, int desiredHeight, boolean displayCode) {
		Bitmap ruseltBitmap = null;
		int marginW = 20;
		BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;
		if (displayCode) {
			Bitmap barcodeBitmap = encodeAsBitmap(contents, barcodeFormat,
					desiredWidth, desiredHeight / 2);
			Bitmap codeBitmap = creatCodeBitmap(contents, desiredWidth - 2
					* marginW, desiredHeight / 8, context);
			ruseltBitmap = mixtureBitmap(barcodeBitmap, codeBitmap,
					desiredWidth, desiredHeight);
		} else {
			ruseltBitmap = encodeAsBitmap(contents, barcodeFormat,
					desiredWidth, desiredHeight);
		}

		return ruseltBitmap;
	}

	protected static Bitmap mixtureBitmap(Bitmap first, Bitmap second,
                                          int desiredWidth, int desiredHeight) {
		if (first == null || second == null) {
			return null;
		}
		Bitmap newBitmap = Bitmap.createBitmap(desiredWidth, desiredHeight,
				Config.ARGB_4444);
		Canvas cv = new Canvas(newBitmap);
		cv.drawColor(Color.WHITE);
		cv.drawBitmap(first, 0, desiredHeight / 4, null);
		// cv.drawBitmap(second, (desiredWidth - second.getWidth()) / 2,
		// desiredHeight / 4 * 3, null);
		cv.save(Canvas.ALL_SAVE_FLAG);
		cv.restore();

		return newBitmap;
	}

	protected static Bitmap creatCodeBitmap(String contents, int width,
                                            int height, Context context) {
		TextView tv = new TextView(context);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		tv.setLayoutParams(layoutParams);
		tv.setText(contents);
		tv.setTextSize(height / 5);
		tv.setGravity(Gravity.CENTER_HORIZONTAL);
		tv.setDrawingCacheEnabled(true);
		tv.setTextColor(Color.BLACK);
		tv.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());
		tv.buildDrawingCache();
		Bitmap bitmapCode = tv.getDrawingCache();
		return bitmapCode;
	}

	protected static Bitmap encodeAsBitmap(String contents,
                                           BarcodeFormat format, int desiredWidth, int desiredHeight) {
		final int WHITE = 0xFFFFFFFF;
		final int BLACK = 0xFF000000;
		MultiFormatWriter writer = new MultiFormatWriter();
		BitMatrix result = null;
		try {
			result = writer.encode(contents, format, desiredWidth,
					desiredHeight, null);
		} catch (WriterException e) {
			e.printStackTrace();
		}
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	private static BitMatrix createBitMatrix(String contents,
                                             BarcodeFormat format, int desiredWidth, int desiredHeight) {
		MultiFormatWriter writer = new MultiFormatWriter();
		try {
			return writer.encode(contents, format, desiredWidth, desiredHeight,
					createHints(contents));
		} catch (WriterException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static HashMap<EncodeHintType, String> createHints(String contents) {
		HashMap<EncodeHintType, String> hints = null;
		String encoding = guessAppropriateEncoding(contents);
		if (encoding != null) {
			hints = new HashMap<EncodeHintType, String>(2);
			hints.put(EncodeHintType.CHARACTER_SET, encoding);
		}
		return hints;
	}

	private static int[] createPixels(BitMatrix result) {
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}
		return pixels;
	}

	private static String guessAppropriateEncoding(CharSequence contents) {
		for (int i = 0; i < contents.length(); i++) {
			if (contents.charAt(i) > 0xFF) {
				return "UTF-8";
			}
		}
		return null;
	}

	private static String albumPath() {
		String path = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
				+ "/Camera/" + System.currentTimeMillis() + ".jpg";
		ALBUM_IMG_PATH = path;
		return new File(path).getAbsolutePath();
	}

	private static File makeSaveFile(String filePath) {
		Log.e("Scan", filePath);
		File mediaStorageDir;
		mediaStorageDir = new File(getImgPath(filePath));
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}
		File mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ getImgName(filePath));
		return mediaFile;
	}

	private static String getImgPath(String savePath) {
		int index = savePath.lastIndexOf('/');
		return savePath.substring(0, index);
	}

	private static String getImgName(String savePath) {
		int index = savePath.lastIndexOf('/');
		return savePath.substring(index + 1);
	}

	public static Result parseImage(String path) {
		if (TextUtils.isEmpty(path)) {
			return null;
		}
		Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
		hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
		int sampleSize = (int) (options.outHeight / (float) 200);
		if (sampleSize <= 0)
			sampleSize = 1;
		options.inSampleSize = sampleSize;
		Bitmap scanBitmap = BitmapFactory.decodeFile(path, options);
		RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
		BinaryBitmap binaryBitmap = new BinaryBitmap(
				new HybridBinarizer(source));
		QRCodeReader reader = new QRCodeReader();
		try {
			return reader.decode(binaryBitmap, hints);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (ChecksumException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * //TODO: TAOTAO 将bitmap由RGB转换为YUV
	 * 
	 * @param bitmap
	 *            转换的图形
	 * @return YUV数据
	 */
	public static byte[] rgb2YUV(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
		int len = width * height;
		byte[] yuv = new byte[len * 3 / 2];
		int y, u, v;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int rgb = pixels[i * width + j] & 0x00FFFFFF;
				int r = rgb & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = (rgb >> 16) & 0xFF;
				y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
				u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
				v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
				y = y < 16 ? 16 : (y > 255 ? 255 : y);
				u = u < 0 ? 0 : (u > 255 ? 255 : u);
				v = v < 0 ? 0 : (v > 255 ? 255 : v);
				yuv[i * width + j] = (byte) y;
			}
		}
		return yuv;
	}
}
