package com.chaquo.myapplication

//import com.google.android.gms.common.util.IOUtils.copyStream

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.SimpleArrayMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.chaquo.myapplication.databinding.ActivityConnectionBinding
import com.chaquo.myapplication.databinding.ActivityPhotoConnectionBinding
import com.chaquo.myapplication.db.AppDatabase
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.io.*
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale


class PhotoConnection : AppCompatActivity() {
    private val TAG = "Connection"
    private val SERVICE_ID = "Nearby"
    private val STRATEGY: Strategy = Strategy.P2P_CLUSTER
    private val context: Context = this

    private var isAdvertising: Boolean = false
    private var isDiscovering: Boolean = false
    private var eid : String = ""

    private lateinit var viewBinding: ActivityConnectionBinding

    private val READ_REQUEST_CODE = 42
    private val ENDPOINT_ID_EXTRA = "com.foo.myapp.EndpointId"

    private val links = mutableListOf<List<String>>() // endpointid, usernumber
    private val connectedEndpoints = mutableListOf<String>()



    companion object {
        private const val LOCATION_PERMISSION_CODE = 100
        private const val READ_PERMISSION_CODE = 101
        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
        private const val REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "Image Connection"
        setContentView(R.layout.activity_photo_connection)

        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE)
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION)
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION)

        val viewBinding = ActivityPhotoConnectionBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.offButton2.setOnClickListener { modeOff() }
        viewBinding.onButton2.setOnClickListener { modeOn() }
        viewBinding.makeConnectionButton.setOnClickListener{ showEndpointDialog()}


        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val requestCode = 1

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission has already been granted
        } else {
            // Permission has not been granted yet, request it at runtime
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted, do something
                    Log.d("perm", "got write permission")
                } else {
                    // Permission has been denied, show a message or disable functionality
                    Log.d("perm", "filed to get write permission")
                }
                return
            }
        }
    }


    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            Log.d("perm", "denied $permission")
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
        else {
            Log.d(TAG, "Permissions granted")
        }
    }

    private fun getLocalUserName(): String {
        val db : AppDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()

        val user = db.userDao().findActive()

        if (user != null) {
            return user.username.toString()
        }

        return ""
    }

    private fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        //val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)

        val endpointName = getLocalUserName()
        Nearby.getConnectionsClient(context)
            .startAdvertising(
                endpointName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { unused: Void? ->
                //connectionReport.text = "Advertising as " + getLocalUserName()
                this.isAdvertising = true
            }
            .addOnFailureListener { e: Exception? -> }
    }

    private fun startDiscovery() {
        val discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)

        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { unused: Void? ->
                //connectionReport.text = "Discovering"
                this.isDiscovering = true
            }
            .addOnFailureListener { e: java.lang.Exception? -> }
    }

    private fun stopAdvertising() {
        Nearby.getConnectionsClient(context).stopAdvertising()
        this.isAdvertising = false
    }

    private fun stopDiscovery() {
        Nearby.getConnectionsClient(context).stopDiscovery()
        this.isDiscovering = false
    }

    private fun modeOff()
    {
        if(isAdvertising)
            stopAdvertising()
        if (isDiscovering)
            stopDiscovery()

        val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)
        connectionReport.text = "Stopped searching for endpoints"
        links.clear()

        for (endpointId in connectedEndpoints) {
            // Check if the endpoint is still connected
            Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId)
            // Clear the list of connected endpoints
            connectedEndpoints.clear()
        }

    }

    private fun modeOn()
    {
        if(!isAdvertising)
            startAdvertising()
        if (!isDiscovering)
            startDiscovery()

    }

    /**
     * Fires an intent to spin up the file chooser UI and select an image for sending to endpointId.
     */
    private fun showImageChooser(endpointId: String) {
        this.eid = endpointId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId)
        startActivityForResult(intent, READ_REQUEST_CODE)
        Log.d(TAG, "end img")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK && resultData != null) {
//            val endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA)
            val endpointId = this.eid
            Log.d("EID", endpointId.toString())

            // The URI of the file selected by the user.
            val uri = resultData.data


            val filePayload: Payload
            filePayload = try {
                // Open the ParcelFileDescriptor for this URI with read access.
                val pfd = contentResolver.openFileDescriptor(uri!!, "r")
                Payload.fromFile(pfd!!)
            } catch (e: FileNotFoundException) {
                Log.e("MyApp", "File not found", e)
                return
            }



            /*
            val filePayload = try{
                    createCompressedPayload(this, uri)
                }
            catch (e: FileNotFoundException) {
                Log.e("MyApp", "File not found", e)
                return
            }

             */


            // Construct a simple message mapping the ID of the file payload to the desired filename.
            val filenameMessage = filePayload.id.toString() + ":" + uri.lastPathSegment

            Log.d("FILENAME", filenameMessage)

            // Send the filename message as a bytes payload.
            val filenameBytesPayload =
                Payload.fromBytes(filenameMessage.toByteArray(StandardCharsets.UTF_8))
            Nearby.getConnectionsClient(context).sendPayload(endpointId!!, filenameBytesPayload)

            // Finally, send the file payload.



            if(endpointId != null) {
                Log.d(TAG, "in result")

                Nearby.getConnectionsClient(context).sendPayload(endpointId, filePayload).addOnSuccessListener {
                    Log.d(TAG, "successful send?")
                }
            }
        }
    }
    private fun sendPayLoad(endPointId: String, filePayload: Payload) {
        Log.d(TAG, context.filesDir.toString())
        try {
            Log.d(TAG, "sending file?")
            Nearby.getConnectionsClient(context).sendPayload(endPointId, filePayload)
        } catch (e: FileNotFoundException) {
            Log.e("MyApp", "File not found", e)
        }

    }

    internal class ReceiveFilePayloadCallback(private val context: Context) :
        PayloadCallback() {
        private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
        private val completedFilePayloads = SimpleArrayMap<Long, Payload?>()
        private val filePayloadFilenames = SimpleArrayMap<Long, String>()
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d("saveimage", "in pay rec")
            if (payload.type == Payload.Type.BYTES) {
                val payloadFilenameMessage = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                val payloadId = addPayloadFilename(payloadFilenameMessage)
                processFilePayload(payloadId)
            } else if (payload.type == Payload.Type.FILE) {
//                Log.d("CON", "IN PAY REC")
//                // Add this to our tracking map, so that we can retrieve the payload later.
//                incomingFilePayloads.put(payload.id, payload)

                // Save the photo file
            }
        }


        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is payloadId:filename.
         */
        private fun addPayloadFilename(payloadFilenameMessage: String): Long {
            val parts = payloadFilenameMessage.split(":").toTypedArray()
            val payloadId = parts[0].toLong()
            val filename = parts[1]
            Log.d("NAME", filename)

            filePayloadFilenames.put(payloadId, filename)
            return payloadId
        }

        // add removed tag back to fix b/183037922
        private fun processFilePayload(payloadId: Long) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            val filePayload = completedFilePayloads[payloadId]
            val filename = filePayloadFilenames[payloadId]
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId)
                filePayloadFilenames.remove(payloadId)

                // Get the received file (which will be in the Downloads folder)
                if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                    // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                    // allowed to access filepaths from another process directly. Instead, we must open the
                    // uri using our ContentResolver.
                    val uri = filePayload.asFile()!!.asUri()
                    createImageDirectory()
                    saveToPhotos2(uri)

                } else {
                    val payloadFile = filePayload.asFile()!!.asJavaFile()

                    // Rename the file.
                    payloadFile!!.renameTo(File(payloadFile.parentFile, filename))
                }
            }
        }

        private fun createImageDirectory() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val minerFinderDir = File(picturesDir, "Received-Images-MinerFinder")

                if (!minerFinderDir.exists()) {
                    if (minerFinderDir.mkdirs()) {
                        Log.d("CreateDirectory", "Directory created: ${minerFinderDir.absolutePath}")
                    } else {
                        Log.e("CreateDirectory", "Failed to create directory: ${minerFinderDir.absolutePath}")
                    }
                }
            }
        }


        private fun saveToPhotos(uri: Uri?) {
            val imageTitle = "My Image Title"
            val imageDescription = "My Image Description"

            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())


            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.TITLE, imageTitle)
                put(MediaStore.Images.Media.DESCRIPTION, imageDescription)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Log.d("image1", MediaStore.Images.Media.RELATIVE_PATH)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Received-Images-MinerFinder")
                }
            }

            Log.d("image2", MediaStore.Images.Media.RELATIVE_PATH)


            val contentResolver = context.contentResolver
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (imageUri != null) {
                val outputStream = contentResolver.openOutputStream(imageUri)
                val inputStream = uri?.let { context.contentResolver.openInputStream(it) }
                if (inputStream != null && outputStream != null) {
                    try {
                        inputStream.copyTo(outputStream)
                    } catch (e: Exception) {
                        Log.e("SaveToPhotos", "Error copying image: ${e.message}")
                    } finally {
                        inputStream.close()
                        outputStream.close()
                    }
                }

                // Use the savedUri to open the image in the gallery app

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(imageUri, "image/*")
                    getFilePathFromUri(imageUri)?.let { Log.e("SaveToPhotos", it) }
                }
                context.startActivity(intent)
            } else {
                Log.e("SaveToPhotos", "Failed to save image to MediaStore.")


            }

        }

        fun getFilePathFromUri(uri: Uri): String? {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (columnIndex != -1) {
                        return it.getString(columnIndex)
                    }
                }
            }
            return null
        }




        private fun saveToPhotos2(uri: Uri?) {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val destFile = File(picturesDir, "testfile.jpg")

            val inputStream = uri?.let { context.contentResolver.openInputStream(it) }
            val outputStream = FileOutputStream(destFile)
            if (inputStream != null) {
                try {
                    inputStream.copyTo(outputStream)
                    outputStream.flush()

                    // Add the image to the MediaStore
                    val savedImage = BitmapFactory.decodeFile(destFile.absolutePath)
                    val imageTitle = "My Image Title"
                    val imageDescription = "My Image Description"


                    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                        .format(System.currentTimeMillis())
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.TITLE, imageTitle)
                        put(MediaStore.Images.Media.DESCRIPTION, imageDescription)
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MinerFinder-Image")
                            Log.e("folder-input", "I came here")
                        }
                    }

                    val contentResolver = context.contentResolver
                    val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)


                    /*val savedUriString = MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        savedImage,
                        imageTitle,
                        imageDescription
                    )

                    //val savedUri = Uri.parse(savedUriString)

                     */

                    // Use the savedUri to open the image in the gallery app
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(imageUri, "image/*")
                    }
                    context.startActivity(intent)

                } finally {
                    inputStream.close()
                    outputStream.close()
                }
            }
        }


