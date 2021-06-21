package me.timur.taxibekatbot.service.telegram

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class Bot: TelegramLongPollingBot(){

    @Autowired
    private var updateHandler: UpdateHandler? = null

    override fun getBotToken(): String = "1701771559:AAEMpiE3J7cg_O3Eq2TufpScSj4bEP4ZJ8g"

    override fun getBotUsername(): String = "TaxiBekatTestBot"

    override fun onUpdateReceived(update: Update) {
        execute(updateHandler!!.handle(update))

        val chatId = if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()
        val messageId = if (update.hasMessage()) update.message.messageId else update.callbackQuery.message.messageId
        val deleteMsg = DeleteMessage(chatId, messageId)
        execute(deleteMsg)
    }
}