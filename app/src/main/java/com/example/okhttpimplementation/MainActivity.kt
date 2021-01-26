package com.example.okhttpimplementation

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException


@Suppress(
    "DEPRECATED_IDENTITY_EQUALS", "DEPRECATION",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
open class MainActivity : AppCompatActivity(), View.OnClickListener {

    var url = "https://reqres.in/api/users/2"
    var postUrl = "https://reqres.in/api/users/"
    // json string for posting on server
    var postBody = ("{\n" +
            " \"name\": \"morpheus\",\n" +
            " \"job\": \"leader\"\n" +
            "}")
    val JSON = MediaType.parse("application/json; charset=utf-8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
                return
            }
        }
       
        enableButton()

        buttonDownload.setOnClickListener {
            DownloadImageTask(image_view)
                .execute("https://images.all-free-download.com/images/wallpapers_large/minions_comedy_movie_14603.jpg")
        }
    }

    private fun enableButton() {
        buttonUpload.setOnClickListener {
            MaterialFilePicker()
                .withActivity(this@MainActivity)
                .withRequestCode(10)
                .start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        if (requestCode == 100 && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
        {
            enableButton()
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                // get user permission
                requestPermissions(arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
            }
        }
    }

    lateinit var progress: ProgressDialog

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10 && resultCode == RESULT_OK)
        {
            progress = ProgressDialog(this@MainActivity)
            progress.setTitle("Uploading")
            progress.setMessage("Please wait...")
            progress.show()
            val t = Thread {
                val f = File(data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH))
                val content_type = getMimeType(f.path)
                val file_path = f.absolutePath
                val client = OkHttpClient()
                val file_body = RequestBody.create(MediaType.parse(content_type), f)
                val request_body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("type", content_type)
                    .addFormDataPart(
                        "uploaded_file", file_path.substring(
                            file_path.lastIndexOf(
                                "/"
                            ) + 1
                        ), file_body
                    )
                    .build()
                val request = Request.Builder()
                    .url("http://theappsfirm.com/save_file.php")
                    .post(request_body)
                    .build()
                try {
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw IOException("Error : $response")
                    }
                    progress.dismiss()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            t.start()
        }
    }

    private fun getMimeType(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).toString()
    }

    private class DownloadImageTask(var bmImage: ImageView): AsyncTask<String, Void, Bitmap>() {
        override fun doInBackground(vararg urls: String): Bitmap? {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(urls[0])
                .build()
            var response: Response? = null
            var mIcon11: Bitmap? = null
            try {
                response = client.newCall(request).execute()
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
            if (response?.isSuccessful == true)
            {
                try
                {
                    mIcon11 = BitmapFactory.decodeStream(response.body()?.byteStream())
                }
                catch (e: Exception) {
                    Log.e("Error", e.message.toString())
                    e.printStackTrace()
                }
            }
            return mIcon11
        }
        override fun onPostExecute(result: Bitmap?) {
            bmImage.setImageBitmap(result)
            MediaStore.Images.Media.insertImage(getContentResolver(), result, "Minions", "funny")
        }

        private fun getContentResolver(): ContentResolver? {
            return null
        }
    }

    private fun postRequest(postUrl: String, postBody: String) {
        val client = OkHttpClient()
        val body = RequestBody.create(JSON, postBody)
        val request = Request.Builder()
            .url(postUrl)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                call.cancel()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                response.body()?.string()?.let { Log.d("TAG", it) }
            }
        })
    }

    private fun asynchronousGet() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                call.cancel()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val myResponse = response.body()?.string()
                this@MainActivity.runOnUiThread(object : Runnable {
                    @SuppressLint("SetTextI18n")
                    override fun run() {
                        try {
                            val json = JSONObject(myResponse)
                            txt_string.text = "First Name: " + json.getJSONObject("data")
                                .getString("first_name") + "\nLast Name: " + json.getJSONObject(
                                "data"
                            ).getString("last_name")
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                })
            }
        })
    }

   override fun onClick(view: View) {
        when (view.id) {
            R.id.asynchronousGet -> {
                asynchronousGet()
            }
            R.id.synchronousGet -> {
                val okHttpHandler = SynchronousGetRequest()
                okHttpHandler.execute(url)
            }
            R.id.asynchronousPost -> {
                postRequest(postUrl, postBody)
            }
        }
    }

    class SynchronousGetRequest:AsyncTask<String, Void, String>() {
        private var client = OkHttpClient()
        override fun doInBackground(vararg params: String): String? {
            val builder = Request.Builder()
            builder.url(params[0])
            val request = builder.build()
            try
            {
                val response = client.newCall(request).execute()
                return response.body()?.string()
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
        override fun onPostExecute(s: String?) {
            super.onPostExecute(s)
        }

    }

}
