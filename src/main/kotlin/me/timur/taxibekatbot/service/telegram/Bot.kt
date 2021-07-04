package me.timur.taxibekatbot.service.telegram

import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import java.io.FileDescriptor.err
import java.lang.System.err
import java.time.format.DateTimeFormatter

@Component
class Bot: TelegramLongPollingBot(){

    @Autowired
    private var updateHandler: UpdateHandler? = null

    override fun getBotToken(): String = "1701771559:AAHHX0t5na8uODKgSqyRjNva8EwvFPWiPTE"

    override fun getBotUsername(): String = "TaxiBekatTestBot"

    override fun onUpdateReceived(update: Update) {
        try {
            val messages = updateHandler!!.handle(update)

            for (message in messages) {
                execute(message)
            }

            val chatId = if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()
            val messageId = if (update.hasMessage()) update.message.messageId else update.callbackQuery.message.messageId

            val deleteMsg = DeleteMessage(chatId, messageId)
            execute(deleteMsg)
        } catch (e: Exception) {
            logger.error((e.message ?: e).toString())
            System.err.println(e)
        }
    }


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Bot::class.java) as Logger
    }
}