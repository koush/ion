package com.koushikdutta.ion;

/**
 * Callback that is invoked on download progress
 */
public interface ProgressCallback {
    void onProgress(int downloaded, int total);
}
