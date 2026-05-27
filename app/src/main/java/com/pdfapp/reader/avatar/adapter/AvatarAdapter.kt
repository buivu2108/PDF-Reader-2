package com.pdfapp.reader.avatar.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.pdfapp.reader.avatar.model.AvatarBanner
import com.google.android.material.imageview.ShapeableImageView
import com.vtsoft.pdfapp.reader.R


class AvatarAdapter(
    private val avatarList: List<AvatarBanner>,
    val avatarClick: (position: Int) -> Unit
) :
    RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ShapeableImageView = itemView.findViewById(R.id.avatar_image)
        val avatarName: TextView = itemView.findViewById(R.id.avatar_price)
        val imgLock: ImageView = itemView.findViewById(R.id.imgLockAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatar = avatarList[position]
        holder.avatarImage.setImageResource(avatar.imageResId)
        holder.avatarName.text = avatar.price
        holder.imgLock.isVisible = avatar.isLock
        holder.itemView.setOnClickListener {
            avatarClick.invoke(position)
        }
    }

    override fun getItemCount(): Int {
        return avatarList.size
    }
}
