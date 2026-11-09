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

                            // بررسی تطابق برای شماره‌های معمولی و سرشماره‌های خدماتی کوتاه
                            if (isServiceOrNumberMatch(sender, savedSourceNumber)) {
                                forwardSms(targetNumber, "$messageBody")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun isServiceOrNumberMatch(sender: String, target: String): Boolean {
        // پاکسازی کاراکترهای اضافی
        val cleanSender = sender.replace("+98", "0").replace(Regex("[^0-9a-zA-Z]"), "").lowercase()
        val cleanTarget = target.replace("+98", "0").replace(Regex("[^0-9a-zA-Z]"), "").lowercase()

        // اگر سرشماره خدماتی یا کوتاه است (مثل 3000940)، بررسی می‌کنیم که آیا فرستنده شامل این شماره است یا برعکس
        if (cleanTarget.length <= 8 || cleanSender.length <= 8) {
            return cleanSender.contains(cleanTarget) || cleanTarget.contains(cleanSender)
        }

        // برای شماره‌های معمولی موبایل (9 رقم انتهای شماره)
        return if (cleanSender.length >= 9 && cleanTarget.length >= 9) {
            cleanSender.takeLast(9) == cleanTarget.takeLast(9)
        } else {
            cleanSender == cleanTarget
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