package com.example.util

import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

object EmailSender {

    data class SendResult(val isSuccess: Boolean, val errorMessage: String? = null)

    fun sendSecurityAlert(
        senderEmail: String,
        appPassword: String,
        recipientEmail: String,
        subject: String,
        bodyText: String,
        imageFile: File? = null
    ): SendResult {
        if (senderEmail.isBlank() || appPassword.isBlank()) {
            return SendResult(false, "البريد الإلكتروني أو كلمة مرور التطبيق فارغة")
        }

        return try {
            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.protocols", "TLSv1.2")
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "15000")
            }

            val cleanPassword = appPassword.replace(" ", "").trim()

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail.trim(), cleanPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail.trim(), "نظام حماية الهاتف"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail.trim()))
                setSubject(subject, "UTF-8")

                val multipart: Multipart = MimeMultipart()

                val textPart = MimeBodyPart().apply {
                    setText(bodyText, "UTF-8", "plain")
                }
                multipart.addBodyPart(textPart)

                if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                    val attachmentPart = MimeBodyPart().apply {
                        val source = FileDataSource(imageFile)
                        dataHandler = DataHandler(source)
                        fileName = "intruder_snapshot.jpg"
                    }
                    multipart.addBodyPart(attachmentPart)
                }

                setContent(multipart)
            }

            Transport.send(message)
            SendResult(true)
        } catch (e: Exception) {
            e.printStackTrace()
            SendResult(false, e.localizedMessage ?: e.message ?: "فشل في إرسال البريد عبر الخادم")
        }
    }
}
