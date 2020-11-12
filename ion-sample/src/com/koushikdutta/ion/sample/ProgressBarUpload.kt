package com.koushikdutta.ion.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.koushikdutta.ion.Ion
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.drain
import com.koushikdutta.scratch.event.AsyncEventLoop
import com.koushikdutta.scratch.event.startThread
import com.koushikdutta.scratch.http.StatusCode
import com.koushikdutta.scratch.http.server.AsyncHttpRouter
import com.koushikdutta.scratch.http.server.AsyncHttpServer
import com.koushikdutta.scratch.http.server.post
import com.koushikdutta.scratch.siphon
import java.io.File
import java.io.RandomAccessFile

/**
 * Created by koush on 5/31/13.
 */
class ProgressBarUpload : Activity() {
    var upload: Button? = null
    var uploadCount: TextView? = null
    var progressBar: ProgressBar? = null
    var uploading: Promise<File>? = null
    var loop = AsyncEventLoop()
    var router = AsyncHttpRouter()
    var port = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loop.startThread("upload")

        router.post("/upload") {
            it.body!!.siphon()
            StatusCode.OK()
        }

        loop.async {
            val serverSocket = listen()
            port = serverSocket.localPort
            val server = AsyncHttpServer(router::handle)
            server.listen(serverSocket)
        }

        // Enable global Ion logging
        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG)
        setContentView(R.layout.progress_upload)
        upload = findViewById<View>(R.id.upload) as Button
        uploadCount = findViewById<View>(R.id.upload_count) as TextView
        progressBar = findViewById<View>(R.id.progress) as ProgressBar
        upload!!.setOnClickListener(View.OnClickListener {
            if (uploading != null && !uploading!!.isCancelled) {
                resetUpload()
                return@OnClickListener
            }
            val f = getFileStreamPath("largefile")
            try {
                val rf = RandomAccessFile(f, "rw")
                rf.setLength(1024 * 1024 * 2.toLong())
                rf.close()
            }
            catch (e: Exception) {
                System.err.println(e)
            }
            val echoedFile = getFileStreamPath("echo")
            upload!!.text = "Cancel"
            // this is a 180MB zip file to test with
            uploading = Ion.with(this@ProgressBarUpload)
                    .load("http://localhost:$port/upload") // attach the percentage report to a progress bar.
                    // can also attach to a ProgressDialog with progressDialog.
                    .uploadProgressBar(progressBar) // callbacks on progress can happen on the UI thread
                    // via progressHandler. This is useful if you need to update a TextView.
                    // Updates to TextViews MUST happen on the UI thread.
                    .uploadProgressHandler { downloaded, total -> uploadCount!!.text = "$downloaded / $total" } // write to a file
                    .setMultipartFile("largefile", f)
                    .write(echoedFile) // run a callback on completion
                    .error { e: Throwable? -> Toast.makeText(this@ProgressBarUpload, "Error uploading file", Toast.LENGTH_LONG).show() }
                    .result { result: File? -> Toast.makeText(this@ProgressBarUpload, "File upload complete", Toast.LENGTH_LONG).show() }
        })
    }

    fun resetUpload() {
        // cancel any pending upload
        uploading!!.cancel()
        uploading = null

        // reset the ui
        upload!!.text = "Upload"
        uploadCount!!.text = null
        progressBar!!.progress = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        loop.stop()
    }
}