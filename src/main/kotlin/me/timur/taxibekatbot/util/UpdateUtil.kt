package me.timur.taxibekatbot.util

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

object UpdateUtil {
    fun sendMessage(update: Update, replyText: String): SendMessage =
        SendMessage(update.getChatId(), replyText)
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

