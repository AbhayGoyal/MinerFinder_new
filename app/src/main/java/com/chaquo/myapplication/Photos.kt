package com.chaquo.myapplication

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class Photos : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var button: Button
    private val pickImage = 100
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos)
        title = "KotlinApp"
        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.buttonLoadPicture)

        //val folder = pickImageFolder()

        button.setOnClickListener {
            //val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            //startActivityForResult(gallery, pickImage)

            //val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            //galleryIntent.type = "image/*"
            //galleryIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            //galleryIntent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(folder))
            //startActivityForResult(galleryIntent, pickImage)

            //openCustomImagePicker()
            pickImageFolder()

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            imageView.setImageURI(imageUri)
        }
    }

    private fun pickImageFolder(){
        val images = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf("Pictures/MinerFinder-Image")

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val imageId = it.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(queryUri, imageId)
                images.add(contentUri)
            }
        }
    }


    private fun openCustomImagePicker() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.MediaColumns.ARTIST} = ?"
        val selectionArgs = arrayOf("minerfinder-images")

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

        val imageUris = mutableListOf<Uri>()

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val data = it.getString(dataColumn)

                val contentUri = ContentUris.withAppendedId(queryUri, id)
                val imageUri = Uri.parse("file://$data")

                imageUris.add(contentUri)
            }
        }

        val galleryIntent = Intent(Intent.ACTION_VIEW)
        galleryIntent.setDataAndType(imageUris.first(), "image/*")
        startActivityForResult(galleryIntent, pickImage)
    }

}