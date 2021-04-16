package org.flval.afpviewer

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL


class MLTAdapter(private val dataSets: ArrayList<JSONObject>, private val token: String): RecyclerView.Adapter<MLTAdapter.ViewHolder>() {
    private val mUiThread: Thread? = null
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.titleTextView2)
        val cardView: CardView = view.findViewById(R.id.cardView2)
        val imageView: ImageView = view.findViewById(R.id.app_bar_view2)
        val llv: RelativeLayout = view.findViewById(R.id.llv2_1)
        val llv2: LinearLayout = view.findViewById(R.id.llv2_2)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.horizcards, viewGroup, false)
        return ViewHolder(view)
    }
    override fun getItemCount() = dataSets.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.cardView.visibility = View.VISIBLE
        holder.llv.visibility = View.VISIBLE
        holder.llv2.visibility = View.VISIBLE
        holder.imageView.visibility = View.VISIBLE
        holder.titleTextView.visibility = View.VISIBLE
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
                        holder.imageView.setImageDrawable(drawable)
                    }
                }.start()
            } catch (e: Exception) {
                holder.imageView.visibility = View.GONE
            }
        } catch (e: Exception) {
            holder.imageView.visibility = View.GONE
        }
        holder.cardView.setOnClickListener {

            val intent = Intent(holder.cardView.context, ViewerActivity::class.java)
            intent.putExtra("data", dataSet.toString())
            intent.putExtra("token", this.token)
            val options: ActivityOptions = if (holder.imageView.visibility != View.GONE) {
                val bitmap2: Bitmap = Bitmap.createBitmap(holder.imageView.width, holder.imageView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap2)
                holder.imageView.draw(canvas)
                val stream = ByteArrayOutputStream()
                bitmap2.compress(Bitmap.CompressFormat.JPEG, 20, stream)
                val bytes = stream.toByteArray()
                intent.putExtra("image", bytes)
                ActivityOptions.makeSceneTransitionAnimation(holder.cardView.context as Activity, android.util.Pair.create(holder.imageView, "imageView"), android.util.Pair.create(holder.titleTextView, "titleTextView"))
            } else {
                intent.putExtra("image", "null".toByteArray())
                ActivityOptions.makeSceneTransitionAnimation(holder.cardView.context as Activity, android.util.Pair.create(holder.titleTextView, "titleTextView"))
            }
            holder.cardView.context.startActivity(intent, options.toBundle())
        }
    }
    private fun loadImage(url: URL): Drawable {
        return Drawable.createFromStream(url.openStream(), "src")
    }
    private fun runOnUiThread(action: Runnable) {
        if (Thread.currentThread() != mUiThread) {
            Handler(Looper.getMainLooper()).post(action)
        } else {
            action.run()
        }
    }
}