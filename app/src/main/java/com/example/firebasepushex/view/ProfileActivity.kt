package com.example.firebasepushex.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.firebasepushex.Constant
import com.example.firebasepushex.Constant.DOC_PROFILE
import com.example.firebasepushex.Constant.FIELD_UID
import com.example.firebasepushex.FcmPush
import com.example.firebasepushex.R
import com.example.firebasepushex.model.Profile
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : BaseActivity() {
    val REQUEST_CODE_PICK_PHOTO = 1001
    private lateinit var ivProfile: ImageView
    private lateinit var tvEmail: TextView
    private lateinit var etName: EditText
    private lateinit var etComment: EditText
    private lateinit var btnRun: Button
    private lateinit var storage: FirebaseStorage
    private lateinit var thisProfile: Profile
    private var mode: Int = 0
    private var fcmPush = FcmPush()

    @RequiresApi(33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        ivProfile = findViewById(R.id.ivProfile)
        tvEmail = findViewById(R.id.tvEmail)
        etName = findViewById(R.id.etName)
        etComment = findViewById(R.id.etComment)
        btnRun = findViewById(R.id.btnRun)

        storage = FirebaseStorage.getInstance()

        determineMode()

        ivProfile.setOnClickListener {
            if(mode == 2) return@setOnClickListener
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(pickIntent, REQUEST_CODE_PICK_PHOTO)
        }
    }

    private fun determineMode() {
        val thisUid = intent.getStringExtra(FIELD_UID)
        if(thisUid.isNullOrEmpty()) return
        db.collection(Constant.DB_PROFILES).document(thisUid)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Profile.makeInstance(task.result)?.let { profile ->
                        thisProfile = profile
                        if(thisUid == auth.currentUser?.uid) {
                            setModeUpdate()
                        } else {
                            setModeSendPush()
                        }
                    }
                }
                if(!this::thisProfile.isInitialized) {
                    thisProfile = Profile(uid = thisUid, email = auth.currentUser?.email)
                    setModeCreate()
                }
                showProfile(thisProfile)
            }
    }

    private fun showProfile(profile: Profile) {
        profile.email?.let { tvEmail.text = it }
        profile.name?.let { etName.setText(it) }
        profile.comment?.let { etComment.setText(it) }
        profile.imageUrl?.let { Glide.with(this).load(it).into(ivProfile) }
    }

    private fun setModeCreate() {
        mode = 0
        editTextEditable(true)
        btnRun.text = "Create profile"
    }

    private fun setModeUpdate() {
        mode = 1
        editTextEditable(true)
        btnRun.text = "Update profile"
    }

    private fun setModeSendPush() {
        mode = 2
        editTextEditable(false)
        btnRun.text = "Send Push Notification"
    }

    private fun editTextEditable(editable: Boolean) {
        val type = if(editable) InputType.TYPE_TEXT_FLAG_MULTI_LINE else InputType.TYPE_NULL
        etName.inputType = type
        etComment.inputType = type
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_PICK_PHOTO && resultCode == Activity.RESULT_OK
            && data?.data != null && auth.currentUser?.uid != null) {
            ivProfile.setImageURI(data.data)
            val uid = auth.currentUser!!.uid
            val storageRef = storage.reference.child(uid).child("$DOC_PROFILE.jpg")
            storageRef.putFile(data.data!!).addOnSuccessListener { task ->
                Toast.makeText(this, "Profile image is uploaded.", Toast.LENGTH_SHORT).show()
                storageRef.downloadUrl.addOnSuccessListener {
                    thisProfile.imageUrl = it.toString()
                }
            }
        }
    }

    fun onClick(v: View) {
        if(mode == 2) {
            sendPushNotification()
        } else {
            saveProfile()
        }
    }

    private fun sendPushNotification() {
        thisProfile.uid?.let { targetUid ->
            fcmPush.sendMessage(targetUid, "Firebase Push Ex", "Test push notification.")
            Toast.makeText(this, "Push notification was sent.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfile() {
        auth.currentUser?.let { user ->
            thisProfile.name = etName.text.toString()
            thisProfile.comment = etComment.text.toString()
            db.collection(Constant.DB_PROFILES).document(user.uid)
                .set(thisProfile)
                .addOnSuccessListener {
                    val strMode = if(mode == 0) "created" else "updated"
                    Toast.makeText(this, "User profile was $strMode.", Toast.LENGTH_SHORT).show()
                    if(mode == 0) {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    val strMode = if(mode == 0) "creating" else "updating"
                    Toast.makeText(this, "Failed $strMode profile.", Toast.LENGTH_SHORT).show()
                }
        }
    }

}