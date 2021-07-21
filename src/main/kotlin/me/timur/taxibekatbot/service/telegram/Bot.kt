package me.timur.taxibekatbot.service.telegram

import ch.qos.logback.classic.Logger
import me.timur.taxibekatbot.exception.InvalidInputException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove

@Component
class Bot: TelegramLongPollingBot(){

    @Value("\${bot.username}")
    lateinit var tgBotName: String;

    @Value("\${bot.token}")
    lateinit var tgBotToken: String;

    @Autowired
    private var updateHandler: UpdateHandler? = null

    override fun getBotToken(): String = tgBotToken

    override fun getBotUsername(): String = tgBotName

    override fun onUpdateReceived(update: Update) {
        val chatId = if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()
        val messageId = if (update.hasMessage()) update.message.messageId else update.callbackQuery.message.messageId

        println("hello")

        try {
            val messages = updateHandler!!.handle(update)

            for (message in messages)
                execute(message)

            val deleteMsg = DeleteMessage(chatId, messageId)
            execute(deleteMsg)
        }
        catch (e: InvalidInputException){
            execute(SendMessage(chatId, e.message ?: "Kutilmagn xatolik"))
        }
        catch (e: Exception) {
            logger.error(e.stackTraceToString())
            System.err.println(e)
        }

    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Bot::class.java) as Logger
    }
}