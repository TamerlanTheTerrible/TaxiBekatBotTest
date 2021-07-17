package me.timur.taxibekatbot.util

import me.timur.taxibekatbot.exception.InvalidInputException
import org.apache.commons.lang3.math.NumberUtils
import org.telegram.telegrambots.meta.api.objects.Update

object PhoneUtil {

    fun formatPhoneNumber(phone: String): String {
        val internationalCode = phone.substring(0, 4)
        val operatorCode = phone.substring(4, 6)
        val localNumber = "${phone.substring(6, 9)}-${phone.substring(9, 11)}-${phone.substring(11, 13)}"
        return "($internationalCode) $operatorCode $localNumber"
    }

    fun containsPhone(update: Update): Boolean {
        val message = update.message

        return if (message.replyToMessage !=null && message.replyToMessage.text.contains("Telefon raqamingizni"))
            true
        else if (message.entities != null && message.entities.size > 0 && message.entities.any { it.type == "phone_number" })
            true
        else {
            val phone = getPhone(update)
            if (NumberUtils.isDigits(phone) && phone.length == 9 && phone.startsWith("998"))
                true
            else if (NumberUtils.isDigits(phone) && phone.length == 12 && phone.startsWith("9"))
                true
            else
                throw InvalidInputException("Noto'g'ri telefon raqami formati")
        }
    }

    fun getFullPhoneNumber(update: Update): String {
        return generateFullPhoneNumber(getPhone(update))
    }

    private fun generateFullPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.length == 9 && !phoneNumber.startsWith("+998"))
            "+998$phoneNumber"
        else if (phoneNumber.length == 12 && !phoneNumber.startsWith("+"))
            "+$phoneNumber"
        else phoneNumber
    }

    private fun getPhone(update: Update): String {
        val phone = if (update.message.contact != null)
            update.message.contact.phoneNumber
        else
            update.message.text

        return (removeDeliminators(phone))
    }

    private fun removeDeliminators(phone: String): String{
        return phone
            .replace("-", "")
            .replace(" ", "")
            .replace(".", "")
            .replace("+", "")
    }

}