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
    private var regionRepository: RegionRepository? = null

    override fun getBotToken(): String = "1701771559:AAEMpiE3J7cg_O3Eq2TufpScSj4bEP4ZJ8g"

    override fun getBotUsername(): String = "TaxiBekatTestBot"

    override fun onUpdateReceived(update: Update) {

        val regionList = regionRepository!!.findAll();

        val keyBoardList = ArrayList<List<InlineKeyboardButton>>();
        var keyBoardRow = ArrayList<InlineKeyboardButton>()

        regionList.forEachIndexed { index, it ->
            keyBoardRow.add(InlineKeyboardButton(it.nameLatin!!).apply { callbackData = it.nameLatin })
            if (index % 2 == 1){
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            }
            else if( index == regionList.size-1)
                keyBoardList.add(keyBoardRow)

//            keyBoardList.add(
//                listOf(InlineKeyboardButton(it.nameLatin!!).apply { callbackData = it.nameLatin })
//            )
        }

        if (update.hasMessage()) {
            if (update.message.text == "/start"){
                val chatId = update.message.chatId
                val markup = InlineKeyboardMarkup().apply {
                    this.keyboard = listOf(
                        listOf(
                            InlineKeyboardButton("\uD83D\uDE96 Taksi izlash").apply { callbackData = "Taxi" },
                            InlineKeyboardButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️Yo'lovchi izlash").apply { callbackData = "Client" })
                    )
                }
                val responseText = "\uD83D\uDC47 Quyidagilardan birini tanlang"
                val replyMessage = SendMessage(chatId.toString(), responseText).apply { replyMarkup = markup }
                execute(replyMessage)
            }
        }

        if(update.hasCallbackQuery()){
            if(update.callbackQuery.data == "Taxi" || update.callbackQuery.data == "Client"){
                val chatId = update.callbackQuery.message.chatId
                val markup = InlineKeyboardMarkup().apply { this.keyboard = keyBoardList }
                val responseText = "Qayerdan"
                val replyMessage = SendMessage(chatId.toString(), responseText).apply { replyMarkup = markup }
                execute(replyMessage)
            }
        }

    }
}