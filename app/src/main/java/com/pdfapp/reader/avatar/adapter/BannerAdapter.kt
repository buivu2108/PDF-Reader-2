package com.pdfapp.reader.avatar.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.avatar.model.AvatarBanner

class BannerAdapter(
    private val avatarBanners: List<AvatarBanner>,
    val bannerClick: (position: Int) -> Unit
) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bannerImage: ImageView = itemView.findViewById(R.id.banner_image)
        val bannerPrice: TextView = itemView.findViewById(R.id.bannerPrice)
        val imgLock: ImageView = itemView.findViewById(R.id.imgLock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val banner = avatarBanners[position]
        holder.bannerImage.setImageResource(banner.imageResId)
        holder.bannerPrice.text = banner.price
        holder.imgLock.isVisible = banner.isLock
        holder.itemView.setOnClickListener {
            bannerClick.invoke(position)
        }
    }

    override fun getItemCount(): Int {
        return avatarBanners.size
    }
}
