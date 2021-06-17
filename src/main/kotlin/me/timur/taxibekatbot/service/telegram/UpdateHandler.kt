package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.Region
import me.timur.taxibekatbot.repository.RegionRepository
import me.timur.taxibekatbot.repository.SubRegionRepository
import me.timur.taxibekatbot.service.AnnouncementService
import me.timur.taxibekatbot.util.InvokeGetter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class UpdateHandler
@Autowired constructor(
    private val regionRepository: RegionRepository,
    private val subRegionRepository: SubRegionRepository,
    private val announcementService: AnnouncementService
){


    fun handle(update: Update): SendMessage{

        if (update.hasMessage()) {
            if (update.message.text == "/start"){
                return commandStart(update)
            }
        }
        if (update.hasCallbackQuery()){
            if(update.callbackQuery.data == "Taxi" || update.callbackQuery.data == "Client"){
                return callbackPlace(update)
            }
        }
        return SendMessage()
    }

    private fun callbackPlace(update: Update): SendMessage {

        val regionList = regionRepository.findAll();
        val keyBoardList = ArrayList<List<InlineKeyboardButton>>();
        var keyBoardRow = ArrayList<InlineKeyboardButton>()

        val field =

        regionList.forEachIndexed { index, it ->
            keyBoardRow.add(InlineKeyboardButton(InvokeGetter.invokeGetter(it, "nameLatin").toString()).apply { callbackData = it.nameLatin })
            if (index % 2 == 1) {
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            } else if (index == regionList.size - 1)
                keyBoardList.add(keyBoardRow)
        }

        val chatId = update.callbackQuery.message.chatId
        val markup = InlineKeyboardMarkup().apply { this.keyboard = keyBoardList }
        val responseText = "Qayerdan"
        return SendMessage(chatId.toString(), responseText).apply { replyMarkup = markup }
    }

    private fun commandStart(update: Update): SendMessage {
        val chatId = update.message.chatId
        val markup = InlineKeyboardMarkup().apply {
            this.keyboard = listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDE96 Taksi izlash").apply { callbackData = "Taxi" },
                    InlineKeyboardButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️Yo'lovchi izlash").apply {
                        callbackData = "Client"
                    })
            )
        }
        val responseText = "\uD83D\uDC47 Quyidagilardan birini tanlang"
        return SendMessage(chatId.toString(), responseText).apply { replyMarkup = markup }
    }
}