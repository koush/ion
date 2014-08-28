package com.koushikdutta.ion.bitmap;

/**
* Created by robUx4 on 28/08/2014.
*/
public class BitmapDecodeException extends Exception {
	public final int width;
	public final int height;

	public BitmapDecodeException(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return super.toString() + " size="+width+'x'+height;
	}
}
