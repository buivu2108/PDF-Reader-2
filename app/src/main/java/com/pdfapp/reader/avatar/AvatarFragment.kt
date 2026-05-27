package com.pdfapp.reader.avatar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.avatar.adapter.AvatarAdapter
import com.pdfapp.reader.avatar.model.AvatarBanner
import com.pdfapp.reader.avatar.utils.MarginItemDecoration
import com.pdfapp.reader.prefers.AppPrefs

class AvatarFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var avatarAdapter: AvatarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_avatar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewAvatars)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        if (recyclerView.itemDecorationCount == 0) {
            val margin = resources.getDimensionPixelSize(R.dimen._6dp)
            recyclerView.addItemDecoration(
                MarginItemDecoration(margin)
            )
        }
        avatarAdapter = AvatarAdapter(getAvatars(), avatarClick = {
            UnlockAvatarDialogFragment(
                unlockType = 0,
                unlockPosition = it
            ).show(parentFragmentManager, "UnlockAvatarDialog")
        })
        recyclerView.adapter = avatarAdapter
    }

    private fun getAvatars(): List<AvatarBanner> {
        // Replace with actual data retrieval logic
        return listOf(
            AvatarBanner(
                imageResId = R.drawable.avatar_01,
                price = "Free",
                isLock = false
            ),
            AvatarBanner(
                imageResId = R.drawable.avatar_02,
                price = "Free",
                isLock = false
            ),
            AvatarBanner(
                imageResId = R.drawable.avatar_03,
                price = "100 point",
                isLock = AppPrefs.get().unLockListAvatarPosition.any {
                    it == 2
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.avatar_04,
                price = "200 point",
                isLock = AppPrefs.get().unLockListAvatarPosition.any {
                    it == 3
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.avatar_05,
                price = "500 point",
                isLock = AppPrefs.get().unLockListAvatarPosition.any {
                    it == 4
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.avatar_06,
                price = "1000 point",
                isLock = AppPrefs.get().unLockListAvatarPosition.any {
                    it == 5
                }
            ),
            AvatarBanner(
                imageResId = R.drawable.avatar_07,
                price = "1500 point",
                isLock = AppPrefs.get().unLockListAvatarPosition.any {
                    it == 6
                }
            )

        )
    }
}