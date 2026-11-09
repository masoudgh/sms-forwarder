package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val prefs = context.getSharedPreferences("SmsConfig", Context.MODE_PRIVATE)
            val sourceNumber = prefs.getString("source_number", "") ?: ""
            val targetNumber = prefs.getString("target_number", "") ?: ""
            val isEnabled = prefs.getBoolean("is_enabled", false)

            if (!isEnabled || sourceNumber.isEmpty() || targetNumber.isEmpty()) {
                return
            }

            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>?
                if (pdus != null) {
                    for (pdu in pdus) {
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = sms.originatingAddress ?: ""
                        val messageBody = sms.messageBody

                        if (sender.endsWith(sourceNumber.takeLast(10))) {
                            forwardSms(targetNumber, "از طرف: $sender\n$messageBody")
                        }
                    }
                }
            }
        }
    }

    private fun forwardSms(targetNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(targetNumber, null, parts, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
