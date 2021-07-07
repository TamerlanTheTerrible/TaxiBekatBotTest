package me.timur.taxibekatbot.util

import me.timur.taxibekatbot.exception.InvalidInputException
import org.apache.commons.lang3.math.NumberUtils
import org.telegram.telegrambots.meta.api.objects.Update

object PhoneUtil {

    fun containsPhone(update: Update): Boolean {
        var containsPhone = false
        val message = update.message

        if (message.replyToMessage.text.contains("Telefon raqamingizni"))
            containsPhone = true
        else if (message.entities.size > 0 && message.entities.any { it.type == "phone_number" })
            containsPhone = true
        else if (message.text.length == 9 || message.text.length == 12){
            if (NumberUtils.isDigits(getPhone(update)))
                containsPhone = true
            else
                throw InvalidInputException("Noto'g'ri telefon raqami formati")
        }
        return containsPhone
    }

    fun getFormattedPhone(update: Update): String {
        return formatPhoneNumber(getPhone(update))
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.length == 9 && !phoneNumber.startsWith("+998"))
            "+998$phoneNumber"
        else if (phoneNumber.length == 12 && !phoneNumber.startsWith("+"))
            "+$phoneNumber"
        else phoneNumber
    }

    private fun getPhone(update: Update): String {
        val phone = update.message.contact.phoneNumber ?: update.message.text
        return (removeDeliminators(phone))
    }

    private fun removeDeliminators(phone: String): String{
        return phone
            .replace("-", "")
            .replace(" ", "")
            .replace(".", "")
    }

}