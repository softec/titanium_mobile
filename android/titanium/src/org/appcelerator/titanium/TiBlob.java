/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiMimeTypeHelper;
import org.appcelerator.titanium.util.TiStreamHelper;
import org.appcelerator.titanium.util.TiUIHelper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

@Kroll.proxy
public class TiBlob extends KrollProxy
{
    public static final String BITMAP_MIMETYPE = "image/bitmap";
    public static final String TEXT_MIMETYPE = "text/plain";

	private static final String LCAT = "TiBlob";
	private static final boolean DBG = TiConfig.LOGD;

	public static final int TYPE_IMAGE = 0;
	public static final int TYPE_FILE = 1;
	public static final int TYPE_DATA = 2;
	public static final int TYPE_STRING = 3;

	private int type;
	private Object data;
	private String mimetype;
	private int width, height;

	private TiBlob(TiContext tiContext, int type, Object data, String mimetype)
	{
		super(tiContext);
		this.type = type;
		this.data = data;
		this.mimetype = mimetype;
		this.width = 0;
		this.height = 0;
	}

	public static TiBlob blobFromString(TiContext tiContext, String data)
	{
		return new TiBlob(tiContext, TYPE_STRING, data, TEXT_MIMETYPE);
	}

	public static TiBlob blobFromFile(TiContext tiContext, TiBaseFile file)
	{
		return blobFromFile(tiContext, file, TiMimeTypeHelper.getMimeType(file.nativePath()));
	}

	public static TiBlob blobFromFile(TiContext tiContext, TiBaseFile file, String mimeType)
	{
		if (mimeType == null) {
			mimeType = TiMimeTypeHelper.getMimeType(file.nativePath());
		}
		return new TiBlob(tiContext, TYPE_FILE, file, mimeType);
	}

    public static TiBlob blobFromImage(TiContext tiContext, Bitmap image) {
        return TiBlob.blobFromImage(tiContext, image, TiBlob.BITMAP_MIMETYPE);
    }

    public static TiBlob blobFromImage(TiContext tiContext, BitmapDrawable image) {
        return TiBlob.blobFromImage(tiContext, image, TiBlob.BITMAP_MIMETYPE);
    }

	public static TiBlob blobFromImage(TiContext tiContext, Bitmap image, String mimetype)
	{
		TiBlob blob = new TiBlob(tiContext, TYPE_IMAGE, image, mimetype);
		blob.width = image.getWidth();
		blob.height = image.getHeight();
		return blob;
	}

    public static TiBlob blobFromImage(TiContext tiContext, BitmapDrawable image, String mimetype)
	{
		TiBlob blob = new TiBlob(tiContext, TYPE_IMAGE, image, mimetype);
		blob.width = image.getIntrinsicWidth();
		blob.height = image.getIntrinsicHeight();
		return blob;
	}

	public static TiBlob blobFromData(TiContext tiContext, byte[] data)
	{
		return blobFromData(tiContext, data, "application/octet-stream");
	}

	public static TiBlob blobFromData(TiContext tiContext, byte[] data, String mimetype)
	{
		if (mimetype == null || mimetype.length() == 0) {
			return new TiBlob(tiContext, TYPE_DATA, data, "application/octet-stream");
		}
		return new TiBlob(tiContext, TYPE_DATA, data, mimetype);
	}

    private byte[] getBitmapBytes() {
        Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte bytes[] = new byte[0];
        if (mimetype != null
                && (mimetype.toLowerCase().contains("jpeg") || mimetype.toLowerCase().contains("jpg"))) {
            format = Bitmap.CompressFormat.JPEG;
        }
        if (getBitmapFromData().compress(format, 100, bos)) {
            bytes = bos.toByteArray();
        }
        return bytes;
    }

	public byte[] getBytes()
	{
		byte[] bytes = new byte[0];

		switch(type) {
			case TYPE_STRING :
				try {
					bytes = ((String) data).getBytes("utf-8");
				} catch (UnsupportedEncodingException e) {
					Log.w(LCAT, e.getMessage(), e);
				}
				break;
			case TYPE_DATA:
                bytes = (byte[]) data;
                break;
			case TYPE_IMAGE:
                bytes = getBitmapBytes();
				break;
			case TYPE_FILE:	
				InputStream stream = getInputStream();
				if (stream != null) {
					try {
						bytes = TiStreamHelper.toByteArray(stream, getLength());
					} finally {
						try {
							stream.close();
						} catch (IOException e) {
							Log.w(LCAT, e.getMessage(), e);
						}
					}
				}
				break;
			default :
				throw new IllegalArgumentException("Unknown Blob type id " + type);
		}

		return bytes;
	}

	@Kroll.getProperty @Kroll.method
	public int getLength()
	{
		switch (type) {
			case TYPE_FILE:
				return (int) ((TiBaseFile)data).size();
			case TYPE_DATA:
                return ((byte[])data).length;
			case TYPE_IMAGE:
			default:
				// this is probably overly expensive.. is there a better way?
				return getBytes().length;
		}
	}

