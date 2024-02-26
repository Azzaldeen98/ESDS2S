package com.example.esds2s.Helpers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.esds2s.Interface.IBaseItemClickListener
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.R

class CustomAdapter(private val mList: List<BaseChatResponse>,private val clickListener: IBaseItemClickListener<BaseChatResponse>,private val context: FragmentActivity) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {


        // Create an instance of AdapterDataObserver
        private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                notifyDataSetChanged()
                Log.d("onChanged","onChanged")
            }

            // Add more overridden methods if needed (onItemRangeInserted, onItemRangeRemoved, etc.)
        }

        init {

            Log.d("onCreate","onCreate")
            // Register the AdapterDataObserver when the adapter is created
            registerAdapterDataObserver(adapterDataObserver)
        }

        // ...

        // Unregister the AdapterDataObserver when the adapter is no longer needed
        fun unregisterAdapterDataObserver() {
            Log.d("Unregister","Unregister")
            unregisterAdapterDataObserver(adapterDataObserver)
        }





    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        // sets the image to the imageview from our itemHolder class
//        holder.imageView.setImageResource(R.drawable.robot_splash)
//        // sets the text to the textview from our itemHolder class
        holder.textView.text = ItemsViewModel.scope
        holder.itemContiner.setOnClickListener{v->  clickListener.onItemClick(mList[position])}

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val itemContiner: CardView=  itemView.findViewById(R.id.ResycleChatMainItem)
//        val imageView: ImageView = itemView.findViewById(R.id.ResycleChatImage)
        val textView: TextView = itemView.findViewById(R.id.ResycleChatLabel)



    }
}