package com.example.firebasepushex

import com.example.firebasepushex.Constant.DB_PUSH_TOKENS
import com.example.firebasepushex.Constant.FIELD_PUSH_TOKEN
import com.example.firebasepushex.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*

class FcmPush() {
    private val JSON = MediaType.parse("application/json; charset=utf-8")
    private val url = "https://fcm.googleapis.com/fcm/send"
    // Get serverKey from - https://console.firebase.google.com/project/fir-pushex-990e6/settings/cloudmessaging/android:com.example.firebasepushex
    private val serverKey = "From - https://console.firebase.google.com/project/fir-pushex-990e6/settings/cloudmessaging/android:com.example.firebasepushex"
    private var okHttpClient: OkHttpClient = OkHttpClient()
    private var callback: Callback? = null

    constructor(callback: Callback?) : this() {
        this.callback = callback
    }

    fun sendMessage(targetUid: String, title: String, message: String) {
        FirebaseFirestore.getInstance().collection(DB_PUSH_TOKENS).document(targetUid)
            .get().addOnCompleteListener { task ->
                if(!task.isSuccessful) return@addOnCompleteListener
                val token = task.result[FIELD_PUSH_TOKEN].toString()
                var pushDTO = PushDTO(token, PushDTO.Notification(title, message))
                var body = RequestBody.create(JSON, Gson().toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=$serverKey")
                    .url(url).post(body).build()
                okHttpClient.newCall(request).enqueue(callback)
            }
    }
}