package com.koushikdutta.ion;

/**
 * Callback that is invoked on download progress
 */
public interface ProgressCallback {
    /**
     * onProgress is invoked periodically during a request download
     * @param downloaded The number of bytes currently downloaded
     * @param total The total number of bytes in this request, or -1 if unknown
     */
    void onProgress(long downloaded, long total);
}
