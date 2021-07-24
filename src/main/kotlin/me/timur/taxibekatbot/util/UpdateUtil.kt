package me.timur.taxibekatbot.util

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard

object UpdateUtil {

    fun sendMessage(update: Update, replyText: String, markup: ReplyKeyboard? = null): SendMessage{
        val chatId = getChatId(update)
        return sendMessage(chatId, replyText, markup)
    }

    fun sendMessage(chatId: String, replyText: String, markup: ReplyKeyboard? = null): SendMessage =
            SendMessage(chatId, replyText).apply { this.replyMarkup = markup }

    fun getChatId(update: Update) =
            if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()
}

fun Update.getChatId() =
    if (this.hasMessage())
        this.message.chatId.toString()
    else
        this.callbackQuery.message.chatId.toString()

fun Update.getStringAfter(afterString: String) =
    this.callbackQuery.data.substringAfter(afterString)

fun Update.getStringBefore(beforeString: String) =
    this.callbackQuery.data.substringBefore(beforeString)

fun Update.getStringBetween(afterString: String, beforeString: String) =
    this.callbackQuery.data.substringAfter(afterString).substringBefore(beforeString)

