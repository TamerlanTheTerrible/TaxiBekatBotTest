package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.*
import me.timur.taxibekatbot.enum.TripType
import me.timur.taxibekatbot.repository.CarRepository
import me.timur.taxibekatbot.repository.FrameRouteRepository
import me.timur.taxibekatbot.repository.RegionRepository
import me.timur.taxibekatbot.service.*
import me.timur.taxibekatbot.util.KeyboardUtils.createKeyboard
import me.timur.taxibekatbot.util.KeyboardUtils.createKeyboardRow
import me.timur.taxibekatbot.util.KeyboardUtils.createReplyKeyboardMarkup
import me.timur.taxibekatbot.util.PhoneUtil.containsPhone
import me.timur.taxibekatbot.util.PhoneUtil.formatPhoneNumber
import me.timur.taxibekatbot.util.UpdateUtil.getChatId
import me.timur.taxibekatbot.util.getStringAfter
import me.timur.taxibekatbot.util.toInlineKeyBoard
import me.timur.taxibekatbot.util.UpdateUtil.sendMessage
import me.timur.taxibekatbot.util.getMessageId
import me.timur.taxibekatbot.util.getStringBefore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.time.LocalDate

@Component("clientMessageService")
@Primary
class ClientMessageService
@Autowired constructor(
        private val regionRepository: RegionRepository,
        private val subRegionService: SubRegionService,
        private val tripService: TripService,
        private val telegramUserService: TelegramUserService,
        private val frameRouteRepository: FrameRouteRepository,
        private val routeService: RouteService,
        private val carRepository: CarRepository,
        private val driverService: DriverService
): MessageService{
    @Value("\${bot.username}")
    lateinit var botName: String
    var user = TelegramUser()
    var trip = Trip()
    var driver = Driver()

    lateinit var clientRoutesCached: List<String>
    lateinit var fromRegionNames: List<String>
    lateinit var fromSubRegionNames: List<String>
    lateinit var toRegionNames: List<String>
    lateinit var toSubRegionNames: List<String>

    lateinit var taxiFrameRoute: String
    lateinit var taxiSubregionToChooseFrom: ArrayList<String>
    val taxiSubRegionNameSet = ArrayList<String>()
    var taxiRoutesLimit = 2
    lateinit var carNames: List<String>
    lateinit var carName: String


    override fun generateMessage(update: Update): List<BotApiMethod<Message>>{
        val messages: List<BotApiMethod<Message>> = when {
            update.hasMessage() -> when {
                //general
                update.message.text == "/start" -> commandStart(update)
                //client
                update.message.text == btnNeedTaxi -> chooseClientRoute(update)
                update.message.text == btnNeedToSendPost -> choosePostRoute(update)
                clientRoutesCached.any { it == update.message.text } -> setRouteAndChooseDate(update)
                datesToChoseFrom.any{ it == update.message.text } -> requestContact(update)
                update.message.text == btnSaveTrip -> saveTrip(update)
                update.message.text == btnChangeTrip -> denyTrip(update)
                update.message.text == btnStartNewTrip -> commandStart(update)
                update.message.text == btnNewClientRoute -> chooseFromRegion(update)
                fromRegionNames.any { it == update.message.text } -> chooseFromSubRegion(update)
                fromSubRegionNames.any { it == update.message.text } -> chooseToRegion(update)
                toRegionNames.any { it == update.message.text } -> chooseToSubRegion(update)
                toSubRegionNames.any{ it == update.message.text} -> chooseDate(update)
                //taxi
                update.message.text == btnIamTaxi -> chooseFirstTaxiRoute(update)
                taxiSubregionToChooseFrom.any{ it == update.message.text } -> chooseOtherTaxiRoutes(update)
                carNames.any { it == update.message.text } -> previewDriverData(update)
                update.message.text == btnSaveDriverDetails -> saveDriverDetails(update)

                containsPhone(update) -> reviewTrip(update)
                    else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
                }

            update.hasCallbackQuery() -> {
                val callbackData = update.callbackQuery.data
                when {
                    callbackData.contains(PREFIX_TYPE) -> chooseClientRoute(update)
//                    callbackData.contains(PREFIX_FRAME_ROUTE) -> chooseFirstTaxiRoute(update)
//                    callbackData.contains(PREFIX_ROUTE_TAXI) -> chooseOtherTaxiRoutes(update)
//                    callbackData.contains(PREFIX_CAR) -> previewDriverData(update)
//                    callbackData.contains(SAVE_DRIVER_DATA) -> saveDriverDetails(update)
//                    callbackData.contains(ACCEPT_CLIENT) -> notifyClient(update)
                    callbackData.contains(PREFIX_ROUTE_CLIENT) -> setRouteAndChooseDate(update)
                    callbackData.contains(PREFIX_NEW_ROUTE_CLIENT) -> chooseFromRegion(update)
                    callbackData.contains(PREFIX_FROM_REGION) -> chooseFromSubRegion(update)
                    callbackData.contains(PREFIX_FROM_SUB_REGION) -> chooseToRegion(update)
                    callbackData.contains(PREFIX_TO_REGION) -> chooseToSubRegion(update)
                    callbackData.contains(PREFIX_TO_SUB_REGION) -> chooseDate(update)
                    callbackData.contains(PREFIX_DATE) -> requestContact(update)
                    callbackData.contains(SAVE_TRIP) -> saveTrip(update)
                    callbackData.contains(CHANGE) -> commandStart(update)
                    else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
                }
            }

            else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
        }

        return messages
    }

    //CLIENT
    private fun commandStart(update: Update): List<SendMessage> {
        clearVariables()
        user = telegramUserService.saveUser(update)

        val markup = createReplyKeyboardMarkup(btnNeedTaxi, btnNeedToSendPost, btnIamTaxi)
        val responseText = "Assalomu alaykum. Nima qidirayapsiz?"
        return listOf(sendMessage(update, responseText, markup))
    }

    private fun choosePostRoute(update: Update): List<BotApiMethod<Message>> {
        if (trip.type == null)
            trip.type = TripType.POST
        return chooseRote(update)
    }

    private fun chooseClientRoute(update: Update): List<BotApiMethod<Message>> {
        if (trip.type == null)
            trip.type = TripType.CLIENT
        return chooseRote(update)
    }

    private fun chooseRote(update: Update): List<BotApiMethod<Message>> {
        clientRoutesCached = tripService.getMostPopularRoutesByUserAndType(user, trip.type!!)

        return if (clientRoutesCached.isNullOrEmpty())
            chooseFromRegion(update)
        else {
            val keyboard = createKeyboard(clientRoutesCached)
            keyboard.add(createKeyboardRow(btnNewClientRoute))

            val markup = ReplyKeyboardMarkup(keyboard, true, true, false)
            listOf(sendMessage(update, "\uD83D\uDDFA Yo'nalishni tanlang", markup))
        }
    }

    private fun setRouteAndChooseDate(update: Update): List<BotApiMethod<Message>> {
        val fromSubRegLatinName = update.getStringBefore("-")
        val toSubRegLatinName = update.getStringAfter("-")

        trip.from = subRegionService.findByNameLatin(fromSubRegLatinName)
        trip.to = subRegionService.findByNameLatin(toSubRegLatinName)

        return chooseDate(update)
    }

    private fun chooseDate(update: Update): List<SendMessage> {
        if(trip.to== null) {
            val toSubRegLatinName = update.message.text.substringAfter("\uD83D\uDFE6 ")
            trip.to = subRegionService.findByNameLatin(toSubRegLatinName)
        }

        val markup = createReplyKeyboardMarkup(datesToChoseFrom)
        val replyText = "\uD83D\uDCC5 Sa'nani kiriting"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun requestContact(update: Update): List<SendMessage> {
        val dateInString = update.message.text
        trip.tripDate = if (dateInString == btnToday) LocalDate.now() else LocalDate.now().plusDays(1)

        val button = KeyboardButton().apply {
            this.text = "\uD83D\uDCF1Raqamni yuborish"
            this.requestContact = true
        }

        val keyboard = KeyboardRow().apply { add(button) }
        val markup = ReplyKeyboardMarkup(listOf(keyboard), true, true, false)

        val replyText = "Telefon raqamingizni kodi bilan kiriting yoki " +
                "\"\uD83D\uDCF1Raqamni yuborish\" tugmachasini bosing ⬇"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun reviewTrip(update: Update): List<SendMessage> {
        telegramUserService.savePhone(update)

        val replyText = generateTripAnnouncement() +
                "\nE’lon berish uchun yoki yo'ovchi qidirish uchun quyidagi botdan foydalaning @$botName"

        val markup = createReplyKeyboardMarkup(btnSaveTrip, btnChangeTrip)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun denyTrip(update: Update): List<SendMessage> {
        val markup = createReplyKeyboardMarkup(btnStartNewTrip)
        val replyText = "❌ O'zgarishlar bekor qilindi" +
                "\n\n Yangi e'lon berish uchun pasdagi tugmani bosing \uD83D\uDC47"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun saveTrip(update: Update): List<BotApiMethod<Message>> {
        val messageId = update.getMessageId()
        trip.telegramMessageId = update.getMessageId()
        trip.telegramUser = user
        tripService.save(trip)

        val replyTextClient = "#${trip.id} raqamli e'lon joylashtirildi" +
                "\n $CHANNEL_LINK_TAXI_BEKAT_TEST/${trip.telegramMessageId}" +
                generateTripAnnouncement() +
                "\n\uD83E\uDD1D Mos haydovchi toplishi bilan aloqaga chiqadi" +
                "\n\n\uD83D\uDE4F @TaxiBekatBot dan foydalanganingiz uchun rahmat. Yo'lingzi bexatar bo'lsin"

        val messageToClient = sendMessage(update, replyTextClient, ReplyKeyboardRemove(true))
        val messageToForward = ForwardMessage(CHANNEL_ID_TAXI_BEKAT_TEST, getChatId(update), messageId-1)
        val messages = arrayListOf(messageToClient, messageToForward)

        val notificationForDrivers = notifyMatchingDrivers(trip)
        messages.addAll(notificationForDrivers)

        return messages
    }

    private fun notifyMatchingDrivers(newTrip: Trip): List<BotApiMethod<Message>> {
        trip = newTrip
        val drivers = driverService.findAllByMatchingRoute(newTrip).toHashSet()

        val notifications = arrayListOf<BotApiMethod<Message>>()

        drivers.forEach {
            val text = "Quyidagi mijoz haydovchi qidirmoqda. " +
                    "\n\n - Ushbu so'rovni qabul qilsangiz mijoz bilan bog'laning. " +
                    "\n - Kelushuvga kelganingizdan so'ng \"Qabul qilish\" tugmasini bosing" +
                    "\n - Agar so'rov mijoz tomonidanam tasdiqlansa sizga xabar keladi" +
                    "\n ${generateTripAnnouncement()}"

            val markup = createReplyKeyboardMarkup(btnAcceptClientRequest, btnDenyClientRequest)
            val notification = sendMessage(it.telegramUser.chatId!!, text, markup)
            notifications.add(notification)
        }

        return notifications
    }

    private fun chooseFromRegion(update: Update): List<SendMessage> {
        fromRegionNames = regionRepository.findAll().map { "\uD83D\uDFE5 ${it.nameLatin!!}"}
        val replyMarkup = createReplyKeyboardMarkup(fromRegionNames)

        return listOf(sendMessage(update, "Qaysi viloyatdan", replyMarkup))
    }

    private fun chooseFromSubRegion(update: Update): List<SendMessage> {
        val regionName = update.message.text.substringAfter("\uD83D\uDFE5 ")
        fromSubRegionNames = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDFE5 ${it.nameLatin}" }
        val replyMarkup = createReplyKeyboardMarkup(fromSubRegionNames)

        return listOf(sendMessage(update, "Qaysi shahar/tumandan", replyMarkup))
    }

    private fun chooseToRegion(update: Update): List<SendMessage> {
        val sebRegionName = update.message.text.substringAfter("\uD83D\uDFE5 ")
        trip.from = subRegionService.findByNameLatin(sebRegionName)

        toRegionNames = regionRepository.findAll().map { "\uD83D\uDFE6 ${it.nameLatin!!}"}
        val replyMarkup = createReplyKeyboardMarkup(toRegionNames)

        return listOf(sendMessage(update, "Qaysi viloyatga", replyMarkup))
    }

    private fun chooseToSubRegion(update: Update): List<SendMessage> {
        val regionName = update.message.text.substringAfter("\uD83D\uDFE6 ")
        toSubRegionNames = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDFE6 ${it.nameLatin}" }
        val replyMarkup = createReplyKeyboardMarkup(toSubRegionNames)

        return listOf(sendMessage(update, "Qaysi shahar/tumanga", replyMarkup))
    }


    //TAXI
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

    private fun chooseFirstTaxiRoute(update: Update): List<BotApiMethod<Message>> {
        taxiFrameRoute = update.message.text
        val regionName = taxiFrameRoute.substringAfterLast("-")
        taxiSubregionToChooseFrom = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDFE2 ${it.nameLatin}" } as ArrayList<String>

        val markup = createReplyKeyboardMarkup(taxiSubregionToChooseFrom)
        val text = "O'zingiz qatnaydigan tuman/shaharni " +
                "\n\n $taxiRoutesLimit ta tanlashingiz mumkin"

        return listOf(sendMessage(update, text, markup))
    }

    private fun chooseOtherTaxiRoutes(update: Update): List<BotApiMethod<Message>> {
        taxiRoutesLimit--
        if (taxiRoutesLimit == 0)
            return chooseTaxiCar(update)

        taxiSubregionToChooseFrom.remove(update.message.text)

        val subRegionName = update.getStringAfter("\uD83D\uDFE2 ")
        taxiSubRegionNameSet.add(subRegionName)

        val markup = createReplyKeyboardMarkup(taxiSubregionToChooseFrom)
        val text = "O'zingiz qatnaydigan tuman/shaharni " +
                "\n\n yana $taxiRoutesLimit ta tanlashingiz mumkin"

        return listOf(sendMessage(update, text, markup))
    }

    private fun chooseTaxiCar(update: Update): List<BotApiMethod<Message>> {
        carNames = carRepository.findAll().map { it.nameLatin!! }
        val markup = createReplyKeyboardMarkup(carNames)
        val replyText = "Moshinangiz rusumini tanlang"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun previewDriverData(update: Update): List<BotApiMethod<Message>> {
        carName = update.message.text

        val replyText = "Agar ma'lumotlar to'g'ri bo'lsa saqlash tugmasini bosing:" +
                "\n\n\uD83D\uDEE3 - Asosiy marshrut: $taxiFrameRoute" +
                "\n\n\uD83C\uDF07 - Siz qatnaydigan tuman/shaharlar: ${taxiSubRegionNameSet.toString().substringAfter("[").substringBefore("]")}" +
                "\n\n\uD83D\uDE98 - Moshinangiz rusumi: $carName"

        val replyMarkup = createReplyKeyboardMarkup(btnSaveDriverDetails, btnCancelDriverDetails)

        return listOf(sendMessage(update, replyText, replyMarkup))
    }

    private fun saveDriverDetails(update: Update): List<BotApiMethod<Message>> {
        driver.car = carRepository.findByNameLatin(carName!!)!!
        driver.telegramUser = trip.telegramUser!!
        driver = driverService.saveOrUpdate(driver)

        val subRegionList = subRegionService.findAllByNames(taxiSubRegionNameSet)
        val destinationRegionName = taxiFrameRoute.substringAfter("-").substringBeforeLast("-")
        val destination = subRegionService.findMainSubRegion(destinationRegionName)
        routeService.createRoutesFromSubRegions(subRegionList, destination, driver)

        val replyText = "✅ Ma'lumotlar saqlandi. " +
                "\n\n\uD83E\uDD1D Yo'nalishlaringizga mos yo'lovchi chiqsa, sizga darhol xabar beramiz. " +
                "\n\n\uD83D\uDCDE Qo'shimcha ma'lumotlar uchun +998 99 3972636 ga murojat qilishingiz mumkin" +
                "\n\n\uD83D\uDE4F @TaxiBekatBot dan foydalanganingiz uchun rahmat. Yo'lingzi bexatar bo'lsin"

        return listOf(sendMessage(update, replyText))
    }

    private fun clearVariables() {
        trip = Trip()
    }

    private fun generateTripAnnouncement(): String =
        "\n ${trip.type!!.emoji} ${trip.type!!.nameLatin} " +
        "\n \uD83D\uDDFA ${trip.getTripStartPlace()} - ${trip.getTripEndPlace()} " +
        "\n \uD83D\uDCC5 ${trip.getTripDay()}-${trip.getTripMonth()}-${trip.getTripYear()}" +
        "\n \uD83D\uDCF1 Tel: ${formatPhoneNumber("${user.phone}")}" +
        "\n" +
        "\n #${trip.getTripStartPlace()?.substringBefore(" ")}${trip.getTripEndPlace()?.substringBefore(" ")}${trip.type}" +
        "\n"

    companion object {
        const val BEAN_PREFIX = "clientMessageService"

        //start
        val btnNeedTaxi = "${TripType.CLIENT.emoji} Menga haydovchi kerak"
        val btnNeedToSendPost = "${TripType.POST.emoji} Pochta jo'natmoqchiman"
        val btnIamTaxi = "${TripType.TAXI.emoji} Men haydovchiman"
        //client route
        val btnNewClientRoute = "➕ Boshqa yo'nalish"
        //date
        const val btnToday = "Bugun"
        const val btnTomorrow = "Ertaga"
        val datesToChoseFrom = listOf(btnToday, btnTomorrow)
        //review announcement
        const val btnSaveTrip = "✅ E'lonni joylash"
        const val btnChangeTrip = "✏ E'lonni o'zgartirish"
        //deny trip
        const val btnStartNewTrip = "Ya'ngi e'lon berish"


        //notify-drivers
        const val btnAcceptClientRequest = "✅ Qabul qilish"
        const val btnDenyClientRequest = "❌ Rad qilish"
        const val btnSaveDriverDetails = "✅ Saqlash"
        const val btnCancelDriverDetails = "❌ Bekor qilish"


        const val PREFIX_TYPE = "$BEAN_PREFIX:Type_"
        const val PREFIX_FROM_SUB_REGION = "$BEAN_PREFIX:FromSubRegion_"
        const val PREFIX_TO_SUB_REGION = "$BEAN_PREFIX:ToSubRegion_"
        const val PREFIX_FROM_REGION = "$BEAN_PREFIX:FromRegion_"
        const val PREFIX_TO_REGION = "$BEAN_PREFIX:ToRegion_"
        const val PREFIX_DATE = "$BEAN_PREFIX:Date_"
        const val PREFIX_ROUTE_CLIENT = "$BEAN_PREFIX:RouteClient_"
        const val PREFIX_NEW_ROUTE_CLIENT = "$BEAN_PREFIX:RouteClientNew_"
        const val SAVE_TRIP = "$BEAN_PREFIX:SaveTrip"
        const val CHANGE = "$BEAN_PREFIX:Change"
        const val ACCEPT_CLIENT = "AcceptClient_"
        const val ACCEPT_DRIVER = "AcceptDriver_"

//        const val PREFIX_ROUTE_TAXI = "RouteTaxi_"
//        const val PREFIX_FRAME_ROUTE = "FrameRoute"
//        const val PREFIX_CAR = "Car_"
//        const val SAVE_DRIVER_DATA = "SaveTaxiDetails"
//        const val ACCEPT_CLIENT = "AcceptClient_"
//        const val ACCEPT_DRIVER = "AcceptDriver_"
//        const val DENY_CLIENT = "DenyClient_"
//        const val DENY_DRIVER = "DenyDriver_"
        const val CHANNEL_ID_TAXI_BEKAT_TEST = "@taxi_bekat_test_chanel"
        const val CHANNEL_LINK_TAXI_BEKAT_TEST = "https://t.me/taxi_bekat_test_chanel"
    }

}