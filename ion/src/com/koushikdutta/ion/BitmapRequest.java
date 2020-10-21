package com.koushikdutta.ion;

import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.PostProcess;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.scratch.buffers.ByteBufferList;

import java.util.ArrayList;
import java.util.List;

class BitmapRequest {
    String decodeKey;
    String bitmapKey;
    ArrayList<Transform> transforms;
    int sampleWidth;
    int sampleHeight;
    boolean animateGif;
    boolean deepZoom;
    ArrayList<PostProcess> postProcess;
    IonExecutor<ByteBufferList> executor;

    public String computeKeys() {
        String uri = executor.getUri();
        if (uri == null)
            uri = executor.getRawRequest().getUri().toString();
        decodeKey = computeDecodeKey(uri, sampleWidth, sampleHeight, animateGif, deepZoom);
        bitmapKey = computeBitmapKey(decodeKey, transforms);
        return decodeKey;
    }

    public static String computeDecodeKey(IonRequestBuilder builder, int resizeWidth, int resizeHeight, boolean animateGif, boolean deepZoom) {
        String uri = builder.uri;
        if (uri == null)
            uri = builder.rawRequest.getUri().toString();
        return computeDecodeKey(uri, resizeWidth, resizeHeight, animateGif, deepZoom);
    }

    public static String computeDecodeKey(String uri, int resizeWidth, int resizeHeight, boolean animateGif, boolean deepZoom) {
        // the decode key is a hash of the uri of the image, and any decode
        // specific flags. this includes:
        // inSampleSize (determined from resizeWidth/resizeHeight)
        // gif animation mode
        // deep zoom
        String decodeKey = uri;
        decodeKey += "resize=" + resizeWidth + "," + resizeHeight;
        if (!animateGif)
            decodeKey += ":noAnimate";
        if (deepZoom)
            decodeKey += ":deepZoom";
        return decodeKey;
    }

    public static String computeBitmapKey(String decodeKey, List<Transform> transforms) {
        // determine the key for this bitmap after all transformations
        String bitmapKey = decodeKey;
        if (transforms == null || transforms.isEmpty())
            return bitmapKey;

        for (Transform transform : transforms) {
            bitmapKey = appendTransformKey(bitmapKey, transform);
        }
        return bitmapKey;
    }

    public static String appendTransformKey(String bitmapKey, Transform transform) {
        return bitmapKey + ":"+ transform.key();
    }
}
