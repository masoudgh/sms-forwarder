package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val prefs = context.getSharedPreferences("SmsConfig", Context.MODE_PRIVATE)
            val savedSourceNumber = prefs.getString("source_number", "")?.trim() ?: ""
            val targetNumber = prefs.getString("target_number", "")?.trim() ?: ""
            val isEnabled = prefs.getBoolean("is_enabled", false)

            if (!isEnabled || savedSourceNumber.isEmpty() || targetNumber.isEmpty()) {
                return
            }

            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>?
                    if (pdus != null) {
                        for (pdu in pdus) {
                            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                            val sender = sms.originatingAddress ?: ""
                            val messageBody = sms.messageBody

                            // پاکسازی و مقایسه امن شماره‌ها (حذف فاصله‌ها و پلاس‌ها برای تطابق دقیق)
                            if (isNumberMatch(sender, savedSourceNumber)) {
                                forwardSms(targetNumber, "پیامک از: $sender\n\n$messageBody")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun isNumberMatch(sender: String, target: String): Boolean {
        // حذف کاراکترهای اضافی مثل + یا فاصله
        val cleanSender = sender.replace("+98", "0").replace(Regex("[^0-9]"), "")
        val cleanTarget = target.replace("+98", "0").replace(Regex("[^0-9]"), "")

        // تطابق 9 رقم انتهای شماره (برای پوشش اختلاف کدهای شهری و صفر اول)
        if (cleanSender.length >= 9 && cleanTarget.length >= 9) {
            return cleanSender.takeLast(9) == cleanTarget.takeLast(9)
        }
        return cleanSender == cleanTarget
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