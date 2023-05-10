package org.flval.afpviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.URL


class MainActivity: AppCompatActivity() {
    private var token: String = ""
    private lateinit var bottomNavigationView: BottomNavigationView
    private var previousView = 1
    private var number: Int = 0
    private lateinit var topics: Array<String>
    private var lang: String = ""
    private var loginMode: String = ""
    private var accessCode: String = ""
    private var username: String = ""
    private var password: String = ""
    private var endURL: String = ""
    private var apiURL: String = "https://afp-apicore-prod.afp.com"
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val alertDialog = MaterialAlertDialogBuilder(this@MainActivity)
        val star: Drawable? = ContextCompat.getDrawable(this.applicationContext, R.drawable.ic_baseline_star_24)
        val starBitmap: Bitmap = Bitmap.createBitmap(star?.intrinsicWidth!! * 2, star.intrinsicHeight * 2, Bitmap.Config.ARGB_8888)
        val starCanvas = Canvas(starBitmap)
        star.setBounds(0, 0, starCanvas.width, starCanvas.height)
        star.draw(starCanvas)
        val delete: Drawable? = ContextCompat.getDrawable(this.applicationContext, R.drawable.ic_baseline_delete_24)
        val deleteBitmap: Bitmap = Bitmap.createBitmap(delete?.intrinsicWidth!! * 2, delete.intrinsicHeight * 2, Bitmap.Config.ARGB_8888)
        val deleteCanvas = Canvas(deleteBitmap)
        delete.setBounds(0, 0, deleteCanvas.width, deleteCanvas.height)
        delete.draw(deleteCanvas)
        var jsonData: ArrayList<JSONObject>?
        val refreshLayout: SwipeRefreshLayout = findViewById(R.id.swiperefresh)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        number = sharedPreferences.getInt("number", 50)
        topics = (sharedPreferences.getString("topics", "").toString().split(",")).toTypedArray()
        lang = sharedPreferences.getString("lang", "en").toString()
        loginMode = sharedPreferences.getString("loginMode", "anon").toString()
        when (loginMode) {
            "anon" -> {
                endURL = "grant_type=anonymous"
            }
            "temp" -> {
                accessCode = sharedPreferences.getString("accessCode", "").toString()
                if (accessCode != "")
                    endURL = "grant_type=authorization_code&code=$accessCode"
                else
                    alertDialog.setTitle(getString(R.string.missingdata))
                        .setMessage(getString(R.string.missingaccesscode))
                        .setPositiveButton(getString(R.string.gotosettings)
                    ) { _: DialogInterface, _: Int ->
                        val intent = Intent(this.applicationContext, SettingActivity::class.java)
                        startActivityForResult(intent, 5555)
                    }.setNegativeButton(getString(R.string.ok), null).show()
                    endURL = "grant_type=anonymous"
            }
            "perm" -> {
                username = sharedPreferences.getString("username", "").toString()
                password = sharedPreferences.getString("password", "").toString()
                endURL = if ((username != "") and (username != ""))
                    "username=$username&password=$password&grant_type=password"
                else {
                    alertDialog.setTitle(getString(R.string.missingdata))
                        .setMessage(getString(R.string.missingunamepwd))
                        .setPositiveButton(
                            getString(R.string.gotosettings)
                        ) { _: DialogInterface, _: Int ->
                            val intent =
                                Intent(this.applicationContext, SettingActivity::class.java)
                            startActivityForResult(intent, 5555)
                        }.setNegativeButton(getString(R.string.ok), null).show()
                    "grant_type=anonymous"
                }
            }
        }
        bottomNavigationView = findViewById(R.id.bottomAppBar)
        val rcv = findViewById<RecyclerView>(R.id.rcv)
        val empty: LinearLayout = findViewById(R.id.empty)

        var isSaved = false

