package com.example.smsforwarder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etSource = findViewById<EditText>(R.id.etSourceNumber)
        val etTarget = findViewById<EditText>(R.id.etTargetNumber)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val switchEnable = findViewById<Switch>(R.id.switchEnable)

        val prefs = getSharedPreferences("SmsConfig", Context.MODE_PRIVATE)

        etSource.setText(prefs.getString("source_number", ""))
        etTarget.setText(prefs.getString("target_number", ""))
        switchEnable.isChecked = prefs.getBoolean("is_enabled", false)

        requestPermissions()

        btnSave.setOnClickListener {
            val source = etSource.text.toString().trim()
            val target = etTarget.text.toString().trim()

            if (source.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "لطفا هر دو شماره را وارد کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().apply {
                putString("source_number", source)
                putString("target_number", target)
                putBoolean("is_enabled", switchEnable.isChecked)
                apply()
            }

            Toast.makeText(this, "تنظیمات با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )

        val neededPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }
}
