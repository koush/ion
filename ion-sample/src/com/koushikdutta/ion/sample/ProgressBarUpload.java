package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by koush on 5/31/13.
 */
public class ProgressBarUpload extends Activity {
    Button upload;
    TextView uploadCount;
    ProgressBar progressBar;

    Future<File> uploading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable global Ion logging
        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);

        setContentView(R.layout.progress_upload);

        upload = (Button)findViewById(R.id.upload);
        uploadCount = (TextView)findViewById(R.id.upload_count);
        progressBar = (ProgressBar)findViewById(R.id.progress);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (uploading != null && !uploading.isCancelled()) {
                    resetUpload();
                    return;
                }

                File f = getFileStreamPath("largefile");
                try {
                    RandomAccessFile rf = new RandomAccessFile(f, "rw");
                    rf.setLength(1024 * 1024 * 2);
                } catch (Exception e) {
                    System.err.println(e);
                }
                File echoedFile = getFileStreamPath("echo");

                upload.setText("Cancel");
                // this is a 180MB zip file to test with
                uploading = Ion.with(ProgressBarUpload.this)
                .load("http://koush.clockworkmod.com/test/echo")
                // attach the percentage report to a progress bar.
                // can also attach to a ProgressDialog with progressDialog.
                .uploadProgressBar(progressBar)
                // callbacks on progress can happen on the UI thread
                // via progressHandler. This is useful if you need to update a TextView.
                // Updates to TextViews MUST happen on the UI thread.
                .uploadProgressHandler(new ProgressCallback() {
                    @Override
                    public void onProgress(long downloaded, long total) {
                        uploadCount.setText("" + downloaded + " / " + total);
                    }
                })
                // write to a file
                .setMultipartFile("largefile", f)
                .write(echoedFile)
                // run a callback on completion
                .setCallback(new FutureCallback<File>() {
                    @Override
                    public void onCompleted(Exception e, File result) {
                        resetUpload();
                        if (e != null) {
                            Toast.makeText(ProgressBarUpload.this, "Error uploading file", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(ProgressBarUpload.this, "File upload complete", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    void resetUpload() {
        // cancel any pending upload
        uploading.cancel();
        uploading = null;

        // reset the ui
        upload.setText("Upload");
        uploadCount.setText(null);
        progressBar.setProgress(0);
    }
}
