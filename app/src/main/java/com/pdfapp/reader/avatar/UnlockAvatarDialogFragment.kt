package com.pdfapp.reader.avatar

import android.app.Dialog
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.prefers.AppPrefs

class UnlockAvatarDialogFragment(
    private val unlockType: Int,
    private val unlockPosition: Int
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_unlock_avatar, null)
        val imgAvatar = view.findViewById<ImageView>(R.id.imgAvatarUnlock)
        val imgBanner = view.findViewById<ImageView>(R.id.imgBannerUnlock)
        val txtType = view.findViewById<TextView>(R.id.txtUnlockType)
        val txtPrice = view.findViewById<TextView>(R.id.txtUnlockPrice)
        val tvYouPoint = view.findViewById<TextView>(R.id.tvYouPoint)

        tvYouPoint.text = "Your Point: ${AppPrefs.get().pointUser}"

        val srcIntUnlock = if (unlockType == 0) {
            when (unlockPosition) {
                0 -> {
                    R.drawable.avatar_01
                }

                1 -> {
                    R.drawable.avatar_02
                }

                2 -> {
                    R.drawable.avatar_03
                }

                3 -> {
                    R.drawable.avatar_04
                }

                4 -> {
                    R.drawable.avatar_05
                }

                5 -> {
                    R.drawable.avatar_06
                }

                6 -> {
                    R.drawable.avatar_07
                }

                else -> {
                    R.drawable.avatar_01
                }
            }
        } else {
            when (unlockPosition) {
                0 -> {
                    R.drawable.banner_01
                }

                1 -> {
                    R.drawable.banner_02
                }

                2 -> {
                    R.drawable.banner_03
                }

                3 -> {
                    R.drawable.banner_04
                }

                4 -> {
                    R.drawable.banner_05
                }

                5 -> {
                    R.drawable.banner_06
                }

                6 -> {
                    R.drawable.banner_07
                }

                else -> {
                    R.drawable.banner_01
                }
            }
        }
        imgBanner.isInvisible = unlockType == 0
        imgAvatar.setImageResource(srcIntUnlock)
        imgBanner.setImageResource(srcIntUnlock)
        txtType.text = if (unlockType == 0) {
            "Unlock Avatar"
        } else {
            "Unlock Banner"
        }

        txtPrice.text = if (unlockPosition <= 1) {
            "Apply"
        } else {
            if (unlockType == 0) {
                when (unlockPosition) {
                    2 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 2
                            }) {
                            "AppLy"
                        } else {
                            "100 point"
                        }
                    }

                    3 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 3
                            }) {
                            "AppLy"
                        } else {
                            "200 point"
                        }
                    }

                    4 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 4
                            }) {
                            "AppLy"
                        } else {
                            "500 point"
                        }
                    }

                    5 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 5
                            }) {
                            "AppLy"
                        } else {
                            "1000 point"
                        }
                    }

                    6 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 6
                            }) {
                            "AppLy"
                        } else {
                            "1500 point"
                        }
                    }

                    else -> {
                        "AppLy"
                    }
                }
            } else {
                when (unlockPosition) {
                    2 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 2
                            }) {
                            "AppLy"
                        } else {
                            "100 point"
                        }
                    }

                    3 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 3
                            }) {
                            "AppLy"
                        } else {
                            "200 point"
                        }
                    }

                    4 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 4
                            }) {
                            "AppLy"
                        } else {
                            "500 point"
                        }
                    }

                    5 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 5
                            }) {
                            "AppLy"
                        } else {
                            "1000 point"
                        }
                    }

                    6 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 6
                            }) {
                            "AppLy"
                        } else {
                            "1500 point"
                        }
                    }

                    else -> {
                        "AppLy"
                    }
                }
            }
        }

        txtPrice.setOnClickListener {
            if (unlockType == 0) {
                when (unlockPosition) {
                    0 -> {
                        Toast.makeText(
                            requireContext(),
                            "Apply Success",
                            Toast.LENGTH_SHORT
                        ).show()
                        AppPrefs.get().applyAvatarPosition = 0
                        dismiss()
                    }

                    1 -> {
                        Toast.makeText(
                            requireContext(),
                            "Apply Success",
                            Toast.LENGTH_SHORT
                        ).show()
                        AppPrefs.get().applyAvatarPosition = 1
                        dismiss()
                    }

                    2 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 2
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyAvatarPosition = 2
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 100) {
                                AppPrefs.get().pointUser -= 100
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListAvatarPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(2)
                                AppPrefs.get().unLockListAvatarPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyAvatarPosition = 2
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    3 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 3
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyAvatarPosition = 3
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 200) {
                                AppPrefs.get().pointUser -= 200
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListAvatarPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(3)
                                AppPrefs.get().unLockListAvatarPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyAvatarPosition = 3
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    4 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 4
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyAvatarPosition = 4
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 500) {
                                AppPrefs.get().pointUser -= 500
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListAvatarPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(4)
                                AppPrefs.get().unLockListAvatarPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyAvatarPosition = 4
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    5 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 5
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyAvatarPosition = 5
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 1000) {
                                AppPrefs.get().pointUser -= 1000
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListAvatarPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(5)
                                AppPrefs.get().unLockListAvatarPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyAvatarPosition = 5
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    6 -> {
                        if (AppPrefs.get().unLockListAvatarPosition.any {
                                it == 6
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyAvatarPosition = 6
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 1500) {
                                AppPrefs.get().pointUser -= 1500
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListAvatarPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(6)
                                AppPrefs.get().unLockListAvatarPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyAvatarPosition = 6
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "Apply Default",
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                }
            } else {
                when (unlockPosition) {
                    0 -> {
                        Toast.makeText(
                            requireContext(),
                            "Apply Success",
                            Toast.LENGTH_SHORT
                        ).show()
                        AppPrefs.get().applyBannerPosition = 0
                        dismiss()
                    }

                    1 -> {
                        Toast.makeText(
                            requireContext(),
                            "Apply Success",
                            Toast.LENGTH_SHORT
                        ).show()
                        AppPrefs.get().applyBannerPosition = 1
                        dismiss()
                    }

                    2 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 2
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyBannerPosition = 2
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 100) {
                                AppPrefs.get().pointUser -= 100
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListBannerPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(2)
                                AppPrefs.get().unLockListBannerPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyBannerPosition = 2
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    3 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 3
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyBannerPosition = 3
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 200) {
                                AppPrefs.get().pointUser -= 200
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListBannerPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(3)
                                AppPrefs.get().unLockListBannerPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyBannerPosition = 3
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    4 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 4
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyBannerPosition = 4
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 500) {
                                AppPrefs.get().pointUser -= 500
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListBannerPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(4)
                                AppPrefs.get().unLockListBannerPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyBannerPosition = 4
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    5 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 5
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyBannerPosition = 5
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 1000) {
                                AppPrefs.get().pointUser -= 1000
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListBannerPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(5)
                                AppPrefs.get().unLockListBannerPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyBannerPosition = 5
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    6 -> {
                        if (AppPrefs.get().unLockListBannerPosition.any {
                                it == 6
                            }) {
                            Toast.makeText(
                                requireContext(),
                                "Apply Success",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppPrefs.get().applyBannerPosition = 6
                            dismiss()
                        } else {
                            if (AppPrefs.get().pointUser >= 1500) {
                                AppPrefs.get().pointUser -= 1500
                                val listUnLockListAvatarPosition =
                                    AppPrefs.get().unLockListBannerPosition.toSet().toMutableList()
                                listUnLockListAvatarPosition.add(6)
                                AppPrefs.get().unLockListBannerPosition =
                                    listUnLockListAvatarPosition
                                AppPrefs.get().applyBannerPosition = 6
                                Toast.makeText(
                                    requireContext(),
                                    "Apply Success",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Not enough points, please buy more points to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "Apply Default",
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                }
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    override fun onResume() {
        super.onResume()
        val window: Window = dialog?.window!!
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val size = Point()
        val display: Display = window.windowManager.defaultDisplay
        display.getSize(size)
        window.setLayout((size.x * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        super.onResume()
    }
}