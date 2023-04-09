package com.example.firebasepushex

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firebasepushex.Constant.DB_PUSH_TOKENS
import com.example.firebasepushex.Constant.FIELD_PUSH_TOKEN
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity(), BaseAdapter.ItemEvent {
    private lateinit var rvProfile: RecyclerView
    private lateinit var adapter: BaseAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvProfile = findViewById(R.id.rvProfile)
        adapter = BaseAdapter.makeInstance(rvProfile, R.layout.item_profile, this)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        updatePushToken()
        initRecyclerView()
    }

    private fun updatePushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if(!task.isSuccessful || auth.currentUser?.uid == null)
                return@addOnCompleteListener
            val pushToken = task.result
            val sp = getSharedPreferences(DB_PUSH_TOKENS, MODE_PRIVATE)
            val prevToken = sp.getString(auth.currentUser?.uid, "")
            if(pushToken == prevToken)
                return@addOnCompleteListener

            val map = mapOf<String,Any>(FIELD_PUSH_TOKEN to pushToken)
            db.collection(DB_PUSH_TOKENS).document(auth.currentUser?.uid!!)
                .set(map)
                .addOnSuccessListener {
                    val edit = sp.edit()
                    edit.putString(auth.currentUser?.uid, pushToken)
                    edit.commit()
                }
        }
    }

    private fun initRecyclerView() {
        // Read Profiles by Push Driven style
        db.collection(Constant.DB_PROFILES).addSnapshotListener { value, error ->
            value?.let { snapshots ->
                val list = mutableListOf<Profile>()
                for(shot in snapshots) {
                    Profile.makeInstance(shot.data)?.let { profile ->
                        profile.uid = shot.id
                        if(profile.email == auth.currentUser?.email) {
                            list.add(0, profile)
                        } else {
                            list.add(profile)
                        }
                    }
                }
                if(!list.isNullOrEmpty()) {
                    adapter.setList(list)
                }
            }
        }
    }

    fun getData(index: Int) = adapter.getData(index) as Profile

    // BaseAdapter event methods - start
    override fun onBindViewHolder(v: View, index: Int, data: Any) {
        val profile = data as Profile
        (v.findViewById(R.id.tvEmail) as TextView).text = profile.email
        (v.findViewById(R.id.tvName) as TextView).text = profile.name
        (v.findViewById(R.id.tvComment) as TextView).text = profile.comment
        profile.imageUrl?.let { imageUrl ->
            val ivProfile = v.findViewById(R.id.ivProfile) as ImageView
            Glide.with(this).load(imageUrl).into(ivProfile)
        }
    }

    override fun onClickItem(index: Int) {
        val profile = getData(index)
        val intent = ProfileActivity.makeIntent(this, profile)
        startActivity(intent)
    }
    // BaseAdapter event methods - end

}