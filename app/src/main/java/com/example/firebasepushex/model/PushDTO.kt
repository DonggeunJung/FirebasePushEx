package com.example.firebasepushex.model

data class PushDTO(var to: String? = null, var notification: Notification? = null) {
    data class Notification(var title: String? = null, var body: String? = null)
}