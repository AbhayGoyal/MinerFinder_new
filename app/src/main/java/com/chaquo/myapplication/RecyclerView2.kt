package com.chaquo.myapplication

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toolbar
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader


class RecyclerView2 : AppCompatActivity() {

    private val TAG = "NavigationView"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        //setSupportActionBar(toolbar)
        supportActionBar?.title = "Miner Prediction Results"


        setContentView(R.layout.recycler_view_2)

        Log.d(TAG, "adding the filenames to the list")

        val filesDir1 = applicationContext.filesDir
        val subDirectoryName = "Results"
        val subDirectory = File(filesDir1, subDirectoryName)
        if (!subDirectory.exists()) {
            subDirectory.mkdirs() // Create the subdirectory if it doesn't exist
        }

        val csvFiles = subDirectory.listFiles { file ->
            file.isFile && file.name.endsWith(".csv") && file.name != ".csv"
        }.sortedBy { it.name }


        val itemList: MutableList<FileItem> = mutableListOf()
        val minerList: MutableList<String> = mutableListOf()

        for (file in csvFiles) {

            val filePath1 = File(subDirectory, file.name).absolutePath
            val fileData = readCSVFile(filePath1)

            val lines = fileData.trim().split("\n")
            val firstColumnValues: List<String> = lines.map { it.split(",")[0] }

            // get the first value in the first column:
            val firstValueInFirstColumn = firstColumnValues.getOrNull(1)
            val stringName = "miner " + firstValueInFirstColumn.toString()
            val fileItem = FileItem(stringName, fileData)

            if(firstValueInFirstColumn != null)
            {
                minerList.add(firstValueInFirstColumn.toString())
            }

            if (fileItem != null) {
                itemList.add(fileItem)
            }
        }





        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val adapter = item_extender(this, itemList, object : item_extender.OnItemClickListener {
            override fun onItemClick(item: FileItem) {
                // Handle item click event here, you can access the selected FileItem object


            }
        })

        recyclerView.adapter = adapter



        // also add the list of miners to a display
        val minerListR: TextView = findViewById(R.id.minerListResults)
        minerListR.text = "Miners tracked: " + minerList.joinToString(" ,")


    }

}

private fun readCSVFile(filePath: String): String {
    try {
        val file1 = FileInputStream(filePath)
        val reader1 = InputStreamReader(file1)
        val buffread1 = BufferedReader(reader1)
        val sb = StringBuilder()
        var line: String?
        while (buffread1.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        buffread1.close()
        return sb.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}
