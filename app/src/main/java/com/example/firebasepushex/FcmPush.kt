package com.example.firebasepushex

import com.example.firebasepushex.Constant.DB_PUSH_TOKENS
import com.example.firebasepushex.Constant.FIELD_PUSH_TOKEN
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class FcmPush() {
    val JSON = MediaType.parse("application/json; charset=utf-8")
    val url = "https://fcm.googleapis.com/fcm/send"
    val serverKey = "AAAAJdzvZ3g:APA91bH9caQZAz2cvFfFMAI5KpDf-72pVWyUDzXCqSjAxVml7VUqvsV-5Bq_3cf1QKIvLR28s-z1Rxd4RBn3sIekzcsJmJ4UFrW3VpajtkaI-oiBH9SsQcMmmt4Np-tEA1zbXjVthzfo"
    var okHttpClient: OkHttpClient = OkHttpClient()

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
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        TODO("Not yet implemented")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        TODO("Not yet implemented")
                    }
                })
            }
    }
}