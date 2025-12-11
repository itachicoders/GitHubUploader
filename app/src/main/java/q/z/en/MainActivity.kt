package q.z.en

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val clipData = result.data?.clipData
            val singleUri = result.data?.data

            if (clipData != null) {
                val uris = Array(clipData.itemCount) { i ->
                    clipData.getItemAt(i).uri
                }
                fileUploadCallback?.onReceiveValue(uris)
            } else if (singleUri != null) {
                fileUploadCallback?.onReceiveValue(arrayOf(singleUri))
            } else {
                fileUploadCallback?.onReceiveValue(null)
            }
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { treeUri ->
                processFolderFiles(treeUri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Разрешения необходимы для работы", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        setupBackHandler()
        checkPermissions()
        setupWebView()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.setSupportMultipleWindows(false)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectAndroidInterface()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                filePickerLauncher.launch(Intent.createChooser(intent, "Выберите файлы"))
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebView", "${consoleMessage?.message()} -- ${consoleMessage?.lineNumber()}")
                return true
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun injectAndroidInterface() {
        val js = """
            (function() {
                if (window.AndroidFileHelper) return;
                
                window.AndroidFileHelper = {
                    pickFolder: function() {
                        AndroidBridge.pickFolder();
                    },
                    pickFiles: function() {
                        AndroidBridge.pickFiles();
                    }
                };
                
                console.log('Android interface injected');
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun pickFolder() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                folderPickerLauncher.launch(intent)
            }
        }

        @JavascriptInterface
        fun pickFiles() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                filePickerLauncher.launch(Intent.createChooser(intent, "Выберите файлы"))
            }
        }

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processFolderFiles(treeUri: Uri) {
        Thread {
            try {
                val documentFile = DocumentFile.fromTreeUri(this, treeUri)
                val filesJson = JSONArray()

                if (documentFile != null) {
                    processDirectory(documentFile, "", filesJson)
                }

                runOnUiThread {
                    val js = """
                        (function() {
                            var filesData = $filesJson;
                            if (window.receiveAndroidFiles) {
                                window.receiveAndroidFiles(filesData);
                            }
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(js, null)
                }
            } catch (e: Exception) {
                Log.e("FolderPicker", "Error processing folder", e)
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun processDirectory(directory: DocumentFile, basePath: String, filesJson: JSONArray) {
        val files = directory.listFiles()
        for (file in files) {
            val filePath = if (basePath.isEmpty()) {
                file.name ?: "unknown"
            } else {
                "$basePath/${file.name}"
            }
            
            if (file.isDirectory) {
                processDirectory(file, filePath, filesJson)
            } else {
                try {
                    val fileContent = readFileContent(file.uri)
                    val fileObj = JSONObject()
                    fileObj.put("name", file.name)
                    fileObj.put("path", filePath)
                    fileObj.put("size", file.length())
                    fileObj.put("content", fileContent)
                    fileObj.put("type", file.type ?: "application/octet-stream")
                    filesJson.put(fileObj)
                } catch (e: Exception) {
                    Log.e("FileRead", "Error reading file: ${file.name}", e)
                }
            }
        }
    }

    private fun readFileContent(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val buffer = ByteArrayOutputStream()
        if (inputStream != null) {
            val data = ByteArray(16384)
            var count: Int
            while (inputStream.read(data).also { count = it } != -1) {
                buffer.write(data, 0, count)
            }
            inputStream.close()
        }
        return Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP)
    }
}