package com.example.firebasepushex

import android.util.Log
import com.google.gson.Gson
import java.lang.Exception

data class Profile(
    var imageUrl: String?,
    var email: String?,
    var name: String?,
    var comment: String?,
    var uid: String? = null
) {
    companion object {
        fun makeInstance(map: Map<String,Any>?): Profile? {
            map ?: return null
            try {
                val json = Gson().toJson(map)
                return Gson().fromJson(json, Profile::class.java)
            } catch (e: Exception) {
                Log.d("FirebasePushEx", "Profile parsing error: " + e.message)
            }
            return null
        }
    }
}