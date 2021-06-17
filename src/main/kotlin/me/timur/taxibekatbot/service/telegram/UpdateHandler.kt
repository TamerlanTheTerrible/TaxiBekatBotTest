package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.repository.RegionRepository
import me.timur.taxibekatbot.repository.SubRegionRepository
import me.timur.taxibekatbot.service.AnnouncementService
import me.timur.taxibekatbot.util.InvokeGetter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class UpdateHandler
@Autowired constructor(
    private val regionRepository: RegionRepository,
    private val subRegionRepository: SubRegionRepository,
    private val announcementService: AnnouncementService
){


    fun handle(update: Update): SendMessage{
        val regionList = regionRepository.findAll()

        if (update.hasMessage()) {
            if (update.message.text == "/start"){
                return commandStart(update)
            }
        }
        if (update.hasCallbackQuery()){
            val callbackData = update.callbackQuery.data

            if(callbackData == "Taxi" || callbackData == "Client"){
                return callbackPlace(update, regionList)
            }
            else if (regionList.any { it.nameLatin == callbackData}){
                return callbackPlace(update, subRegionRepository.findAllByRegionNameLatin(callbackData))
            }
        }
        return SendMessage()
    }

    private fun callbackPlace(update: Update, list: List<Any>): SendMessage {
        val replyMarkup = createMarkupFromList(list, "nameLatin")
        val chatId = update.callbackQuery.message.chatId
        return SendMessage(chatId.toString(), "Qayerdan").apply { this.replyMarkup = replyMarkup }
    }

    private fun commandStart(update: Update): SendMessage {
        val chatId = update.message.chatId
        val markup = InlineKeyboardMarkup().apply {
            this.keyboard = listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDE96 Taksi izlash").apply { callbackData = "Taxi" },
                    InlineKeyboardButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️Yo'lovchi izlash").apply {
                        callbackData = "Client"}))
        }
        val responseText = "\uD83D\uDC47 Quyidagilardan birini tanlang"

        return SendMessage(chatId.toString(), responseText).apply { replyMarkup = markup }
    }

    private fun createMarkupFromList(objectList: List<Any>, keyboardTextField: String): InlineKeyboardMarkup{
        val keyBoardList = ArrayList<List<InlineKeyboardButton>>();
        var keyBoardRow = ArrayList<InlineKeyboardButton>()

        objectList.forEachIndexed { index, it ->
            val fieldValue = InvokeGetter.invokeGetter(it, keyboardTextField).toString()
            keyBoardRow.add(InlineKeyboardButton(fieldValue).apply { callbackData = fieldValue })
            if (index % 2 == 1) {
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            } else if (index == objectList.size - 1)
                keyBoardList.add(keyBoardRow)
        }

        return InlineKeyboardMarkup().apply { this.keyboard = keyBoardList }
    }

}