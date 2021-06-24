package me.timur.taxibekatbot.service.telegram

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove

@Component
class Bot: TelegramLongPollingBot(){

    @Autowired
    private var updateHandler: UpdateHandler? = null

    override fun getBotToken(): String = "1701771559:AAHHX0t5na8uODKgSqyRjNva8EwvFPWiPTE"

    override fun getBotUsername(): String = "TaxiBekatTestBot"

    override fun onUpdateReceived(update: Update) {
        val messages = updateHandler!!.handle(update)

        for (message in messages) {
            execute(message)
        }

        val chatId = if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()
        val messageId = if (update.hasMessage()) update.message.messageId else update.callbackQuery.message.messageId

        //TODO move forward logic in UpdateHandler
        execute(ForwardMessage(UpdateHandler.TELEGRAM_CHANNEL, chatId, messageId))

        val deleteMsg = DeleteMessage(chatId, messageId)
        execute(deleteMsg)

    }
}