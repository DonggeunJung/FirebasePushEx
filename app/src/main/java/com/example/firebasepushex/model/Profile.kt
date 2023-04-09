package com.example.firebasepushex.model

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.firebasepushex.Constant.FIELD_UID
import com.example.firebasepushex.view.ProfileActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.gson.Gson
import java.lang.Exception

data class Profile(
    var imageUrl: String? = null,
    var email: String? = null,
    var name: String? = null,
    var comment: String? = null,
    var uid: String? = null
) {
    companion object {
        fun makeInstance(shot: DocumentSnapshot?): Profile? {
            try {
                val json = Gson().toJson(shot?.data)
                val profile = Gson().fromJson(json, Profile::class.java)
                profile.uid = shot?.id
                return profile
            } catch (e: Exception) {
                Log.d("FirebasePushEx", "Profile parsing error: " + e.message)
            }
            return null
        }

        fun makeIntent(context: Context, profile: Profile): Intent {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra(FIELD_UID, profile.uid)
            return intent
        }

    }
}