override fun onPayloadTransferUpdate (endpointId: String, update: PayloadTransferUpdate) {
    if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
        val payloadId = update.payloadId
        val payload = incomingFilePayloads.remove(payloadId)
        completedFilePayloads.put(payloadId, payload)
        if (payload!!.type == Payload.Type.FILE) {
            processFilePayload(payloadId)
        }
    }
}

companion object {
    /** Copies a stream from one location to another.  */
    @Throws(IOException::class)
    private fun copyStream(`in`: InputStream?, out: OutputStream) {
        try {
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`!!.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            out.flush()
        } finally {
            `in`!!.close()
            out.close()
        }
    }
}
}

private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
object : EndpointDiscoveryCallback() {
    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
        // An endpoint was found. We request a connection to it.
        //Nearby.getConnectionsClient(context)
            //.requestConnection(getLocalUserName(), endpointId, connectionLifecycleCallback)
            //.addOnSuccessListener(
            //    OnSuccessListener { unused: Void? -> })
            //.addOnFailureListener(
            //    OnFailureListener { e: java.lang.Exception? -> })

        val discoveredEndpointName = info.endpointName
        val discoveredEndpointID = endpointId
        links.add(listOf(discoveredEndpointID, discoveredEndpointName))
        // display the potential endpoints
        val linksDisplay: TextView = findViewById<TextView>(R.id.connection_report)
        val linksNumbers = links.map { it[1] }
        linksDisplay.text = "Potential Endpoints: $linksNumbers"

    }

    override fun onEndpointLost(endpointId: String) {

        // A previously discovered endpoint has gone away.
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            links.removeIf { it[0] == endpointId }
        }

        // Update the display of potential endpoints
        val linksDisplay: TextView = findViewById<TextView>(R.id.connection_report)
        val linksNumbers = links.map { it[1] }
        linksDisplay.text = "An endpoint was lost! Potential Endpoints: $linksNumbers"

    }
}

private val connectionLifecycleCallback: ConnectionLifecycleCallback =
object : ConnectionLifecycleCallback() {

    override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
        // Automatically accept the connection on both sides.
        Log.d("CONINFO", connectionInfo.toString())
        Log.d("CONINFO", endpointId.toString())
        Log.d("CONINFO", context.toString())

        Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
    }

    override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)

        when (result.status.statusCode) {
            ConnectionsStatusCodes.STATUS_OK -> {
                connectionReport.text = "Connected"
                if (isAdvertising) {
//                            sendPayLoad(endpointId)
                    //stopAdvertising()
                    //showImageChooser(endpointId)
                }
                else {
                    //stopDiscovery()
                }
            }
            ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
            ConnectionsStatusCodes.STATUS_ERROR -> {}
            else -> {}
        }
    }

    override fun onDisconnected(endpointId: String) {
        // We've been disconnected from this endpoint. No more data can be
        // sent or received.
    }
}

private val payloadCallback: PayloadCallback = object : PayloadCallback() {
//        private val context: Context? = null
private var incomingFilePayloads = SimpleArrayMap<Long, Payload>()
private var completedFilePayloads = SimpleArrayMap<Long, Payload>()
private var filePayloadFilenames = SimpleArrayMap<Long, String>()
override fun onPayloadReceived(endpointId: String, payload: Payload) {
    // A new payload is being sent over.
    Log.d(TAG, "Payload Received")
    when (payload.type) {
        Payload.Type.BYTES -> {
            val payloadFilenameMessage = String(payload.asBytes()!!, StandardCharsets.UTF_8)
            val payloadId: Long = addPayloadFilename(payloadFilenameMessage)
            processFilePayload(payloadId)
            Log.d(TAG, payload.asBytes().toString())
            val dataDisplay: TextView = findViewById<TextView>(R.id.data_received)
            dataDisplay.text = payloadFilenameMessage
        }
        Payload.Type.FILE -> {
            Log.d(TAG, "receiving file?")
            // Add this to our tracking map, so that we can retrieve the payload later.
            incomingFilePayloads.put(payload.id, payload);

            val fileUri = payload.asFile()!!.asUri()
            Log.d("saveimage", fileUri.toString())

        }
    }
}

/**
 * Extracts the payloadId and filename from the message and stores it in the
 * filePayloadFilenames map. The format is payloadId:filename.
 */
private fun addPayloadFilename(payloadFilenameMessage: String): Long {
    Log.d("PATH", "IN ADDPAYFIL")
    val parts = payloadFilenameMessage.split(":").toTypedArray()
    val payloadId = parts[0].toLong()
    val filename = parts[1]
    filePayloadFilenames.put(payloadId, filename)
    return payloadId
}

private fun copyStream(`in`: InputStream?, out: OutputStream) {
    try {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`!!.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        out.flush()
    } finally {
        `in`!!.close()
        out.close()
    }
}

private fun processFilePayload(payloadId: Long) {
    Log.d("PATH", "IN PROCFILE")
    // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
    // payload is completely received. The file payload is considered complete only when both have
    // been received.
    val filePayload = completedFilePayloads[payloadId]
    val filename: String? = filePayloadFilenames.get(payloadId)
    if(filename != null)
        Log.d("PFP", filename)
    if (filePayload != null && filename != null) {
        completedFilePayloads.remove(payloadId)
        filePayloadFilenames.remove(payloadId)

        Log.d("DOWN", "ABOVE REMOVE DOWN")

        // Get the received file (which will be in the Downloads folder)
        // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
        // allowed to access filepaths from another process directly. Instead, we must open the
        // uri using our ContentResolver.
        val uri: Uri? = filePayload.asFile()!!.asUri()

        lateinit var imageView: ImageView
        imageView = findViewById(R.id.imageView)
        imageView.setImageURI(uri)

        saveToPhotos(uri)

    }
}

    private fun saveToPhotos(uri: Uri?) {
        val imageTitle = "My Image Title"
        val imageDescription = "My Image Description"

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())


        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.TITLE, imageTitle)
            put(MediaStore.Images.Media.DESCRIPTION, imageDescription)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Log.d("image1", MediaStore.Images.Media.RELATIVE_PATH)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Received-Images-MinerFinder")
            }
        }

        Log.d("image2", MediaStore.Images.Media.RELATIVE_PATH)


        val contentResolver = context.contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (imageUri != null) {
            val outputStream = contentResolver.openOutputStream(imageUri)
            val inputStream = uri?.let { context.contentResolver.openInputStream(it) }
            if (inputStream != null && outputStream != null) {
                try {
                    inputStream.copyTo(outputStream)
                } catch (e: Exception) {
                    Log.e("SaveToPhotos", "Error copying image: ${e.message}")
                } finally {
                    inputStream.close()
                    outputStream.close()
                }
            }

        } else {
            Log.e("SaveToPhotos", "Failed to save image to MediaStore.")


        }

    }

