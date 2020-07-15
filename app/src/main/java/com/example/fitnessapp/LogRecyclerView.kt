package com.example.fitnessapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var lActivityName: TextView? = null
    private var lTotalTime: TextView? = null
    private var lTotalDistance: TextView? = null
    private var lDeleteButton: Button? = null

    init {
        lActivityName = itemView.findViewById(R.id.textActivityName)
        lTotalTime = itemView.findViewById(R.id.textTotalTime)
        lTotalDistance = itemView.findViewById(R.id.textTotalDistance)
        lDeleteButton = itemView.findViewById(R.id.buttonDelete)
    }

    fun bind(log: GPSActivity, clickListener: OnItemClickListener) {
        lActivityName?.text = log.ActivityName

        if(log.TotalDistance > 1000.0) {
            val distance = log.TotalDistance / 1000
            lTotalDistance?.text = "%.2f".format(distance) + "km"
        }else {
            lTotalDistance?.text = "%.1f".format(log.TotalDistance) + "m"
        }

        lTotalTime?.text = log.TotalTime

        itemView.setOnClickListener {
            clickListener.onItemClickedListener(log)
        }

        lDeleteButton?.setOnClickListener {
            clickListener.onDeleteButtonClickedListener(log)
        }
    }
}

class LogsAdapter(var logList:MutableList<GPSActivity>, private val itemClickListener: OnItemClickListener):
    RecyclerView.Adapter<LogRecyclerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): LogRecyclerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_recycler_view_item, parent, false)
        return LogRecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return logList.size
    }
    override fun onBindViewHolder(holder: LogRecyclerViewHolder, position: Int){
        val list = logList[position]
        holder.bind(list, itemClickListener)
    }
}

interface OnItemClickListener{
    fun onItemClickedListener(log: GPSActivity)
    fun onDeleteButtonClickedListener(log: GPSActivity)
}