	public InputStream getInputStream()
	{
		switch (type) {
			case TYPE_FILE:
			try {
				return ((TiBaseFile)data).getInputStream();
			} catch (IOException e) {
				Log.e(LCAT, e.getMessage(), e);
				return null;
			}
			default:
				return new ByteArrayInputStream(getBytes());
		}
	}

	@Kroll.method
	public void append(TiBlob blob)
	{
		switch(type) {
			case TYPE_STRING :
				try {
					String dataString = (String)data;
					dataString += new String(blob.getBytes(), "utf-8");
				} catch (UnsupportedEncodingException e) {
					Log.w(LCAT, e.getMessage(), e);
				}
				break;
			case TYPE_IMAGE:
                byte[] dBytes = getBytes();
                byte[] aBytes = blob.getBytes();
                byte[] nData = new byte[dBytes.length + aBytes.length];
                System.arraycopy(dBytes, 0, nData, 0, dBytes.length);
                System.arraycopy(aBytes, 0, nData, dBytes.length, aBytes.length);
                data = nData;
                break;
			case TYPE_DATA :
				byte[] dataBytes = (byte[]) data;
				byte[] appendBytes = blob.getBytes();
				byte[] newData = new byte[dataBytes.length + appendBytes.length];
				System.arraycopy(dataBytes, 0, newData, 0, dataBytes.length);
				System.arraycopy(appendBytes, 0, newData, dataBytes.length, appendBytes.length);

				data = newData;
				break;
			case TYPE_FILE :
				throw new IllegalStateException("Not yet implemented. TYPE_FILE");
				// break;
			default :
				throw new IllegalArgumentException("Unknown Blob type id " + type);
		}
	}

	@Kroll.getProperty @Kroll.method
	public String getText()
	{
		String result = null;

		// Only support String and Data. Same as iPhone
		switch(type) {
			case TYPE_STRING :
				result = (String) data;
			case TYPE_DATA:
			case TYPE_FILE:
				// Don't try to return a string if we can see the 
				// mimetype is binary, unless it's application/octet-stream, which means
				// we don't really know what it is, so assume the user-developer knows
				// what she's doing.
				if (mimetype != null && TiMimeTypeHelper.isBinaryMimeType(mimetype) && mimetype != "application/octet-stream") {
					return null;
				}
				try {
					result = new String(getBytes(), "utf-8");
				} catch (UnsupportedEncodingException e) {
					Log.w(LCAT, "Unable to convert to string.");
				}
				break;
		}

		return result;
	}

	@Kroll.getProperty @Kroll.method
	public String getMimeType()
	{
		return mimetype;
	}

	public Object getData()
	{
		return data;
	}
	
	@Kroll.getProperty @Kroll.method
	public int getType()
	{
		return type;
	}

	@Kroll.getProperty @Kroll.method
	public int getWidth()
	{
		return width;
	}

	@Kroll.getProperty @Kroll.method
	public int getHeight()
	{
		return height;
	}

	public String toString()
	{
		// blob should return the text value on toString 
		// if it's not null
		String text = getText();
		if (text != null) {
			return text;
		}
		return "[object TiBlob]";
	}

	@Kroll.getProperty @Kroll.method
	public String getNativePath()
	{
		if (data == null) {
			return null;
		}
		if (this.type != TYPE_FILE) {
			Log.w(LCAT, "getNativePath not supported for non-file blob types.");
			return null;
		} else if (!(data instanceof TiBaseFile)) {
			Log.w(LCAT, "getNativePath unable to return value: underlying data is not file, rather " + data.getClass().getName());
			return null;
		} else {
			String path = ((TiBaseFile)data).nativePath();
			if (path != null && path.startsWith("content://")) {
				File f = ((TiBaseFile)data).getNativeFile();
				if (f != null) {
					path = f.getAbsolutePath();
					if (path != null && path.startsWith("/")) {
						path = "file://" + path;
					}
				}
			}
			return path;
		}
	}

	@Kroll.getProperty @Kroll.method
	public TiFileProxy getFile()
	{
		if (data == null) {
			return null;
		}
		if (this.type != TYPE_FILE) {
			Log.w(LCAT, "getFile not supported for non-file blob types.");
			return null;
		} else if (!(data instanceof TiBaseFile)) {
			Log.w(LCAT, "getFile unable to return value: underlying data is not file, rather " + data.getClass().getName());
			return null;
		} else {
			return new TiFileProxy(context, (TiBaseFile)data);
		}
	}

	@Kroll.method
	public String toBase64()
	{
		return new String(Base64.encodeBase64(getBytes()));
	}
	
