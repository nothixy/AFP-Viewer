package org.flval.afpviewer

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.io.File
import java.net.URL


class Adapter(private val dataSets: ArrayList<JSONObject>, private var context: Context, private val token: String): RecyclerView.Adapter<Adapter.ViewHolder>() {
    private val mUiThread: Thread? = null
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        val tagsTextView: LinearLayout = view.findViewById(R.id.tagsTextView)
        val subtitleTextView: TextView = view.findViewById(R.id.subtitleTextView)
        val cardView: CardView = view.findViewById(R.id.cardView)
        val imageView: ImageView = view.findViewById(R.id.app_bar_view)
        val bolt: LinearLayout = view.findViewById(R.id.bolt)
        val llv: LinearLayout = view.findViewById(R.id.llv)
        val llv2: LinearLayout = view.findViewById(R.id.llv2)
        val loader: ProgressBar = view.findViewById(R.id.loader)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.cards, viewGroup, false)
        return ViewHolder(view)
    }
    fun removeAt(position: Int, view: View, message: String, action: Boolean, saved: Boolean, bottomNavigationView: BottomNavigationView) {
        val removed: JSONObject = dataSets[position]
        if (saved) {
            val savedData = File(context.filesDir, dataSets[position].getString("uno"))
            savedData.delete()
        }
        dataSets.removeAt(position)
        val snackbar: Snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.anchorView = bottomNavigationView
        if (action) {
            snackbar.setAction(this.context.getString(R.string.cancel)) {
                val file = File(context.filesDir, removed.getString("uno"))
                file.writeText(removed.toString())
                dataSets.add(position, removed)
                notifyItemInserted(position)
            }
        }
        snackbar.show()
        notifyItemRemoved(position)
    }
    fun saveForLater(position: Int, isSaved: Boolean) {
        if (!isSaved) {
            val dataSet = dataSets[position]
            val location = File(context.filesDir, dataSet.getString("uno"))
            location.writeText(dataSet.toString())

        } else {
            return
        }
    }
    override fun getItemCount() = dataSets.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bolt.visibility = View.GONE
        holder.cardView.visibility = View.VISIBLE
        holder.llv.visibility = View.VISIBLE
        holder.llv2.visibility = View.VISIBLE
        holder.imageView.visibility = View.VISIBLE
        holder.titleTextView.visibility = View.VISIBLE
        holder.tagsTextView.visibility = View.VISIBLE
        holder.subtitleTextView.visibility = View.VISIBLE
        holder.loader.visibility = View.VISIBLE
        val dataSet: JSONObject = dataSets[position]
        val title: String = dataSet.getString("title")
        holder.titleTextView.text = title
        val href: String?
        try {
            val bagItem0: JSONObject = dataSet.getJSONArray("bagItem")[0] as JSONObject
            try {
                val medias0: JSONObject = bagItem0.getJSONArray("medias").get(0) as JSONObject
                href = medias0.getString("href")
                Thread {
                    val drawable = loadImage(URL(href))
                    runOnUiThread {
                        if (drawable != null) {
                            holder.imageView.setImageDrawable(drawable)
                        } else {
                            holder.loader.visibility = View.GONE
                        }
                        holder.loader.visibility = View.GONE
                    }
                }.start()
            } catch (e: Exception) {
                holder.loader.visibility = View.GONE
                holder.imageView.visibility = View.GONE
            }
        } catch (e: Exception) {
            holder.imageView.visibility = View.GONE
            holder.loader.visibility = View.GONE
            holder.tagsTextView.visibility = View.GONE
        }
        try {
            val slugs = dataSet.getJSONArray("slug")
            var textView: TextView
            for (i in 0 until slugs.length()) {
                textView = TextView(holder.tagsTextView.context)
                textView.text = slugs[i].toString()
                textView.id = View.generateViewId()
                val linearLayoutParams: LinearLayout.LayoutParams = (holder.tagsTextView).layoutParams as LinearLayout.LayoutParams
                linearLayoutParams.setMargins(5,0,5,0)
                linearLayoutParams.width = LayoutParams.WRAP_CONTENT
                textView.layoutParams = linearLayoutParams
                textView.setPadding(5,0,5,0)

                val randomColor = Color.rgb((Math.random() * 256).toFloat(), (Math.random() * 256).toFloat(), (Math.random() * 256).toFloat())
                if (ColorUtils.calculateLuminance(randomColor) < 0.3)
                    textView.setTextColor(Color.WHITE)
                else
                    textView.setTextColor(Color.BLACK)
                textView.setBackgroundColor(randomColor)
                (holder.tagsTextView as ViewGroup).addView(textView)
            }
        } catch (e: Exception) {
            holder.tagsTextView.visibility = View.GONE
        }
        try {
            val subtitle: String = dataSet.getJSONArray("caption")[0] as String
            holder.subtitleTextView.text = subtitle
        } catch (e: Exception) {
            holder.subtitleTextView.visibility = View.GONE
        }
        if (dataSet.getJSONArray("news").length() == 1) {
            holder.bolt.visibility = View.VISIBLE
        }
        holder.cardView.setOnClickListener {
            val intent = Intent(holder.cardView.context, ViewerActivity::class.java)
            intent.putExtra("data", dataSet.toString())
            intent.putExtra("token", this.token)
            val options = ActivityOptions.makeSceneTransitionAnimation(holder.cardView.context as Activity, android.util.Pair.create(holder.titleTextView, "titleTextView"))
            holder.cardView.context.startActivity(intent, options.toBundle())
        }
    }
    private fun loadImage(url: URL): Drawable? {
        try {
            return Drawable.createFromStream(url.openStream(), "src")
        } catch (e: Exception) {
            return null
        }
    }
    private fun runOnUiThread(action: Runnable) {
        if (Thread.currentThread() != mUiThread) {
            Handler(Looper.getMainLooper()).post(action)
        } else {
            action.run()
        }
    }
}