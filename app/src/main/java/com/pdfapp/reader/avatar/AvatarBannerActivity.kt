package com.pdfapp.reader.avatar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.vtsoft.pdfapp.reader.databinding.ActivityAvatarBannerBinding

class AvatarBannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvatarBannerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarBannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Adapter 2 trang: Avatar / Banner
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> AvatarFragment()   // giữ nguyên như bản Fragment cũ
                1 -> BannerFragment()   // từ com.tracker.waterreminder.fragment
                else -> error("Unexpected position $position")
            }
        }

        // Tab titles
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Avatar"
                1 -> "Banner"
                else -> null
            }
        }.attach()
    }
}
