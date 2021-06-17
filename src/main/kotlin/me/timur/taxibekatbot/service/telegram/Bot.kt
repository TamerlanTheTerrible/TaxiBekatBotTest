package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.repository.RegionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class Bot: TelegramLongPollingBot(){

    @Autowired
    private var updateHandler: UpdateHandler? = null

    override fun getBotToken(): String = "1701771559:AAEMpiE3J7cg_O3Eq2TufpScSj4bEP4ZJ8g"

    override fun getBotUsername(): String = "TaxiBekatTestBot"

    override fun onUpdateReceived(update: Update) {
        execute(updateHandler!!.handle(update))
    }
}