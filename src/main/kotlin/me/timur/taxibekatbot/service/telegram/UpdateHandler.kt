package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.Announcement
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.enum.AnnouncementType
import me.timur.taxibekatbot.repository.RegionRepository
import me.timur.taxibekatbot.repository.SubRegionRepository
import me.timur.taxibekatbot.service.AnnouncementService
import me.timur.taxibekatbot.service.TelegramUserService
import me.timur.taxibekatbot.util.InvokeGetter
import me.timur.taxibekatbot.util.PhoneUtil.containsPhone
import me.timur.taxibekatbot.util.PhoneUtil.formatPhoneNumber
import me.timur.taxibekatbot.util.PhoneUtil.getFullPhoneNumber
import me.timur.taxibekatbot.util.getStringByKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    @Value("\${bot.username}")
    lateinit var botName: String

    var announcementType: AnnouncementType? = null
    var from: SubRegion? = null
    var to: SubRegion? = null
    var date: LocalDate? = null
    var phone: String? = null
    var telegramUser: TelegramUser? = null

    fun handle(update: Update): List<BotApiMethod<Message>>{

        val messages: List<BotApiMethod<Message>> = when {
            update.hasMessage() -> when {
                    update.message.text == "/start" -> commandStart(update)
                    containsPhone(update) -> reviewAnnouncement(update)
                    else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
                }

            update.hasCallbackQuery() -> {
                val callbackData = update.callbackQuery.data
                when {
                    callbackData.contains(PREFIX_TYPE) -> chooseRoute(update)
                    callbackData.contains(PREFIX_ROUTE) -> setRouteAndChooseDate(update)
                    callbackData.contains(PREFIX_NEW_ROUTE) -> chooseFromRegion(update)
                    callbackData.contains(PREFIX_FROM_REGION) -> chooseFromSubRegion(update)
                    callbackData.contains(PREFIX_FROM_SUB_REGION) -> chooseToRegion(update)
                    callbackData.contains(PREFIX_TO_REGION) -> chooseToSubRegion(update)
                    callbackData.contains(PREFIX_TO_SUB_REGION) -> chooseDate(update)
                    callbackData.contains(PREFIX_DATE) -> requestContact(update)
                    callbackData.contains(SAVE_ANNOUNCEMENT) -> saveAnnouncement(update)
                    callbackData.contains(CHANGE) -> commandStart(update)
                    else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
                }
            }

            else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
        }

        return messages
    }

    private fun setRouteAndChooseDate(update: Update): List<BotApiMethod<Message>> {
        val fromSubRegLatinName = update.getStringByKey(PREFIX_ROUTE).substringBefore("-")
        val toSubRegLatinName = update.getStringByKey("-")

        from = subRegionRepository.findByNameLatin(fromSubRegLatinName)
        to = subRegionRepository.findByNameLatin(toSubRegLatinName)

        return chooseDate(update)
    }

    private fun saveAnnouncement(update: Update): List<BotApiMethod<Message>> {
        val messageId = if (update.hasMessage()) update.message.messageId else update.callbackQuery.message.messageId

        val announcement = Announcement(announcementType, date, from, to, telegramUser, messageId)
        announcementService.save(announcement)

        var replyText = "#${announcement.id} raqamli e'lon joylashtirildi" +
                "\n $CHANNEL_LINK_TAXI_BEKAT_TEST/${announcement.telegramMessageId}{" +
                "\n ${announcementType!!.emoji} Qidirilmoqda: ${announcementType!!.nameLatin} " +
                "\n\n \uD83D\uDDFA ${from?.nameLatin} - ${to?.nameLatin} " +
                "\n \uD83D\uDCC5 ${date!!.dayOfMonth}-${date!!.month}-${date!!.year}\"" +
                "\n \uD83D\uDCF1 Tel: ${formatPhoneNumber("$phone")}" +
                "\n" +
                "\n #${(from?.nameLatin)?.substringBefore(" ")}${(to?.nameLatin)?.substringBefore(" ")}$announcementType"

        val matchingAnnouncements = announcementService.matchAnnouncement(announcement)

        if (matchingAnnouncements.isEmpty())
           replyText = "$replyText\n\n hozircha mos e'lon topilmadi."
        else {
            replyText = "$replyText\n\n Quyidagi e'lonlar sizga mos kelishi mumkin: "
            matchingAnnouncements.forEach {
                replyText += "\n\n $CHANNEL_LINK_TAXI_BEKAT_TEST/${it.telegramMessageId}"
            }
        }

        val chatId = getChatId(update)
        val sendMessage = SendMessage(chatId, replyText)

        val forwardMessage = ForwardMessage(CHANNEL_ID_TAXI_BEKAT_TEST, chatId, messageId)

        return listOf(
            sendMessage.apply { replyMarkup = ReplyKeyboardRemove(true) },
            forwardMessage)
    }

    private fun reviewAnnouncement(update: Update): List<SendMessage> {
        phone = getFullPhoneNumber(update)

        telegramUserService.savePhone(update)

        val replyText = "\n ${announcementType!!.emoji} Qidirilmoqda: ${announcementType!!.nameLatin} " +
                "\n\n \uD83D\uDDFA ${from?.nameLatin} - ${to?.nameLatin} " +
                "\n \uD83D\uDCC5 ${date!!.dayOfMonth}-${date!!.month}-${date!!.year}\"" +
                "\n \uD83D\uDCF1 Tel: ${formatPhoneNumber("$phone")}" +
                "\n" +
                "\n #${(from?.nameLatin)?.substringBefore(" ")}${(to?.nameLatin)?.substringBefore(" ")}$announcementType" +
                "\n" +
                "\nYangi e’lon berish uchun quyidagi botdan foydalaning @$botName"

        val markup = InlineKeyboardMarkup().apply { this.keyboard = listOf(
            listOf(
                InlineKeyboardButton("✅ E'lonni joylash").apply { callbackData = SAVE_ANNOUNCEMENT },
                InlineKeyboardButton("✏️O'zgartirish").apply { callbackData = CHANGE }
            ))
        }

        return listOf(sendMessage(update, replyText).apply { this.replyMarkup = markup })
    }

    private fun requestContact(update: Update): List<SendMessage> {
        val dateInString = update.getStringByKey(PREFIX_DATE)
        date = LocalDate.parse(dateInString)

        val replyText = "Telefon raqamingizni kodi bilan kiriting yoki " +
                "\"\uD83D\uDCF1Raqamni yuborish\" tugmachasini bosing ⬇"
        val keyboard = KeyboardButton().apply {
            this.text = "\uD83D\uDCF1Raqamni yuborish"
            this.requestContact = true
        }

        val keyboardRow = KeyboardRow().apply { add(keyboard) }
        val markup = ReplyKeyboardMarkup().apply {
            this.keyboard = listOf(keyboardRow)
            oneTimeKeyboard = true
            resizeKeyboard = true
        }

        return listOf(sendMessage(update, replyText).apply { this.replyMarkup = markup })
    }

    private fun chooseDate(update: Update): List<SendMessage> {
        if(to == null) {
            val name = update.getStringByKey(PREFIX_TO_SUB_REGION)
            to = subRegionRepository.findByNameLatin(name)
        }

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

            if (i % 5 == 1){
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

    private fun chooseToSubRegion(update: Update): List<SendMessage> {
        val name = update.getStringByKey(PREFIX_TO_REGION)
        val list = subRegionRepository.findAllByRegionNameLatin(name)
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_TO_SUB_REGION)

        return listOf(sendMessage(update, "\uD83D\uDFE6 Qaysi shahar/tumanga").apply { this.replyMarkup = replyMarkup })
    }

    private fun chooseToRegion(update: Update): List<SendMessage> {
        val name = update.getStringByKey(PREFIX_FROM_SUB_REGION)
        from = subRegionRepository.findByNameLatin(name)

        val list = regionRepository.findAll()
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_TO_REGION)

        return listOf(sendMessage(update, "\uD83D\uDFE6 Qaysi viloyatga").apply { this.replyMarkup = replyMarkup })
    }

    private fun chooseFromSubRegion(update: Update): List<SendMessage> {
        val name = update.getStringByKey(PREFIX_FROM_REGION)
        val list = subRegionRepository.findAllByRegionNameLatin(name)
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_FROM_SUB_REGION)

        return listOf(sendMessage(update, "\uD83D\uDFE5 Qaysi shahar/tumandan").apply { this.replyMarkup = replyMarkup })
    }


    private fun chooseFromRegion(update: Update): List<SendMessage> {
        if (announcementType == null)
            announcementType = AnnouncementType.findByName(update.getStringByKey(PREFIX_TYPE))

        val list = regionRepository.findAll()
        val replyMarkup = createMarkupFromPlaceList(list, PREFIX_FROM_REGION)

        return listOf(sendMessage(update, "\uD83D\uDFE5 Qaysi viloyatdan").apply { this.replyMarkup = replyMarkup })
    }

    private fun chooseRoute(update: Update): List<BotApiMethod<Message>> {
        if (announcementType == null)
            announcementType = AnnouncementType.findByName(update.getStringByKey(PREFIX_TYPE))

        val routes = announcementService.getMostPopularRoutesByUserAndAnnouncementType(telegramUser!!, announcementType!!)

        return if (routes.isNullOrEmpty())
            chooseFromRegion(update)
        else {
            val keyBoardList = ArrayList<List<InlineKeyboardButton>>()
            routes.forEach {
                keyBoardList.add(listOf(InlineKeyboardButton("✅ $it").apply { callbackData = "$PREFIX_ROUTE$it" }))
            }
            keyBoardList.add(listOf(InlineKeyboardButton("➕ Boshqa yo'nalish").apply { callbackData = PREFIX_NEW_ROUTE }))

            val inlineKeyboard = InlineKeyboardMarkup().apply { keyboard = keyBoardList }

            listOf(sendMessage(update, "\uD83D\uDDFA Yo'nalishni tanlang").apply { replyMarkup = inlineKeyboard })
        }
    }

    private fun commandStart(update: Update): List<SendMessage> {

        clearVariables()

        telegramUser = telegramUserService.saveUser(update)

        val keyboard = listOf( listOf(
            InlineKeyboardButton("${AnnouncementType.TAXI.emoji} ${AnnouncementType.TAXI.nameLatin}") .apply { callbackData = "${PREFIX_TYPE}${AnnouncementType.TAXI.name}"},
            InlineKeyboardButton("${AnnouncementType.CLIENT.emoji}️${AnnouncementType.CLIENT.nameLatin}").apply { callbackData = "${PREFIX_TYPE}${AnnouncementType.CLIENT.name}"},
            InlineKeyboardButton("${AnnouncementType.POST.emoji}️${AnnouncementType.POST.nameLatin}").apply { callbackData = "${PREFIX_TYPE}${AnnouncementType.POST.name}"}
        ))

        val markup = InlineKeyboardMarkup().apply { this.keyboard = keyboard }
        val responseText = "\uD83D\uDC47 Nima qidirayapsiz"

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
        const val PREFIX_ROUTE = "Route_"
        const val PREFIX_NEW_ROUTE = "NewRoute"
        const val SAVE_ANNOUNCEMENT = "SaveAnnouncement"
        const val CHANGE = "Change"
        const val CHANNEL_ID_TAXI_BEKAT_TEST = "@taxi_bekat_test_chanel"
        const val CHANNEL_LINK_TAXI_BEKAT_TEST = "https://t.me/taxi_bekat_test_chanel"
    }

}
