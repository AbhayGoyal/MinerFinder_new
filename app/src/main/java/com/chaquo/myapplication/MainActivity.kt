package com.chaquo.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.chaquo.myapplication.db.AppDatabase
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 90

    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // binds to the activity_main layout file (currently not really used for this demo)
        setContentView(R.layout.activity_main)

        // ask for permission for StepCounter
        checkPermissions()


        // starts the step counter
        startService(Intent(this, StepCounter::class.java))

        // debugging
        Log.d("MAIN;USERNAME", "${Helper().getLocalUserName(applicationContext)}")

        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        val py = Python.getInstance()
        // runs the python file called plot.py in the python folder
        val module = py.getModule("plotScriptable")
        //module.callAttr("mainScriptable", "3.csv")
        //module.callAttr("mainScriptable", "short_short_data_25.csv")

        //py.getModule("plot")

        // saves some files to get data
        //val module2 = py.getModule("dataScript")
        //module2.callAttr("main", "2.csv")
        //module2.callAttr("main", "3.csv")

        val logoutButton = findViewById<Button>(R.id.ButtonLogout)

        logoutButton.setOnClickListener {
            // Call the function you want to execute when the button is clicked
            logout()
        }


        Log.d(TAG, " CHECK: finished calling main")

         // this is just a test app
        // the result from the models are all being outputted in the logcat (nothing is being displayed
        // or used on the app gui)
        // NOTE: inside the python file, you can call short_data_25.csv (~1 min execution) or short_short_data_25.csv
        // (~6 min execution)
        // FUTURE WORK: currently changesToGraph is being run using a ptl file but GCNModelVAE is being
        // run directly with torch on the app. It may execute faster if that is turned into a ptl file
    }

    fun navigationView(view: View?) {
        val intent = Intent(this, RecyclerView2::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun cameraView(view: View?)
    {
        val intent = Intent(this, Camera::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun connectionView(view: View?)
    {
        val intent = Intent(this, PhotoConnection::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun connection2View(view: View?)
    {
        val intent = Intent(this, Connection::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }



    fun accountView(view: View?) {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun sensorsView(view: View?) {
        val intent = Intent(this, DataDisplay::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun dataView(view: View?) {
        val intent = Intent(this, MinerDataDisplay::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun contactView(view: View?) {
        val intent = Intent(this, ContactDisplay::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun galleryView(view: View?) {
        val intent = Intent(this, Photos::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun testLocationView(view: View?) {
        val intent = Intent(this, TestLocation::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }




    private fun db(): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
    }

    private fun logout() {
        val user = db().userDao().findActive()
        if (user != null) {
            db().userDao().log_in_out(user.uid, false)

            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivityForResult(intent, REQUEST_LOGIN)
        }
    }

    companion object {
        const val REQUEST_LOGIN = 123 // Use any unique request code value
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission denied, ask user for permission
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION)
        }
    }

    override fun onDestroy() {
        // ends the background service step counter
        stopService(Intent(this, StepCounter::class.java))
        super.onDestroy()
    }




}



