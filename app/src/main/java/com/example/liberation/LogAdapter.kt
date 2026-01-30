package com.example.liberation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView

// 클래스 이름을 LogAdapter로 변경합니다.
class LogAdapter(private val logs: List<AnalysisEvent>) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtUrl: TextView = view.findViewById(R.id.txtUrl)
        val txtDecision: TextView = view.findViewById(R.id.txtDecision)
        val txtDate: TextView = view.findViewById(R.id.txtDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        holder.txtUrl.text = log.final_url_display
        holder.txtDate.text = log.ts
        holder.txtDecision.text = log.decision

        when (log.decision) {
            "BLOCK" -> holder.txtDecision.setTextColor(Color.RED)
            "HOLD" -> holder.txtDecision.setTextColor(Color.parseColor("#FFA500"))
            "ALLOW" -> holder.txtDecision.setTextColor(Color.GREEN)
            "BYPASS" -> holder.txtDecision.setTextColor((Color.parseColor("#FFFF00")))
        }
    }

    override fun getItemCount() = logs.size
}