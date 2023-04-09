package com.example.firebasepushex.view

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.firebasepushex.Constant
import com.example.firebasepushex.Constant.DB_PROFILES
import com.example.firebasepushex.R
import com.example.firebasepushex.model.Profile
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LogInActivity : BaseActivity() {
    val REQUEST_CODE_GOOGLE_LOGIN = 1002
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    fun onBtnLoginEmail(v: View) {
        val email: String = etEmail.text.toString()
        val password: String = etPassword.text.toString()
        if(email.isNullOrEmpty() || password.isNullOrEmpty()) {
            Toast.makeText(this, "Input valid account.", Toast.LENGTH_SHORT).show()
            return
        }
        createAndLoginEmail(email, password)
    }

    private fun createAndLoginEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    Toast.makeText(this, "Creating account is succeeded.", Toast.LENGTH_SHORT).show()
                    moveProfilePage(auth.currentUser)
                } else if(task.exception?.message.isNullOrEmpty()) {
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                } else {
                    signinEmail(email, password)
                }
            }
    }

    private fun signinEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    Toast.makeText(this, "Email LogIn succeeded.", Toast.LENGTH_SHORT).show()
                    checkProfileExist()
                } else {
                    Toast.makeText(this, "Email LogIn failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun onBtnLoginGoogle(v: View) {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_LOGIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_GOOGLE_LOGIN && data != null) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result?.isSuccess == true) {
                val credential = GoogleAuthProvider.getCredential(result.signInAccount?.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        Toast.makeText(this, "Google LogIn succeeded.", Toast.LENGTH_SHORT).show()
                        checkProfileExist()
                    }
                }
            }
        }
    }

    private fun checkProfileExist() {
        auth.currentUser?.uid?.let {
            // Read Profile by Pull Driven style
            db.collection(DB_PROFILES).document(it)
                .get().addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        val profile = Profile.makeInstance(task.result)
                        profile?.email?.let {
                            moveMainPage()
                            finish()
                            return@addOnCompleteListener
                        }
                    }
                    moveProfilePage(auth.currentUser)
                }
        }
    }

    private fun moveProfilePage(user: FirebaseUser?) {
        user?.let {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(Constant.FIELD_UID, user.uid)
            startActivity(intent)
        }
    }

    private fun moveMainPage() {
        startActivity(Intent(this, MainActivity::class.java))
    }

}