override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
    if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
        Log.d("PATH", "ST SUCC")

        val payloadId = update.payloadId
        val payload = incomingFilePayloads.remove(payloadId)
        completedFilePayloads.put(payloadId, payload)

        Log.d("PATH", completedFilePayloads.toString())

        if (payload != null && payload.type == Payload.Type.FILE) {
            processFilePayload(payloadId)
        }
    }
}
}

/** Helper class to serialize and deserialize an Object to byte[] and vice-versa  */
object SerializationHelper {
@Throws(IOException::class)
fun serialize(`object`: Any?): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
    // transform object to stream and then to a byte array
    objectOutputStream.writeObject(`object`)
    objectOutputStream.flush()
    objectOutputStream.close()
    return byteArrayOutputStream.toByteArray()
}

@Throws(IOException::class, ClassNotFoundException::class)
fun deserialize(bytes: ByteArray?): Any {
    val byteArrayInputStream = ByteArrayInputStream(bytes)
    val objectInputStream = ObjectInputStream(byteArrayInputStream)
    return objectInputStream.readObject()
}
}


    private fun makeConnection(endpointId: String)
    {
        // should use a list
        // right now, seeks the first viable connection
        if (links.isNotEmpty() && links[0].isNotEmpty()) {

            val endpointId = links[0][0]

            // An endpoint was found. We request a connection to it.
            Nearby.getConnectionsClient(context)
                .requestConnection(getLocalUserName(), endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(
                    OnSuccessListener { unused: Void? ->
                        connectedEndpoints.add(endpointId)
                        showImageChooser(endpointId)
                    })
                .addOnFailureListener(
                    OnFailureListener { e: java.lang.Exception? -> })
        }
        else
        {
            Log.d(TAG,"No endpoints were found!")
            val duration = Toast.LENGTH_SHORT
            val toast = Toast.makeText(applicationContext, "No endpoints were found", duration)
            toast.show()


        }

    }

    fun showEndpointDialog() {
        if(links.isNotEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Endpoint")

            //builder.setMessage("Select an endpoint to connect to:")
            val endpointList = links.map { it[1] }

            val endpointNames = endpointList.toTypedArray()
            var selectedItem: Int = 0

            builder.setSingleChoiceItems(endpointNames, 1,
                DialogInterface.OnClickListener { dialog, which ->
                    selectedItem = which
                })

            builder.setPositiveButton(
                "OK"
            ) { dialog, which ->
                //when user clicks OK
                val selectedEndpoint = endpointList[selectedItem]
                // send to the selected endpoint here
                val toSend = links.find { it[1] == selectedEndpoint }
                val targetEndpoint = toSend?.get(0).toString()
                if(targetEndpoint != null)
                {
                    makeConnection(targetEndpoint)
                }

            }



            builder.setNegativeButton("Cancel") { dialog, which ->
                // Handle cancel action if needed
            }

            val dialog = builder.create()
            dialog.show()
            //val duration = Toast.LENGTH_SHORT
            //val toast = Toast.makeText(applicationContext, endpointNames[0].toString(), 5)
            //toast.show()
        }
    }


    fun createCompressedPayload(context: Context, uri: Uri?): Payload {
        val contentResolver: ContentResolver = context.contentResolver

        return try {
            val inputStream: InputStream? = uri?.let { contentResolver.openInputStream(it) }

            // Check if the InputStream is not null
            if (inputStream != null) {

                val bitmap = BitmapFactory.decodeStream(inputStream)

                // Specify the compression quality (0-100)
                val compressionQuality = 75

                val outputStream = ByteArrayOutputStream()

                // Compress the Bitmap into the ByteArrayOutputStream with the specified quality
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)

                val compressedImageBytes = outputStream.toByteArray()

                Payload.fromBytes(compressedImageBytes)
            } else {
                Log.e("PhotoConnection", "InputStream is null")
                Payload.fromBytes(byteArrayOf())
            }
        } catch (e: FileNotFoundException) {
            Log.e("PhotoConnection", "File not found", e)
            Payload.fromBytes(byteArrayOf())
        }
    }



    private fun db(): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
    }

}