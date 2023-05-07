package org.flval.afpviewer

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.URL


class ViewerActivity: AppCompatActivity() {
    private var token: String = ""
    private lateinit var dataSet: JSONObject
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.viewer)
        val imageView: ImageView = findViewById(R.id.imageView)
        token = intent.getStringExtra("token")!!
        val data = intent.getSerializableExtra("data") as String
        dataSet = JSONObject(data)
        var jsonDataReturn: ArrayList<JSONObject>?
        val loader: ProgressBar = findViewById(R.id.loader2)
        try {
            val bagItem: JSONArray = dataSet.getJSONArray("bagItem")
            val lL: LinearLayout = findViewById(R.id.imageSet)
            for (i in 0 until bagItem.length()) {
                val medias = (bagItem[i] as JSONObject).getJSONArray("medias") as JSONArray
                val href = (medias[1] as JSONObject).getString("href")
                if (i == 0) {
                    Thread {
                        val imageDrawable = loadImage(URL(href))
                        runOnUiThread {
                            if (imageDrawable != null) {
                                imageView.setImageDrawable(imageDrawable)
                            } else {
                                imageView.visibility = View.GONE
                            }
                            loader.visibility = View.GONE
                        }
                    }.start()
                }
                val lL2 = LinearLayout(this.applicationContext)
                lL2.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                val iV = ImageView(this.applicationContext)
                val tV = TextView(this.applicationContext)
                val sB = StringBuilder()
                sB.append((bagItem[i] as JSONObject).getString("caption") + " ; " + (bagItem[i] as JSONObject).getString("creator") + " ; " + (bagItem[i] as JSONObject).getJSONObject("newslines").getString("copyright"))
                tV.text = sB.toString()
                iV.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500)
                iV.scaleType = ImageView.ScaleType.CENTER_CROP
                tV.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                tV.setTextColor(ContextCompat.getColor(this, R.color.grey))
                lL.addView(iV)
                lL.addView(tV)
                Thread {
                    val imageDrawable = loadImage(URL(href))
                    runOnUiThread {
                        if (imageDrawable != null) {
                            iV.setImageDrawable(imageDrawable)
                        } else {
                            iV.visibility = View.GONE
                        }
                    }
                }.start()
            }
        } catch (e: Exception) {
            (findViewById<ViewGroup>(R.id.rL)).visibility = View.GONE
            e.printStackTrace()
        }
        val title = dataSet.getString("title")
        val titleTextView: TextView = findViewById(R.id.viewerTitleTextView)
        val fab: FloatingActionButton = findViewById(R.id.floatingActionButton3)
        val uno: String = dataSet.getString("uno")
        titleTextView.text = title
        fab.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "https://srgoti.gitlab.io/afp/afpviewerpage.html?uno=$uno")
                putExtra(Intent.EXTRA_TITLE, titleTextView.text)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
        val mltTextView: TextView = findViewById(R.id.mlt)
        mltTextView.visibility = View.VISIBLE
        val newsTextView: TextView = findViewById(R.id.newsTextView)
        val stringBuilder: StringBuilder = StringBuilder()
        for (i in 0 until dataSet.getJSONArray("news").length() - 1) {
            stringBuilder.append(dataSet.getJSONArray("news")[i].toString() + " ")
        }
        val text = stringBuilder.toString()
        newsTextView.text = text
        val rcv: RecyclerView = findViewById(R.id.viewerRcv)
        Thread {
            jsonDataReturn = loadDataRelated(uno, token)
            runOnUiThread {
                if (jsonDataReturn != null) {
                    val adapter = MLTAdapter(jsonDataReturn!!, token)
                    val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                    layoutManager.reverseLayout = true
                    layoutManager.stackFromEnd = true
                    rcv.layoutManager = layoutManager
                    rcv.adapter = adapter
                } else {
                    mltTextView.visibility = View.GONE
                    (rcv.parent as ViewGroup).removeView(rcv)
                }
            }
        }.start()
    }
    private fun loadDataRelated(uno: String, token: String): ArrayList<JSONObject>? {
        val url = URL("https://afp-apicore-prod.afp.com/v1/api/mlt?uno=$uno&lang=en&size=50&tz=GMT&c=false&gap=day&sort=published%20desc&to=now&wt=xml&access_token=$token")
        val connection = url.openConnection()
        connection.setRequestProperty("Allow", "application/json")
        val inputStream: InputStream = BufferedInputStream(connection.getInputStream())
        val content = inputStream.reader().readText()
        val stringStream: String = StringBuilder().append(content).toString()
        val jsonObject = JSONObject(stringStream)
        return if (jsonObject.getJSONObject("response").getInt("numFound") != 0) {
            val realData = jsonObject.getJSONObject("response").getJSONArray("docs")
            val articles: ArrayList<JSONObject> = ArrayList()
            for (i in 0 until realData.length()) {
                articles.add(realData[i] as JSONObject)
            }
            articles
        } else {
            null
        }
    }
    private fun loadImage(url: URL): Drawable? {
        try {
            return Drawable.createFromStream(url.openStream(), "src")
        } catch (e: Exception) {
            return null
        }
    }
}