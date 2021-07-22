package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.*
import me.timur.taxibekatbot.enum.AnnouncementType
import me.timur.taxibekatbot.repository.CarRepository
import me.timur.taxibekatbot.repository.FrameRouteRepository
import me.timur.taxibekatbot.repository.RegionRepository
import me.timur.taxibekatbot.service.*
import me.timur.taxibekatbot.util.KeyboardUtils.createInlineButton
import me.timur.taxibekatbot.util.KeyboardUtils.createInlineButtonList
import me.timur.taxibekatbot.util.PhoneUtil.containsPhone
import me.timur.taxibekatbot.util.PhoneUtil.formatPhoneNumber
import me.timur.taxibekatbot.util.PhoneUtil.getFullPhoneNumber
import me.timur.taxibekatbot.util.getStringAfter
import me.timur.taxibekatbot.util.getStringBetween
import me.timur.taxibekatbot.util.toInlineKeyBoard
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
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
    private val subRegionService: SubRegionService,
    private val announcementService: AnnouncementService,
    private val telegramUserService: TelegramUserService,
    private val frameRouteRepository: FrameRouteRepository,
    private val routeService: RouteService,
    private val carRepository: CarRepository,
    private val driverService: DriverService
){
    @Value("\${bot.username}")
    lateinit var botName: String

    var announcementType: AnnouncementType? = null
    var from: SubRegion? = null
    var to: SubRegion? = null
    var date: LocalDate? = null
    var phone: String? = null
    var telegramUser: TelegramUser? = null

    var taxiFrameRoute: String? = null
    val subRegionNameSet = ArrayList<String>()
    var carName: String? = null
    var taxiRoutesLimit = 2
    val taxiRoutes = ArrayList<Route>()

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
                    callbackData.contains(PREFIX_FRAME_ROUTE) -> chooseFirstTaxiRoute(update)
                    callbackData.contains(PREFIX_ROUTE_TAXI) -> chooseOtherTaxiRoutes(update)
                    callbackData.contains(PREFIX_CAR) -> previewDriverData(update)
                    callbackData.contains(SAVE_DRIVER_DATA) -> saveDriverDetails(update)
                    callbackData.contains(PREFIX_ROUTE_CLIENT) -> setRouteAndChooseDate(update)
                    callbackData.contains(PREFIX_NEW_ROUTE_CLIENT) -> chooseFromRegion(update)
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
        val fromSubRegLatinName = update.getStringBetween(PREFIX_ROUTE_CLIENT, "-")
        val toSubRegLatinName = update.getStringAfter("-")

        from = subRegionService.findByNameLatin(fromSubRegLatinName)
        to = subRegionService.findByNameLatin(toSubRegLatinName)

        return chooseDate(update)
    }

    private fun saveAnnouncement(update: Update): List<BotApiMethod<Message>> {
        val messageId = if (update.hasMessage()) update.message.messageId else update.callbackQuery.message.messageId

        val announcement = Announcement(announcementType, date, from, to, telegramUser, messageId)
        announcementService.save(announcement)

        val replyTextClient = "#${announcement.id} raqamli e'lon joylashtirildi" +
                "\n $CHANNEL_LINK_TAXI_BEKAT_TEST/${announcement.telegramMessageId}{" +
                "\n ${announcementType!!.emoji} Qidirilmoqda: ${announcementType!!.nameLatin} " +
                "\n\n \uD83D\uDDFA ${from?.nameLatin} - ${to?.nameLatin} " +
                "\n \uD83D\uDCC5 ${date!!.dayOfMonth}-${date!!.month}-${date!!.year}\"" +
                "\n \uD83D\uDCF1 Tel: ${formatPhoneNumber("$phone")}" +
                "\n" +
                "\n #${(from?.nameLatin)?.substringBefore(" ")}${(to?.nameLatin)?.substringBefore(" ")}$announcementType" +
                "\n" +
                "\n\uD83E\uDD1D Mos haydovchi toplishi bilan aloqaga chiqadi" +
                "\n\n\uD83D\uDE4F @TaxiBekatBot dan foydalanganingiz uchun rahmat. Yo'lingzi bexatar bo'lsin"

        val messageToClient = sendMessage(update, replyTextClient, ReplyKeyboardRemove(true))
        val messageToForward = ForwardMessage(CHANNEL_ID_TAXI_BEKAT_TEST, getChatId(update), messageId)
        val notificationForDrivers = notifyMatchingDrivers(announcement)

        val messages = arrayListOf(messageToClient, messageToForward)
        messages.addAll(notificationForDrivers)

        return messages
    }

    private fun notifyMatchingDrivers(announcement: Announcement): List<BotApiMethod<Message>> {
        val drivers = driverService.findAllByMatchingRoute(announcement)

        val notifications = arrayListOf<BotApiMethod<Message>>()

        drivers.forEach {
            val text = "Quyidagi mijoz haydovchi qidirmoqda. " +
                    "\n\n - Ushbu so'rovni qabul qilsangiz mijoz bilan bog'laning. " +
                    "\n - Kelushuvga kelganingizdan so'ng \"Qabul qilish\" tugmasini bosing" +
                    "\n - Agar so'rov mijoz tomonidanam tasdiqlansa sizga xabar keladi" +
                    "\n ${generateAnnouncementText()}"
            val keyboard = InlineKeyboardMarkup().apply { this.keyboard = listOf(
                createInlineButtonList("✅ Qabul qilish", ACCEPT_CLIENT),
                createInlineButtonList("❌ Rad qilish", DENY_CLIENT)
            )}
            val notification = sendMessage(it.telegramUser.chatId!!, text, keyboard)
            notifications.add(notification)
        }

        return notifications
    }

    private fun reviewAnnouncement(update: Update): List<SendMessage> {
        phone = getFullPhoneNumber(update)

        telegramUserService.savePhone(update)

        val replyText = generateAnnouncementText() +
                "\nYangi e’lon berish uchun quyidagi botdan foydalaning @$botName"

        val markup = InlineKeyboardMarkup().apply { this.keyboard = listOf(
            listOf(
                createInlineButton("✅ E'lonni joylash", SAVE_ANNOUNCEMENT),
                createInlineButton("✏️O'zgartirish", CHANGE)
            ))
        }

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun requestContact(update: Update): List<SendMessage> {
        val dateInString = update.getStringAfter(PREFIX_DATE)
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

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun chooseDate(update: Update): List<SendMessage> {
        if(to == null) {
            val name = update.getStringAfter(PREFIX_TO_SUB_REGION)
            to = subRegionService.findByNameLatin(name)
        }

        val keyBoardList = ArrayList<List<InlineKeyboardButton>>()
        var keyBoardRow = ArrayList<InlineKeyboardButton>()

        var now = LocalDate.now()
        var currentMonth = 0

        for (i in 0..4){

            if (now.month.value > currentMonth){
                currentMonth = now.month.value
                keyBoardRow.add(createInlineButton(now.month.name, PREFIX_TO_SUB_REGION))
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            }

            if (i % 2 == 1){
                keyBoardList.add(keyBoardRow)
                keyBoardRow = ArrayList()
            }

            keyBoardRow.add(createInlineButton(now.dayOfMonth.toString(), "$PREFIX_DATE$now"))

            now = now.plusDays(1)
        }

        keyBoardList.add(keyBoardRow)

        val replyText = "\uD83D\uDCC5 Sa'nani kiriting"
        val markup = InlineKeyboardMarkup().apply { keyboard = keyBoardList }

        return listOf(sendMessage(update, replyText, markup))

    }

    private fun chooseToSubRegion(update: Update): List<SendMessage> {
        val name = update.getStringAfter(PREFIX_TO_REGION)
        val list = subRegionService.findAllByRegionNameLatin(name)
        val replyMarkup = list.toInlineKeyBoard(PREFIX_TO_SUB_REGION, "nameLatin")

        return listOf(sendMessage(update, "\uD83D\uDFE6 Qaysi shahar/tumanga", replyMarkup))
    }

    private fun chooseToRegion(update: Update): List<SendMessage> {
        val name = update.getStringAfter(PREFIX_FROM_SUB_REGION)
        from = subRegionService.findByNameLatin(name)

        val list = regionRepository.findAll()
        val replyMarkup = list.toInlineKeyBoard(PREFIX_TO_REGION, "nameLatin")

        return listOf(sendMessage(update, "\uD83D\uDFE6 Qaysi viloyatga", replyMarkup))
    }

    private fun chooseFromSubRegion(update: Update): List<SendMessage> {
        val name = update.getStringAfter(PREFIX_FROM_REGION)
        val list = subRegionService.findAllByRegionNameLatin(name)
        val replyMarkup = list.toInlineKeyBoard(PREFIX_FROM_SUB_REGION, "nameLatin")

        return listOf(sendMessage(update, "\uD83D\uDFE5 Qaysi shahar/tumandan", replyMarkup))
    }

    private fun saveDriverDetails(update: Update): List<BotApiMethod<Message>> {
        val car = carRepository.findByNameLatin(carName!!)!!
        val driver = driverService.saveOrUpdate(Driver(telegramUser!!, car))

        val subRegionList = subRegionService.findAllByNames(subRegionNameSet)
        val destinationRegionName = taxiFrameRoute!!.substringAfter("-").substringBeforeLast("-")
        val destination = subRegionService.findMainSubRegion(destinationRegionName)
        routeService.createRoutesFromSubRegions(subRegionList, destination, driver)

        val replyText = "✅ Ma'lumotlar saqlandi. " +
                "\n\n\uD83E\uDD1D Yo'nalishlaringizga mos yo'lovchi chiqsa, sizga darhol xabar beramiz. " +
                "\n\n\uD83D\uDCDE Qo'shimcha ma'lumotlar uchun +998 99 3972636 ga murojat qilishingiz mumkin" +
                "\n\n\uD83D\uDE4F @TaxiBekatBot dan foydalanganingiz uchun rahmat. Yo'lingzi bexatar bo'lsin"

        return listOf(sendMessage(update, replyText))
    }

    private fun previewDriverData(update: Update): List<BotApiMethod<Message>> {
        carName = update.getStringAfter(PREFIX_CAR)

        val replyText = "Agar ma'lumotlar to'g'ri bo'lsa tasdiqlash tugmasini bosing:" +
                "\n\n\uD83D\uDEE3 - Asosiy marshrut: $taxiFrameRoute" +
                "\n\n\uD83C\uDF07 - Siz qatnaydigan tuman/shaharlar: ${subRegionNameSet.toString().substringAfter("[").substringBefore("]")}" +
                "\n\n\uD83D\uDE98 - Moshinangiz rusumi: $carName"

        val replyMarkup = InlineKeyboardMarkup().apply {
            this.keyboard = listOf(
                createInlineButtonList("✅ Tasdiqlash", SAVE_DRIVER_DATA),
                createInlineButtonList("❌ Bekor qilish", CHANGE)
            )
        }

        return listOf(sendMessage(update, replyText, replyMarkup))
    }

    private fun chooseTaxiCar(update: Update): List<BotApiMethod<Message>> {
        val cars = carRepository.findAll()
        val markup = cars.toInlineKeyBoard(PREFIX_CAR, "nameLatin")
        val replyText = "Moshinangiz rusumini tanlang"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun chooseOtherTaxiRoutes(update: Update): List<BotApiMethod<Message>> {
        taxiRoutesLimit--

        if (taxiRoutesLimit == 0)
            return chooseTaxiCar(update)

        val subRegionName = update.getStringAfter(PREFIX_ROUTE_TAXI)
        subRegionNameSet.add(subRegionName)

        val destinationRegionName = taxiFrameRoute!!.substringAfterLast("-")
        val subregions = subRegionService.findAllByRegionNameLatin(destinationRegionName)
            .filter { it.nameLatin != subRegionName }

        val markup = subregions.toInlineKeyBoard(PREFIX_ROUTE_TAXI, "nameLatin")
        val text = "O'zingiz qatnaydigan tuman/shaharni " +
                "\n\n yana $taxiRoutesLimit ta tanlashingiz mumkin"

        return listOf(sendMessage(update, text, markup))
    }

    private fun chooseFirstTaxiRoute(update: Update): List<BotApiMethod<Message>> {
        taxiFrameRoute = update.getStringAfter(PREFIX_FRAME_ROUTE)
        val regionName = taxiFrameRoute!!.substringAfterLast("-")
        val subregions = subRegionService.findAllByRegionNameLatin(regionName)

        val markup = subregions.toInlineKeyBoard(PREFIX_ROUTE_TAXI, "nameLatin")
        val text = "O'zingiz qatnaydigan tuman/shaharni " +
                "\n\n $taxiRoutesLimit ta tanlashingiz mumkin"

        return listOf(sendMessage(update, text, markup))
    }

    private fun chooseFromRegion(update: Update): List<SendMessage> {
        if (announcementType == null)
            announcementType = AnnouncementType.findByName(update.getStringAfter(PREFIX_TYPE))

        val list = regionRepository.findAll()
        val replyMarkup = list.toInlineKeyBoard(PREFIX_FROM_REGION, "nameLatin")

        return listOf(sendMessage(update, "\uD83D\uDFE5 Qaysi viloyatdan", replyMarkup))
    }

    private fun chooseRoute(update: Update): List<BotApiMethod<Message>> {
        val type = AnnouncementType.valueOf(update.getStringAfter(PREFIX_TYPE))

        return if(type == AnnouncementType.TAXI)
            chooseTaxiRoute(update)
        else chooseClientRoute(update)
    }

    private fun chooseTaxiRoute(update: Update): List<BotApiMethod<Message>> {
        val frameRoutes = frameRouteRepository.findAll()

        val keyBoardList = ArrayList<List<InlineKeyboardButton>>()

        frameRoutes.forEach {
            val home = it.home!!.nameLatin
            val destination = it.destination!!.nameLatin
            val route = "$home-$destination-$home"

            val buttons = createInlineButtonList(route, "$PREFIX_FRAME_ROUTE$route")
            keyBoardList.add(buttons)
        }

        val inlineKeyboard = InlineKeyboardMarkup().apply { keyboard = keyBoardList }
        return listOf(sendMessage(update, "\uD83D\uDDFA Qaysi yo'nalishda qatnaysiz", inlineKeyboard))
    }

    private fun chooseClientRoute(update: Update): List<BotApiMethod<Message>> {
        if (announcementType == null)
            announcementType = AnnouncementType.findByName(update.getStringAfter(PREFIX_TYPE))

        val routes = announcementService.getMostPopularRoutesByUserAndAnnouncementType(telegramUser!!, announcementType!!)

        return if (routes.isNullOrEmpty())
            chooseFromRegion(update)
        else {
            val keyBoardList = ArrayList<List<InlineKeyboardButton>>()
            routes.forEach {
                val buttons = createInlineButtonList("✅ $it", "$PREFIX_ROUTE_CLIENT$it")
                keyBoardList.add(buttons)
            }
            val anotherRouteButton = createInlineButtonList("➕ Boshqa yo'nalish", PREFIX_NEW_ROUTE_CLIENT)
            keyBoardList.add(anotherRouteButton)

            val inlineKeyboard = InlineKeyboardMarkup().apply { keyboard = keyBoardList }

            listOf(sendMessage(update, "\uD83D\uDDFA Yo'nalishni tanlang", inlineKeyboard))
        }
    }

    private fun commandStart(update: Update): List<SendMessage> {
        clearVariables()

        telegramUser = telegramUserService.saveUser(update)

        val keyboard = listOf(
            createInlineButtonList("${AnnouncementType.TAXI.emoji} Men haydovchiman", "${PREFIX_TYPE}${AnnouncementType.TAXI.name}"),
            createInlineButtonList("${AnnouncementType.CLIENT.emoji}️Menga haydovchi kerak", "${PREFIX_TYPE}${AnnouncementType.CLIENT.name}"),
            createInlineButtonList("${AnnouncementType.POST.emoji}️Pochta jo'natmoqchiman", "${PREFIX_TYPE}${AnnouncementType.POST.name}"))

        val markup = InlineKeyboardMarkup().apply { this.keyboard = keyboard }
        val responseText = "\uD83D\uDC47 Nima qidirayapsiz"

        ReplyKeyboardRemove(true)

        return listOf(sendMessage(update, responseText, markup))
    }

    private fun sendMessage(update: Update, replyText: String, markup: ReplyKeyboard? = null): SendMessage{
        val chatId = getChatId(update)
        return sendMessage(chatId, replyText, markup)
    }

    private fun sendMessage(chatId: String, replyText: String, markup: ReplyKeyboard? = null): SendMessage =
        SendMessage(chatId, replyText).apply { this.replyMarkup = markup }

    private fun getChatId(update: Update) =
        if (update.hasMessage()) update.message.chatId.toString() else update.callbackQuery.message.chatId.toString()

    private fun clearVariables() {
        announcementType = null
        from = null
        to = null
        telegramUser = null
        date = null
        phone = null
    }

    private fun generateAnnouncementText(): String =
        "\n ${announcementType!!.emoji} ${announcementType!!.nameLatin} " +
        "\n \uD83D\uDDFA ${from?.nameLatin} - ${to?.nameLatin} " +
        "\n \uD83D\uDCC5 ${date!!.dayOfMonth}-${date!!.month}-${date!!.year}\"" +
        "\n \uD83D\uDCF1 Tel: ${formatPhoneNumber("$phone")}" +
        "\n" +
        "\n #${(from?.nameLatin)?.substringBefore(" ")}${(to?.nameLatin)?.substringBefore(" ")}$announcementType" +
        "\n"


    companion object {
        const val PREFIX_TYPE = "Type_"
        const val PREFIX_FROM_SUB_REGION = "FromSubRegion_"
        const val PREFIX_TO_SUB_REGION = "ToSubRegion_"
        const val PREFIX_FROM_REGION = "FromRegion_"
        const val PREFIX_TO_REGION = "ToRegion_"
        const val PREFIX_DATE = "Date_"
        const val PREFIX_ROUTE_CLIENT = "RouteClient_"
        const val PREFIX_ROUTE_TAXI = "RouteTaxi_"
        const val PREFIX_FRAME_ROUTE = "FrameRoute"
        const val PREFIX_NEW_ROUTE_CLIENT = "RouteClientNew_"
        const val PREFIX_CAR = "Car_"
        const val SAVE_ANNOUNCEMENT = "SaveAnnouncement"
        const val SAVE_DRIVER_DATA = "SaveTaxiDetails"
        const val CHANGE = "Change"
        const val ACCEPT_CLIENT = "AcceptClient_"
        const val ACCEPT_DRIVER = "AcceptDriver_"
        const val DENY_CLIENT = "DenyClient_"
        const val DENY_DRIVER = "DenyDriver_"
        const val CHANNEL_ID_TAXI_BEKAT_TEST = "@taxi_bekat_test_chanel"
        const val CHANNEL_LINK_TAXI_BEKAT_TEST = "https://t.me/taxi_bekat_test_chanel"
    }

}