package com.fluttycat.permissionfit

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    private lateinit var mGoogleApiClient: GoogleApiClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        googleSignInPermission()

        val unsubFit = findViewById<TextView>(R.id.unsubFit)
        unsubFit.setOnClickListener {
            unsubscribeFitClient()
        }
    }

    private fun googleSignInPermission() {

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                account,
                fitnessOptions
            )
        } else {
            accessGoogleFit()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessGoogleFit() {
        val end = LocalDateTime.now()
        val start = end.minusYears(1)
        val endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond()
        val startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond()

        checkActivityRecognitionPermission()
        recordingFitClient()
    }

    private fun checkActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION
            )
        }
    }

    private fun recordingFitClient() {
        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                Log.i("TAG", "Successfully subscribed!")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "There was a problem subscribing.", e)
            }

    }

    private fun getFitHistoryClient(startSeconds: Long, endSeconds: Long) {
        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .build()
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)


        Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                // Use response data here
                Log.i("TAG", "OnSuccess()${response}")
            }
            .addOnFailureListener({ e -> Log.d("TAG", "OnFailure()", e) })
    }

    private fun unsubscribeFitClient() {
        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .unsubscribe(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                Log.i("UNSUBSCRIBE", "Successfully unsubscribed.")
                disconnectFromGoogleFit()
            }
            .addOnFailureListener { e ->
                Log.w("UNSUBSCRIBE", "Failed to unsubscribe.")
            }
    }

    private fun disconnectFromGoogleFit() {
        Fitness.getConfigClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .disableFit()
            .addOnSuccessListener {
                Log.i("DISCONNECT", "Disabled Google Fit")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    this.revokeSelfPermissionOnKill(android.Manifest.permission.ACTIVITY_RECOGNITION)
                }

                signOutGoogleAccount()

            }
            .addOnFailureListener { e ->
                Log.w("DISCONNECT", "There was an error disabling Google Fit", e)
            }

    }

    private fun signOutGoogleAccount() {

        val gso: GoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .addExtension(fitnessOptions)
            .requestEmail()
            .build()

        /* mGoogleApiClient = GoogleApiClient.Builder(this)
             .addApi<GoogleSignInOptions>(Auth.GOOGLE_SIGN_IN_API, options)
             .build()*/

        val mGoogleSignInClient = GoogleSignIn.getClient(this, options)
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        mGoogleSignInClient.revokeAccess()
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    Toast.makeText(this, "access revoked.", Toast.LENGTH_LONG).show()
                }else{
                    Log.i("REVOKE", "${it.exception}")
                }
            }



        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                Toast.makeText(this, "signed out.", Toast.LENGTH_LONG).show()
            }


        /*Auth.GoogleSignInApi.signOut(mGoogleApiClient)
            .setResultCallback {
                Toast.makeText(this, "signed out. status : $it", Toast.LENGTH_LONG).show()
            }*/
    }

    private fun revokeGoogleAccountAccess(context: Context) {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.fluttycat.permissionfit")

        for (account in accounts) {
            accountManager.clearPassword(account)
            accountManager.removeAccount(account, null, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> accessGoogleFit()
            else -> {
                // Result wasn't from Google Fit
            }
        }

    }

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1
        const val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 10
    }
}