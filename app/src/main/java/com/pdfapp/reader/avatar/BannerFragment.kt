package com.pdfapp.reader.avatar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pdfapp.reader.avatar.adapter.BannerAdapter
import com.pdfapp.reader.avatar.model.AvatarBanner
import com.pdfapp.reader.avatar.utils.MarginItemDecoration
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.prefers.AppPrefs

class BannerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bannerAdapter: BannerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_banner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewBanner)
        recyclerView.layoutManager = LinearLayoutManager(context)
        if (recyclerView.itemDecorationCount == 0) {
            val margin = resources.getDimensionPixelSize(R.dimen._6dp)
            recyclerView.addItemDecoration(
                MarginItemDecoration(margin)
            )
        }

        bannerAdapter = BannerAdapter(getBannerData(), bannerClick = {
            UnlockAvatarDialogFragment(
                unlockType = 1,
                unlockPosition = it
            ).show(parentFragmentManager, "UnlockAvatarDialog")
        })
        recyclerView.adapter = bannerAdapter
    }

    private fun getBannerData(): List<AvatarBanner> {
        // Replace with actual data retrieval logic
        return listOf(
            AvatarBanner(
                imageResId = R.drawable.banner_01,
                price = "Free",
                isLock = false
            ),
            AvatarBanner(
                imageResId = R.drawable.banner_02,
                price = "Free",
                isLock = false
            ),
            AvatarBanner(
                imageResId = R.drawable.banner_03,
                price = "100 point",
                isLock = AppPrefs.get().unLockListBannerPosition.any {
                    it == 2
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.banner_04,
                price = "200 point",
                isLock = AppPrefs.get().unLockListBannerPosition.any {
                    it == 3
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.banner_05,
                price = "500 point",
                isLock = AppPrefs.get().unLockListBannerPosition.any {
                    it == 4
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.banner_06,
                price = "1000 point",
                isLock = AppPrefs.get().unLockListBannerPosition.any {
                    it == 5
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.banner_07,
                price = "1500 point",
                isLock = AppPrefs.get().unLockListBannerPosition.any {
                    it == 6
                }
            )

        )
    }
}