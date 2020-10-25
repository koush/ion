package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.scratch.Promise;
import com.koushikdutta.scratch.Result;

import java.io.File;

/**
 * Created by koush on 5/31/13.
 */
public class ProgressBarDownload extends Activity {
    Button download;
    TextView downloadCount;
    ProgressBar progressBar;

    Promise<Result<File>> downloading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable global Ion logging
        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);

        setContentView(R.layout.progress);

        download = (Button)findViewById(R.id.download);
        downloadCount = (TextView)findViewById(R.id.download_count);
        progressBar = (ProgressBar)findViewById(R.id.progress);

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloading != null && !downloading.isCancelled()) {
                    resetDownload();
                    return;
                }

                download.setText("Cancel");
                // this is a big zip file to test with
                downloading = Ion.with(ProgressBarDownload.this)
                .load("https://github.com/koush/vysor.io/releases/download/v3.1.4/Vysor-mac-3.1.4.zip")
                    // attach the percentage report to a progress bar.
                    // can also attach to a ProgressDialog with progressDialog.
                    .progressBar(progressBar)
                    // callbacks on progress can happen on the UI thread
                    // via progressHandler. This is useful if you need to update a TextView.
                    // Updates to TextViews MUST happen on the UI thread.
                    .progressHandler(new ProgressCallback() {
                        @Override
                        public void onProgress(long downloaded, long total) {
                            downloadCount.setText("" + downloaded + " / " + total);
                        }
                    })
                    // write to a file
                    .write(new File(getFilesDir(), "zip-" + System.currentTimeMillis() + ".zip"))
                    // run a callback on completion
                    .complete(c -> resetDownload())
                    .error(e -> Toast.makeText(ProgressBarDownload.this, "Error downloading file", Toast.LENGTH_LONG).show())
                    .result(result -> Toast.makeText(ProgressBarDownload.this, "File upload complete", Toast.LENGTH_LONG).show());
            }
        });
    }

    void resetDownload() {
        // cancel any pending download
        downloading.cancel();
        downloading = null;

        // reset the ui
        download.setText("Download");
        downloadCount.setText(null);
        progressBar.setProgress(0);
    }
}