        abstract class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                val paint = Paint()
                if (dX > 0) {
                    paint.color = Color.parseColor("#325aff")
                    c.drawRect(viewHolder.itemView.left.toFloat(), viewHolder.itemView.top.toFloat(), viewHolder.itemView.left.toFloat() + dX, viewHolder.itemView.bottom.toFloat(), paint)
                    c.drawBitmap(starBitmap, viewHolder.itemView.left.toFloat() + starBitmap.width / 2, viewHolder.itemView.top.toFloat() + (viewHolder.itemView.height - starBitmap.height) / 2, paint)
                } else {
                    paint.color = Color.parseColor("#ff325b")
                    c.drawRect(viewHolder.itemView.right.toFloat() + dX, viewHolder.itemView.top.toFloat(), viewHolder.itemView.right.toFloat(), viewHolder.itemView.bottom.toFloat(), paint)
                    c.drawBitmap(deleteBitmap, viewHolder.itemView.right.toFloat() - starBitmap.width * 1.5.toFloat(), viewHolder.itemView.top.toFloat() + (viewHolder.itemView.height - starBitmap.height) / 2, paint)
                }
            }
        }
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        rcv.layoutManager = layoutManager
        ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_star_24)!!
        val swipeHandler = object : SwipeToDeleteCallback() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    val adapter = rcv.adapter as Adapter
                    adapter.removeAt(position = viewHolder.bindingAdapterPosition, view = rcv, message = getString(R.string.removed), action = true, saved =  isSaved, bottomNavigationView = findViewById(R.id.bottomAppBar))
                } else if (direction == ItemTouchHelper.RIGHT) {
                    val adapter = rcv.adapter as Adapter
                    adapter.saveForLater(position = viewHolder.bindingAdapterPosition, isSaved = isSaved)
                    adapter.removeAt(position = viewHolder.bindingAdapterPosition, view = rcv, message = getString(R.string.savedforlater), action = false, saved = false, bottomNavigationView = findViewById(R.id.bottomAppBar))
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(rcv)
        var adapter: Adapter
        refreshLayout.setOnRefreshListener {
            Thread {
                jsonData = downloadData(token, previousView - 1)
                runOnUiThread {
                    if (jsonData != null) {
                        adapter = Adapter(jsonData!!, this.applicationContext, token)
                        rcv.adapter = adapter
                        (rcv.adapter as RecyclerView.Adapter).notifyDataSetChanged()
                        empty.visibility = View.GONE
                        rcv.visibility = View.VISIBLE
                    } else {
                        empty.visibility = View.VISIBLE
                        rcv.visibility = View.GONE
                    }
                    refreshLayout.isRefreshing = false
                }
            }.start()
        }
        val airplaneMode: LinearLayout = findViewById(R.id.airplaneMode)
        val loader: ProgressBar = findViewById(R.id.progressBar)
        Thread {
            while (!isInternetAvailable(this.applicationContext)) {
                airplaneMode.visibility = View.VISIBLE
                loader.visibility = View.GONE
                Thread.sleep(500)
            }
            loader.visibility = View.VISIBLE
            airplaneMode.visibility = View.GONE
            token = fetchToken(endURL)
            jsonData = downloadData(token, 0)!!
            runOnUiThread {
                if (jsonData != null) {
                    adapter = Adapter(jsonData!!, this.applicationContext, token)
                    rcv.adapter = adapter
                    loader.visibility = View.GONE
                    empty.visibility = View.GONE
                    rcv.visibility = View.VISIBLE
                } else {
                    empty.visibility = View.VISIBLE
                }
            }
        }.start()
        bottomNavigationView.selectedItemId = R.id.defaultfeed
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.defaultfeed -> {
                    if (previousView != 1) {
                        previousView = 1
                        isSaved = false
                        Thread {
                            jsonData = downloadData(token, 0)
                            runOnUiThread {
                                if (jsonData != null) {
                                    adapter = Adapter(jsonData!!, this.applicationContext, token)
                                    rcv.adapter = adapter
                                    (rcv.adapter as RecyclerView.Adapter).notifyDataSetChanged()
                                    empty.visibility = View.GONE
                                    rcv.visibility = View.VISIBLE
                                } else {
                                    empty.visibility = View.VISIBLE
                                    rcv.visibility = View.GONE
                                }
                            }
                        }.start()
                    }
                    true
                }
                R.id.userfeed -> {
                    if ((topics.size == 1) and (topics[0] == "")) {
                        alertDialog.setTitle(getString(R.string.missingdata))
                            .setMessage(getString(R.string.notopicsset))
                            .setPositiveButton(
                                getString(R.string.gotosettings)
                            ) { _: DialogInterface, _: Int ->
                                val intent =
                                    Intent(this.applicationContext, SettingActivity::class.java)
                                startActivityForResult(intent, 5555)
                            }.setNegativeButton(getString(R.string.ok), null).show()
                    } else {
                        if (previousView != 2) {
                            previousView = 2
                            isSaved = false
                            Thread {
                                jsonData = downloadData(token, 1)
                                runOnUiThread {
                                    if (jsonData != null) {
                                        adapter =
                                            Adapter(jsonData!!, this.applicationContext, token)
                                        rcv.adapter = adapter
                                        (rcv.adapter as RecyclerView.Adapter).notifyDataSetChanged()
                                        empty.visibility = View.GONE
                                        rcv.visibility = View.VISIBLE
                                    } else {
                                        empty.visibility = View.VISIBLE
                                        rcv.visibility = View.GONE
                                    }
                                }
                            }.start()
                        }
                    }
                    true
                }
                R.id.saved -> {
                    if (previousView != 3) {
                        previousView = 3
                        isSaved = true
                        Thread {
                            jsonData = downloadData(token, 2)
                            runOnUiThread {
                                if (jsonData != null) {
                                    adapter = Adapter(jsonData!!, this.applicationContext, token)
                                    rcv.adapter = adapter
                                    (rcv.adapter as RecyclerView.Adapter).notifyDataSetChanged()
                                    empty.visibility = View.GONE
                                    rcv.visibility = View.VISIBLE
                                } else {
                                    empty.visibility = View.VISIBLE
                                    rcv.visibility = View.GONE
                                }
                            }
                        }.start()
                    }
                    true
                }
                R.id.settings -> {
                    val intent = Intent(this.applicationContext, SettingActivity::class.java)
                    startActivityForResult(intent, 5555)
                    true
                }
                else -> false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bottomNavigationView.selectedItemId = bottomNavigationView.menu.getItem(previousView).itemId
    }
    private fun fetchToken(endURL: String): String {
        val url = URL("$apiURL/oauth/token?$endURL")
        val connection = url.openConnection()
        connection.setRequestProperty("Allow", "application/json")
        val inputStream: InputStream = BufferedInputStream(connection.getInputStream())
        val content = inputStream.reader().readText()
        val stringStream: String = StringBuilder().append(content).toString()
        val jsonObject = JSONObject(stringStream)
        return jsonObject.getString("access_token")
    }
    private fun downloadData(token: String, feedMode: Int): ArrayList<JSONObject>? {
        var url: URL
        when (feedMode) {
            0 -> {
                url = URL("$apiURL/v1/api/search?lang=$lang&size=$number&q=*%3A*&c=false&sort=published%20desc&tz=GMT&gap=day&to=now&wt=xml&access_token=$token")
                val connection = url.openConnection()
                connection.setRequestProperty("Allow", "application/json")
                val inputStream: InputStream = BufferedInputStream(connection.getInputStream())
                val content = inputStream.reader().readText()
                val stringStream: String = StringBuilder().append(content).toString()
                val jsonObject = JSONObject(stringStream)
                val articles: ArrayList<JSONObject> = ArrayList()
                try {
                    val realData = jsonObject.getJSONObject("response").getJSONArray("docs")
                    for (i in 0 until realData.length()) {
                        articles.add(realData[i] as JSONObject)
                    }
                } catch (e: Exception) {}
                return articles
            }
            1 -> {
                val articles: ArrayList<JSONObject> = ArrayList()
                for (element in topics) {
                    url = URL("$apiURL/v1/api/search?lang=$lang&size=$number&q=*:$element&c=false&sort=published%20desc&tz=GMT&gap=day&to=now&wt=xml&access_token=$token")
                    val connection = url.openConnection()
                    connection.setRequestProperty("Allow", "application/json")
                    val inputStream: InputStream = BufferedInputStream(connection.getInputStream())
                    val content = inputStream.reader().readText()
                    val stringStream: String = StringBuilder().append(content).toString()
                    val jsonObject = JSONObject(stringStream)
                    try {
                        val realData = jsonObject.getJSONObject("response").getJSONArray("docs")
                        for (i in 0 until realData.length()) {
                            articles.add(realData[i] as JSONObject)
                        }
                    } catch (e: Exception) { }
                }
                return articles
            }
            2 -> {
                val articles: ArrayList<JSONObject> = ArrayList()
                for (file in filesDir.listFiles()!!) {
                    articles.add(JSONObject(file.readText()))
                }
                return if (articles.size > 0)
                    articles
                else
                    null
            }
        }
        return null
    }
    private fun isInternetAvailable(context: Context): Boolean {
        val result: Boolean
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        return result
    }
}