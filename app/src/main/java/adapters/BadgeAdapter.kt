package adapters

import com.example.gooter_proyecto.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import models.Badge

class BadgeAdapter(private val badges: List<Badge>) :
    RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    inner class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val badgeTitle: TextView = itemView.findViewById(R.id.badgeTitle)
        val badgePoints: TextView = itemView.findViewById(R.id.badgePoints)
        val badgeIcon: ImageView = itemView.findViewById(R.id.badgeIcon)
        val badgeCheck: ImageView = itemView.findViewById(R.id.badgeCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        holder.badgeTitle.text = badge.getName()
        holder.badgePoints.text = badge.getPoints()
        holder.badgeIcon.setImageResource(badge.getIconRes())
        holder.badgeCheck.visibility = if (badge.isUnlocked()) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount(): Int = badges.size
}
