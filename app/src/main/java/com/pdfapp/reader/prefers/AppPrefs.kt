package com.pdfapp.reader.prefers

import android.content.SharedPreferences
import com.pdfapp.reader.App

class AppPrefs {

    private val FILE_NAME = "andG_preferences"
    private val KEY_FIRST_OPEN_APP = "KEY_FIRST_OPEN_APP"
    private val KEY_IS_HAS_PURCHASE = "KEY_IS_HAS_PURCHASE"
    private val KEY_IS_HAS_PURCHASE_NUMBER_ACTION = "KEY_IS_HAS_PURCHASE_NUMBER_ACTION"

    private val KEY_UNLOCK_LIST_AVATAR_POSITION = "KEY_UNLOCK_LIST_AVATAR_POSITION"

    private val KEY_UNLOCK_LIST_BANNER_POSITION = "KEY_UNLOCK_LIST_BANNER_POSITION"

    private val KEY_IS_POINT_USER = "KEY_IS_POINT_USER"

    private var prefs: SharedPreferences =
        PreferenceHelper.newPrefs(App.get(), FILE_NAME)

    private val KEY_APPLY_AVATAR_POSITION = "KEY_APPLY_AVATAR_POSITION"

    private val KEY_APPLY_BANNER_POSITION = "KEY_APPLY_BANNER_POSITION"

    var isPurchase: Boolean
        set(value) {
            prefs[KEY_IS_HAS_PURCHASE] = value
        }
        get() = prefs[KEY_IS_HAS_PURCHASE, false] ?: false

    var isPurchaseNumberAction: Int
        set(value) {
            prefs[KEY_IS_HAS_PURCHASE_NUMBER_ACTION] = value
        }
        get() = prefs[KEY_IS_HAS_PURCHASE_NUMBER_ACTION, 0] ?: 0

    var unLockListAvatarPosition: MutableList<Int>
        set(value) {
            prefs[KEY_UNLOCK_LIST_AVATAR_POSITION] = value
        }
        get() = prefs[KEY_UNLOCK_LIST_AVATAR_POSITION, mutableListOf()] ?: mutableListOf()

    var unLockListBannerPosition: MutableList<Int>
        set(value) {
            prefs[KEY_UNLOCK_LIST_BANNER_POSITION] = value
        }
        get() = prefs[KEY_UNLOCK_LIST_BANNER_POSITION, mutableListOf()] ?: mutableListOf()


    var pointUser: Int
        set(value) {
            prefs[KEY_IS_POINT_USER] = value
        }
        get() = prefs[KEY_IS_POINT_USER, 0] ?: 0

    var applyBannerPosition: Int
        set(value) {
            prefs[KEY_APPLY_BANNER_POSITION] = value
        }
        get() = prefs[KEY_APPLY_BANNER_POSITION, -1] ?: -1

    var applyAvatarPosition: Int
        set(value) {
            prefs[KEY_APPLY_AVATAR_POSITION] = value
        }
        get() = prefs[KEY_APPLY_AVATAR_POSITION, -1] ?: -1


    var isNotFirstTimeOpenApp: Boolean
        set(value) {
            prefs[KEY_FIRST_OPEN_APP] = value
        }
        get() = prefs[KEY_FIRST_OPEN_APP, false] ?: false

    companion object {

        private var appPrefs = AppPrefs()

        fun get(): AppPrefs {
            return appPrefs
        }
    }
}