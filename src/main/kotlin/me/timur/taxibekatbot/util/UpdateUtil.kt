package me.timur.taxibekatbot.util

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard

object UpdateUtil {

    fun sendMessage(update: Update, replyText: String, markup: ReplyKeyboard? = null): SendMessage{
        val chatId = getChatId(update)
        return sendMessage(chatId, replyText, markup)
    }

    fun sendMessage(chatId: String, replyText: String, markup: ReplyKeyboard? = null): SendMessage =
            SendMessage(chatId, replyText).apply { this.replyMarkup = markup }

    fun deleteMessage(update: Update): BotApiMethod<Message> {
        val chatId = update.callbackQuery.message.chatId.toString()
        val messageId = update.callbackQuery.message.messageId

        return DeleteMessage(chatId, messageId) as BotApiMethod<Message>
    }

    fun getChatId(update: Update) =
            if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()
}

fun Update.getMessageId() =
    if (this.hasMessage())
        this.message.messageId
    else
        this.callbackQuery.message.messageId

fun Update.getChatId() =
    if (this.hasMessage())
        this.message.chatId.toString()
    else
        this.callbackQuery.message.chatId.toString()

fun Update.getStringAfter(afterString: String): String {
    val str = this.getText()
    return str!!.substringAfter(afterString)
}

fun Update.getStringBefore(beforeString: String): String {
    val str = this.getText()
    return str!!.substringBefore(beforeString)
}

private fun Update.getText(): String? {
    return if (hasCallbackQuery())
        callbackQuery.data
    else
        message.text
}