	@Kroll.method
	public TiBlob imageAsResized(int iWidth, int iHeight) {
        Bitmap bmpInput = getBitmapFromData();
        Bitmap bmpScaled = Bitmap.createScaledBitmap(bmpInput, iWidth, iHeight, false);
		return TiBlob.blobFromImage(this.context, bmpScaled, this.getMimeType());
	}
	
	@Kroll.method
	public TiBlob imageAsCropped(@Kroll.argument(optional=false) KrollDict config) {
		if (!config.containsKeyAndNotNull("x")
				|| !config.containsKeyAndNotNull("y")
				|| !config.containsKeyAndNotNull("width")
				|| !config.containsKeyAndNotNull("height")) {
			throw new IllegalStateException("imageAsCropped:Failed, missing some config parameters (x,y,width,height)");					
		}
		
		int dx = config.getInt("x"),
		    dy = config.getInt("y"),
		    dWidth = config.getInt("width"),
		    dHeight = config.getInt("height");

        Bitmap bmpInput = getBitmapFromData();
		Bitmap bmpCropped = Bitmap.createBitmap(bmpInput, dx, dy, dWidth, dHeight);
		return TiBlob.blobFromImage(this.context, bmpCropped, this.getMimeType());
	}

	@Kroll.method
	public TiBlob imageWithRoundedCorner(int cornerSize, @Kroll.argument(optional=true) Integer borderSize) {
		Bitmap bmpInput = getBitmapFromData();
        int bSize = (borderSize == null) ? 0 : borderSize;
		int width = bmpInput.getWidth();
		int height = bmpInput.getHeight();
		
		Bitmap rounder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(rounder);
		Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		xferPaint.setColor(Color.RED);
		
		canvas.drawRoundRect(new RectF(0,0,width, height), cornerSize, cornerSize, xferPaint);
		
		xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		
		
		Bitmap bmpOutput = null;
		if (bSize <= 0) {
			bmpOutput = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			canvas = new Canvas(bmpOutput);
			canvas.drawBitmap(bmpInput, 0, 0, null);
			canvas.drawBitmap(rounder, 0, 0, xferPaint);
		} else {
			bmpOutput = Bitmap.createBitmap(width + (bSize * 2), height + (bSize * 2),
					Bitmap.Config.ARGB_8888);

			canvas = new Canvas(bmpOutput);
			canvas.drawBitmap(bmpInput, bSize, bSize, null);
			canvas.drawBitmap(rounder, bSize, bSize, xferPaint);
		}
		
		return TiBlob.blobFromImage(this.context, bmpOutput, this.getMimeType());
	}
	
	@Kroll.method
	public TiBlob imageWithTransparentBorder(int size) {
		Bitmap bmpInput = getBitmapFromData();

		int width = bmpInput.getWidth();
		int height = bmpInput.getHeight();
		
		Bitmap bmpOutput =  Bitmap.createBitmap(width + (size * 2), height + (size * 2),
				Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bmpOutput);
		canvas.drawBitmap(bmpInput, size, size, null);
		return TiBlob.blobFromImage(this.context, bmpOutput, this.getMimeType());
	}

	@Kroll.method
	public TiBlob imageWithAlpha() {
        Bitmap bmpInput = getBitmapFromData();
		int width = bmpInput.getWidth();
		int height = bmpInput.getHeight();
			
		Bitmap bmpOutput = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		int[] pixels = new int[width * height];
		bmpInput.getPixels(pixels, 0, width, 0, 0, width, height);
		bmpOutput.setPixels(pixels, 0, width, 0, 0, width, height);
		
		return TiBlob.blobFromImage(this.context, bmpOutput, this.getMimeType());
	}

    /**
     * Retrieve an android bitmap based on the data.
     * <p>If the data is a Bitmap, simply return it; otherwise try to load
     * the bitmap based on the bytes contained in the blob</p>
     * @return The loaded bitmap
     * @throws IllegalStateException If the blob doesn't contain a valid bitmap representation
     */
    public Bitmap getBitmapFromData() {
        if (this.data instanceof Bitmap) {
            return (Bitmap) data;
        }
        if (this.data instanceof BitmapDrawable) {
            return (Bitmap) (data = ((BitmapDrawable) data).getBitmap());
        }

        return (Bitmap) (data = TiUIHelper.createBitmap(getInputStream()));
    }

    /**
     * Retrieve an android bitmap based on the data.
     * <p>If the data is a BitmapDrawable, simply return it; otherwise try to load
     * the bitmap based on the bytes contained in the blob</p>
     * @return The loaded BitmapDrawable
     * @throws IllegalStateException If the blob doesn't contain a valid bitmap representation
     */
    public BitmapDrawable getBitmapDrawableFromData() {
        if (this.data instanceof BitmapDrawable) {
            return (BitmapDrawable) data;
        }

        BitmapDrawable bitmap = new BitmapDrawable(getBitmapFromData());
        bitmap.setBounds(0, 0, bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight());
        return (BitmapDrawable) (data = bitmap);
    }
}
