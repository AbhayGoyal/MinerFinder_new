package com.chaquo.myapplication

import android.content.Intent
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
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // binds to the activity_main layout file (currently not really used for this demo)
        setContentView(R.layout.activity_main)

        // starts the step counter
        startService(Intent(this, StepCounter::class.java))

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
        val module2 = py.getModule("dataScript")
        module2.callAttr("main", "2.csv")
        module2.callAttr("main", "3.csv")

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
        val intent = Intent(this, Connection::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }


    fun accountView(view: View?) {
        val intent = Intent(this, Account::class.java)
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




}


