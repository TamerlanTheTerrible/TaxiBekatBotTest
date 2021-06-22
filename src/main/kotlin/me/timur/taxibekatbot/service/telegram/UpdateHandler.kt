package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.Announcement
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.entity.enum.AnnouncementType
import me.timur.taxibekatbot.repository.RegionRepository
import me.timur.taxibekatbot.repository.SubRegionRepository
import me.timur.taxibekatbot.service.AnnouncementService
import me.timur.taxibekatbot.util.InvokeGetter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.time.LocalDate

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

        if (update.hasMessage()){
            if (update.message.text == "/start")
                return commandStart(update)

            if (update.message.replyToMessage.text == "Telefon raqamingizni ulashing")
                return reviewAnnouncement(update)
        }

        if (update.hasCallbackQuery()){
            val callbackData = update.callbackQuery.data

            if(callbackData.contains(PREFIX_TYPE))
                return callbackType(update)

            if (callbackData.contains(PREFIX_FROM_REGION))
                return callbackFromRegion(update)

            if (callbackData.contains(PREFIX_FROM_SUB_REGION))
                return callbackFromSubRegion(update)

            if (callbackData.contains(PREFIX_TO_REGION))
                return callbackToRegion(update)

            if (callbackData.contains(PREFIX_TO_SUB_REGION)){
                return callbackToSubRegion(update)
            }
        }

        return sendMessage(update, "Kutilmagan xatolik")
    }

    private fun reviewAnnouncement(update: Update): SendMessage {
        //TODO("save phone number in TelegramUser")
        //TODO("replace LocalDate.now() to a date, chosen by a user")
        val replyText = "E'lon: " +
                "\n \uD83C\uDF06 Yo'nalish: ${from?.nameLatin} - ${to?.nameLatin} " +
                "\n \uD83D\uDCC5 Sana: ${LocalDate.now()}"

        return sendMessage(update, replyText).apply { this.replyMarkup = ReplyKeyboardRemove(true) }
    }

    private fun callbackToSubRegion(update: Update): SendMessage {
        val name = update.callbackQuery.data.substringAfter(PREFIX_TO_SUB_REGION)
        to = subRegionRepository.findByNameLatin(name)

        //TODO move contact request logic to the next request
        //TODO request a trip date
        val replyText = "Telefon raqamingizni ulashing"
        val keyboard = KeyboardButton().apply {
            this.text = "Tel raqamni yuborish"
            this.requestContact = true
        }

        val keyboardRow = KeyboardRow().apply { add(keyboard) }
        val markup = ReplyKeyboardMarkup().apply { this.keyboard = listOf(keyboardRow) }

        return sendMessage(update, replyText).apply { this.replyMarkup = markup }
    }

    private fun callbackToRegion(update: Update): SendMessage {
        val name = update.callbackQuery.data.substringAfter(PREFIX_TO_REGION)
        val list = subRegionRepository.findAllByRegionNameLatin(name)
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_TO_SUB_REGION)

        return sendMessage(update, "Qaysi shahar/tumanga").apply { this.replyMarkup = replyMarkup }
    }

    private fun callbackFromSubRegion(update: Update): SendMessage {
        val name = update.callbackQuery.data.substringAfter(PREFIX_FROM_SUB_REGION)
        from = subRegionRepository.findByNameLatin(name)

        val list = regionRepository.findAll()
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_TO_REGION)

        return sendMessage(update, "Qaysi viloyatga").apply { this.replyMarkup = replyMarkup }
    }

    private fun callbackType(update: Update): SendMessage {
        announcementType = AnnouncementType.findByName(update.callbackQuery.data.substringAfter(PREFIX_TYPE))

        val list = regionRepository.findAll()
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_FROM_REGION)

        return sendMessage(update, "Qaysi viloyatdan").apply { this.replyMarkup = replyMarkup }
    }

    private fun callbackFromRegion(update: Update): SendMessage {
        val name = update.callbackQuery.data.substringAfter(PREFIX_FROM_REGION)
        val list = subRegionRepository.findAllByRegionNameLatin(name)
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_FROM_SUB_REGION)

        return sendMessage(update, "Qaysi shahar/tumandan").apply { this.replyMarkup = replyMarkup }
    }

    private fun commandStart(update: Update): SendMessage {

        //TODO save contact if not saved

        val keyboard = listOf( listOf(
            InlineKeyboardButton("\uD83D\uDE96 Taksi izlash") .apply { callbackData = "${PREFIX_TYPE}TAXI" },
            InlineKeyboardButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️Yo'lovchi izlash").apply { callbackData = "${PREFIX_TYPE}CLIENT"}))

        val markup = InlineKeyboardMarkup().apply { this.keyboard = keyboard }
        val responseText = "\uD83D\uDC47 Quyidagilardan birini tanlang"

        return sendMessage(update, responseText).apply { replyMarkup = markup }
    }

    private fun sendMessage(update: Update, replyText: String): SendMessage{
        val chatId = if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()

        return SendMessage(chatId, replyText)
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