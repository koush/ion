package com.koushikdutta.ion;

import android.graphics.drawable.Drawable;

final class DrawableCache<T extends Drawable> extends WeakReferenceHashTable<String, T> {
}
