package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.Announcement
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.entity.enum.AnnouncementType
import me.timur.taxibekatbot.repository.RegionRepository
import me.timur.taxibekatbot.repository.SubRegionRepository
import me.timur.taxibekatbot.service.AnnouncementService
import me.timur.taxibekatbot.service.TelegramUserService
import me.timur.taxibekatbot.util.InvokeGetter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.Message
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
    private val announcementService: AnnouncementService,
    private val telegramUserService: TelegramUserService
){

    var announcementType: AnnouncementType? = null
    var from: SubRegion? = null
    var to: SubRegion? = null
    var date: LocalDate? = null
    var phone: String? = null
    var telegramUser: TelegramUser? = null

    fun handle(update: Update): List<BotApiMethod<Message>>{

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

            if (callbackData.contains(PREFIX_TO_SUB_REGION))
                return callbackToSubRegion(update)

            if (callbackData.contains(PREFIX_DATE))
                return callbackDate(update)

            if (callbackData.contains(SAVE_ANNOUNCEMENT))
                return saveAnnouncement(update)

            if (callbackData.contains(CHANGE))
                return commandStart(update)
        }

        return listOf(sendMessage(update, "Kutilmagan xatolik"))
    }

    private fun saveAnnouncement(update: Update): List<BotApiMethod<Message>> {
        val announcement = Announcement(announcementType, date, from, to, telegramUser)
        announcementService.save(announcement)

        val replyText = "#${announcement.id} raqamli e'lon joylashtirildi" +
                "\n $CHANNEL_LINK_TAXI_BEKAT_TEST"

        val chatId = getChatId(update)
        val sendMessage = SendMessage(chatId, replyText)

        val messageToForward = if (update.hasMessage()) update.message.messageId else update.callbackQuery.message.messageId
        val forwardMessage = ForwardMessage(CHANNEL_ID_TAXI_BEKAT_TEST, chatId, messageToForward)

        return listOf(sendMessage, forwardMessage)
    }

    private fun reviewAnnouncement(update: Update): List<SendMessage> {
        phone = update.message.contact.phoneNumber

        telegramUserService.savePhone(update)

        val replyText = "\n \uD83D\uDD0D Qidirilmoqda: $announcementType" +
                "\n \uD83D\uDCE2 E'lon: " +
                "\n \uD83C\uDF06 Yo'nalish: ${from?.nameLatin} - ${to?.nameLatin} " +
                "\n \uD83D\uDCC5 Sana: $date" +
                "\n \uD83D\uDCF1 Tel: $phone" +
                "\n" +
                "\n #${(from?.nameLatin)?.substringBefore(" ")}" +
                "${(to?.nameLatin)?.substringBefore(" ")}" +
                "$announcementType"

        val markup = InlineKeyboardMarkup().apply { this.keyboard = listOf(
            listOf(
                InlineKeyboardButton("E'lonni joylash").apply { callbackData = SAVE_ANNOUNCEMENT },
                InlineKeyboardButton("O'zgartirish").apply { callbackData = CHANGE }
            ))
        }

        return listOf(sendMessage(update, replyText).apply { this.replyMarkup = markup })
    }

    private fun callbackDate(update: Update): List<SendMessage> {
        val dateInString = update.callbackQuery.data.substringAfter(PREFIX_DATE)
        date = LocalDate.parse(dateInString)

        val replyText = "Telefon raqamingizni ulashing"
        val keyboard = KeyboardButton().apply {
            this.text = "Tel raqamni yuborish"
            this.requestContact = true
        }

        val keyboardRow = KeyboardRow().apply { add(keyboard) }
        val markup = ReplyKeyboardMarkup().apply { this.keyboard = listOf(keyboardRow) }

        return listOf(sendMessage(update, replyText).apply { this.replyMarkup = markup })
    }

    private fun callbackToSubRegion(update: Update): List<SendMessage> {
        val name = update.callbackQuery.data.substringAfter(PREFIX_TO_SUB_REGION)
        to = subRegionRepository.findByNameLatin(name)

        val keyBoardList = ArrayList<List<InlineKeyboardButton>>()
        var keyBoardRow = ArrayList<InlineKeyboardButton>()

        var now = LocalDate.now()
        var currentMonth = 0

        for (i in 0..9){

            if (now.month.value > currentMonth){
                currentMonth = now.month.value
                keyBoardRow.add(InlineKeyboardButton(now.month.name).apply { callbackData = PREFIX_TO_SUB_REGION })
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            }

            if (i % 7 == 1){
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            }

            keyBoardRow.add(InlineKeyboardButton(now.dayOfMonth.toString()).apply { callbackData = "$PREFIX_DATE$now"})

            now = now.plusDays(1)
        }

        keyBoardList.add(keyBoardRow)

        val replyText = "\uD83D\uDCC5 Sa'nani kiriting"
        val markup = InlineKeyboardMarkup().apply { keyboard = keyBoardList }

        return listOf(sendMessage(update, replyText).apply { replyMarkup = markup })

    }

    private fun callbackToRegion(update: Update): List<SendMessage> {
        val name = update.callbackQuery.data.substringAfter(PREFIX_TO_REGION)
        val list = subRegionRepository.findAllByRegionNameLatin(name)
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_TO_SUB_REGION)

        return listOf(sendMessage(update, "Qaysi shahar/tumanga").apply { this.replyMarkup = replyMarkup })
    }

    private fun callbackFromSubRegion(update: Update): List<SendMessage> {
        val name = update.callbackQuery.data.substringAfter(PREFIX_FROM_SUB_REGION)
        from = subRegionRepository.findByNameLatin(name)

        val list = regionRepository.findAll()
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_TO_REGION)

        return listOf(sendMessage(update, "Qaysi viloyatga").apply { this.replyMarkup = replyMarkup })
    }

    private fun callbackType(update: Update): List<SendMessage> {
        announcementType = AnnouncementType.findByName(update.callbackQuery.data.substringAfter(PREFIX_TYPE))

        val list = regionRepository.findAll()
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_FROM_REGION)

        return listOf(sendMessage(update, "Qaysi viloyatdan").apply { this.replyMarkup = replyMarkup })
    }

    private fun callbackFromRegion(update: Update): List<SendMessage> {
        val name = update.callbackQuery.data.substringAfter(PREFIX_FROM_REGION)
        val list = subRegionRepository.findAllByRegionNameLatin(name)
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_FROM_SUB_REGION)

        return listOf(sendMessage(update, "Qaysi shahar/tumandan").apply { this.replyMarkup = replyMarkup })
    }

    private fun commandStart(update: Update): List<SendMessage> {

        clearVariables()

        telegramUser = telegramUserService.saveUser(update)

        val keyboard = listOf( listOf(
            InlineKeyboardButton("\uD83D\uDE96 Taksi izlash") .apply { callbackData = "${PREFIX_TYPE}TAXI" },
            InlineKeyboardButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️Yo'lovchi izlash").apply { callbackData = "${PREFIX_TYPE}CLIENT"}))

        val markup = InlineKeyboardMarkup().apply { this.keyboard = keyboard }
        val responseText = "\uD83D\uDC47 Quyidagilardan birini tanlang"

        ReplyKeyboardRemove(true)

        return listOf(sendMessage(update, responseText).apply { replyMarkup = markup })
    }

    private fun sendMessage(update: Update, replyText: String): SendMessage{
        val chatId = getChatId(update)
        return SendMessage(chatId, replyText)
    }


    private fun getChatId(update: Update) =
        if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()

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
            }
            else if (index == objectList.size - 1)
                keyBoardList.add(keyBoardRow)
        }

        return InlineKeyboardMarkup().apply { this.keyboard = keyBoardList }
    }

    private fun clearVariables() {
        announcementType = null
        from = null
        to = null
        telegramUser = null
        date = null
        phone = null
    }

    companion object {
        const val PREFIX_TYPE = "Type_"
        const val PREFIX_FROM_SUB_REGION = "FromSubRegion_"
        const val PREFIX_TO_SUB_REGION = "ToSubRegion_"
        const val PREFIX_FROM_REGION = "FromRegion_"
        const val PREFIX_TO_REGION = "ToRegion_"
        const val PREFIX_DATE = "Date_"
        const val SAVE_ANNOUNCEMENT = "SaveAnnouncement"
        const val CHANGE = "Change"
        const val CHANNEL_ID_TAXI_BEKAT_TEST = "@taxi_bekat_test_chanel"
        const val CHANNEL_LINK_TAXI_BEKAT_TEST = "https://t.me/taxi_bekat_test_chanel"
    }

}
