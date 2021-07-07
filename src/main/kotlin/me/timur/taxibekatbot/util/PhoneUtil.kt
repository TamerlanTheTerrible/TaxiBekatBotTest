package me.timur.taxibekatbot.util

import me.timur.taxibekatbot.exception.InvalidInputException
import org.apache.commons.lang3.math.NumberUtils
import org.telegram.telegrambots.meta.api.objects.Update

object PhoneUtil {

    fun containsPhone(update: Update): Boolean {
        var containsPhone: Boolean = false
        val message = update.message

        if (message.replyToMessage.text.contains("Telefon raqamingizni"))
            containsPhone = true
        else if (message.entities.size > 0 && message.entities.any { it.type == "phone_number" })
            containsPhone = true
        else if (message.text.length == 9){
            if (NumberUtils.isDigits(removeDeliminators(getPhone(update))))
                containsPhone = true
            else
                throw InvalidInputException("Noto'g'ri telefon raqami formati")
        }
        return containsPhone
    }

    private fun removeDeliminators(phone: String): String{
        return phone
            .replace("-", "")
            .replace(" ", "")
            .replace(".", "")
    }

    private fun getPhone(update: Update) = update.message.contact.phoneNumber ?: update.message.text
}