package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.Region
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.entity.enum.AnnouncementType
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

    var announcementType: AnnouncementType? = null
    var from: SubRegion? = null
    var to: SubRegion? = null

    fun handle(update: Update): SendMessage{
        val regionList = regionRepository.findAll()
        val subRegionList = subRegionRepository.findAll()

        if (update.hasMessage())
            if (update.message.text == "/start")
                return commandStart(update)

        if (update.hasCallbackQuery()){
            val callbackData = update.callbackQuery.data

            if(callbackData.contains(PREFIX_TYPE))
                return callbackAnnouncementType(update, regionList)

            if (callbackData.contains(PREFIX_FROM_REGION))
                return callbackFromRegion(update, subRegionList)

            if (subRegionList.any { it.nameLatin == callbackData})
                return callbackSaveAnnouncement(update)
        }
        return sendMessage(update, "Kutilmagan xatolik")
    }

    private fun callbackFromRegion(update: Update, subRegionList: List<SubRegion>): SendMessage {
        val callbackData = update.callbackQuery.data
        val list = subRegionList.filter { it.region!!.nameLatin == callbackData.substringAfter(PREFIX_FROM_REGION) }

        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_FROM_SUB_REGION)
        return sendMessage(update, "Qaysi shahar/tumandan").apply { this.replyMarkup = replyMarkup }
    }

    private fun callbackAnnouncementType(update: Update, regionList: List<Region>): SendMessage {
        announcementType = AnnouncementType.findByName(update.callbackQuery.message.text.substringAfter(PREFIX_TYPE))

        val replyMarkup = createMarkupFromPlaceList(regionList, PREFIX_FROM_REGION)
        return sendMessage(update, "Qaysi viloyatdan").apply { this.replyMarkup = replyMarkup }
    }

    private fun callbackSaveAnnouncement(update: Update): SendMessage {
        TODO("Not yet implemented")
    }

    private fun commandStart(update: Update): SendMessage {
        val keyboard = listOf( listOf(
            InlineKeyboardButton("\uD83D\uDE96 Taksi izlash") .apply { callbackData = "${PREFIX_TYPE}TAXI" },
            InlineKeyboardButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️Yo'lovchi izlash").apply { callbackData = "${PREFIX_TYPE}CLIENT"}))

        val markup = InlineKeyboardMarkup().apply { this.keyboard = keyboard }
        val responseText = "\uD83D\uDC47 Quyidagilardan birini tanlang"

        return sendMessage(update, responseText).apply { replyMarkup = markup }
    }

    private fun sendMessage(update: Update, replyText: String): SendMessage{
        val id = if (update.hasMessage()) update.message.chatId.toString()
        else update.callbackQuery.message.chatId.toString()
        return SendMessage(id, replyText)
    }

    private fun createMarkupFromPlaceList(objectList: List<Any>, callbackDataPrefix: String? = null): InlineKeyboardMarkup{
        val keyBoardList = ArrayList<List<InlineKeyboardButton>>()
        var keyBoardRow = ArrayList<InlineKeyboardButton>()
        val keyboardTextField = "nameLatin"

        objectList.forEachIndexed { index, it ->
            val fieldValue = InvokeGetter.invokeGetter(it, keyboardTextField).toString()
            val cbData = if (callbackDataPrefix == null) fieldValue else "${callbackDataPrefix}${fieldValue}"
            keyBoardRow.add(InlineKeyboardButton(fieldValue).apply { callbackData = cbData })
            if (index % 2 == 1) {
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            } else if (index == objectList.size - 1)
                keyBoardList.add(keyBoardRow)
        }

        return InlineKeyboardMarkup().apply { this.keyboard = keyBoardList }
    }

    companion object {
        const val PREFIX_TYPE = "Type_"
        const val PREFIX_FROM_SUB_REGION = "FromSubRegion_"
        const val PREFIX_TO_SUB_REGION = "ToSubRegion_"
        const val PREFIX_FROM_REGION = "FromRegion_"
        const val PREFIX_TO_REGION = "ToRegion_"
    }

}