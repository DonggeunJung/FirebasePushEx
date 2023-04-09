package com.example.firebasepushex

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.firebasepushex.Constant.DOC_PROFILE
import com.example.firebasepushex.Constant.FIELD_COMMENT
import com.example.firebasepushex.Constant.FIELD_EMAIL
import com.example.firebasepushex.Constant.FIELD_IMAGE_URL
import com.example.firebasepushex.Constant.FIELD_NAME
import com.example.firebasepushex.Constant.FIELD_UID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {
    private val REQUEST_CODE_PICK_PHOTO = 1001

    private lateinit var ivProfile: ImageView
    private lateinit var tvEmail: TextView
    private lateinit var etName: EditText
    private lateinit var etComment: EditText
    private lateinit var btnRun: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUrl: String? = null
    private var mode: Int = 0
    var fcmPush = FcmPush()

    @RequiresApi(33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        ivProfile = findViewById(R.id.ivProfile)
        tvEmail = findViewById(R.id.tvEmail)
        etName = findViewById(R.id.etName)
        etComment = findViewById(R.id.etComment)
        btnRun = findViewById(R.id.btnRun)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        determineMode()

        ivProfile.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(pickIntent, REQUEST_CODE_PICK_PHOTO)
        }
    }

    private fun determineMode() {
        val email = intent.getStringExtra(FIELD_EMAIL)
        tvEmail.text = email
        val name = intent.getStringExtra(FIELD_NAME)
        if(name == null) {
            setModeCreate()
        } else {
            etName.setText(name)
            val comment = intent.getStringExtra(FIELD_COMMENT)
            etComment.setText(comment)
            imageUrl = intent.getStringExtra(FIELD_IMAGE_URL)
            if(!imageUrl.isNullOrEmpty()) {
                Glide.with(this).load(imageUrl).into(ivProfile)
            }
            if(email == auth.currentUser?.email) {
                setModeUpdate()
            } else {
                setModeSendPush()
            }
        }
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
        if(requestCode == REQUEST_CODE_PICK_PHOTO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                ivProfile.setImageURI(uri)
                auth.currentUser?.uid?.let { uid ->
                    val storageRef = storage.reference.child(uid).child("$DOC_PROFILE.jpg")
                    storageRef.putFile(uri).addOnSuccessListener { task ->
                        Toast.makeText(this, "Profile image is uploaded.", Toast.LENGTH_SHORT).show()
                        storageRef.downloadUrl.addOnSuccessListener {
                            imageUrl = it.toString()
                        }
                    }
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
        val targetUid = intent.getStringExtra(FIELD_UID)
        targetUid?.let {
            fcmPush.sendMessage(it, "Firebase Push Ex", "Test push notification.")
            Toast.makeText(this, "Push notification was sent.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfile() {
        auth.currentUser?.let {
            val email = it.email ?: ""
            val name = etName.text.toString()
            val comment = etComment.text.toString()
            val profile = Profile(imageUrl, email, name, comment)
            db.collection(Constant.DB_PROFILES).document(it.uid)
                .set(profile)
                .addOnSuccessListener {
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

    companion object {
        fun makeIntent(context: Context, profile: Profile): Intent {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra(FIELD_EMAIL, profile.email)
            intent.putExtra(FIELD_NAME, profile.name)
            intent.putExtra(FIELD_COMMENT, profile.comment)
            intent.putExtra(FIELD_IMAGE_URL, profile.imageUrl)
            intent.putExtra(FIELD_UID, profile.uid)
            return intent
        }
    }

}