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

    var clientRoutesCached = emptyList<String>()
    var fromRegionNames = emptyList<String>()
    var fromSubRegionNames = emptyList<String>()
    var toRegionNames = emptyList<String>()
    var toSubRegionNames = emptyList<String>()

    var taxiFrameRoute: String = ""
    var taxiFrameRoutes = arrayListOf<String>()
    var taxiSubregionsToChooseFrom = arrayListOf<String>()
    var taxiSubRegionNameSet = arrayListOf<String>()
    var taxiRoutesLimit = 2
    var carNames = emptyList<String>()
    var carName: String = ""


    override fun generateMessage(update: Update): List<BotApiMethod<Message>>{
        val messages: List<BotApiMethod<Message>> = when {
            update.hasMessage() -> when {
                //general
                update.message.text == "/start" -> commandStart(update)
                update.message.text == btnMainMenu -> commandStart(update)
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
                update.message.text == btnIamTaxi -> chooseTaxiFrameRoute(update)
                taxiFrameRoutes.any { it == update.message.text } -> chooseFirstTaxiRoute(update)
                taxiSubregionsToChooseFrom.any{ it == update.message.text } -> chooseOtherTaxiRoutes(update)
                carNames.any { it == update.message.text } -> previewDriverData(update)
                update.message.text == btnSaveDriverDetails -> saveDriverDetails(update)
                update.message.text == btnCancelDriverDetails -> cancelDriverDetails(update)

                containsPhone(update) -> reviewTrip(update)
                    else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
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
            val toSubRegLatinName = update.message.text.substringAfter("\uD83D\uDD35 ")
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
                    "\n\n E'lon: ${trip.id}" +
                    "\n ${generateTripAnnouncement()}" +
                    "\n\n - Ushbu so'rovni qabul qilsangiz mijoz bilan bog'laning. " +
                    "\n - Kelushuvga kelganingizdan so'ng \"Qabul qilish\" tugmasini bosing" +
                    "\n - Agar so'rov mijoz tomonidanam tasdiqlansa sizga xabar keladi"


            val markup = createReplyKeyboardMarkup(btnAcceptClientRequest, btnDenyClientRequest)
            val notification = sendMessage(it.telegramUser.chatId!!, text, markup)
            notifications.add(notification)
        }

        return notifications
    }

    private fun chooseFromRegion(update: Update): List<SendMessage> {
        fromRegionNames = regionRepository.findAll().map { "\uD83D\uDD34 ${it.nameLatin!!}"}
        val replyMarkup = createReplyKeyboardMarkup(fromRegionNames)

        return listOf(sendMessage(update, "Qaysi viloyatdan", replyMarkup))
    }

    private fun chooseFromSubRegion(update: Update): List<SendMessage> {
        val regionName = update.message.text.substringAfter("\uD83D\uDD34 ")
        fromSubRegionNames = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDD34 ${it.nameLatin}" }
        val replyMarkup = createReplyKeyboardMarkup(fromSubRegionNames)

        return listOf(sendMessage(update, "Qaysi shahar/tumandan", replyMarkup))
    }

    private fun chooseToRegion(update: Update): List<SendMessage> {
        val sebRegionName = update.message.text.substringAfter("\uD83D\uDD34 ")
        trip.from = subRegionService.findByNameLatin(sebRegionName)

        toRegionNames = regionRepository.findAll().map { "\uD83D\uDD35 ${it.nameLatin!!}"}
        val replyMarkup = createReplyKeyboardMarkup(toRegionNames)

        return listOf(sendMessage(update, "Qaysi viloyatga", replyMarkup))
    }

    private fun chooseToSubRegion(update: Update): List<SendMessage> {
        val regionName = update.message.text.substringAfter("\uD83D\uDD35 ")
        toSubRegionNames = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDD35 ${it.nameLatin}" }
        val replyMarkup = createReplyKeyboardMarkup(toSubRegionNames)

        return listOf(sendMessage(update, "Qaysi shahar/tumanga", replyMarkup))
    }


    //TAXI
    private fun chooseTaxiFrameRoute(update: Update): List<BotApiMethod<Message>> {
        val frameRoutes = frameRouteRepository.findAll()

        frameRoutes.forEach {
            val home = it.home!!.nameLatin
            val destination = it.destination!!.nameLatin
            val route = "$home-$destination-$home"
            taxiFrameRoutes.add(route)
        }

        val markup = createReplyKeyboardMarkup(taxiFrameRoutes)
        return listOf(sendMessage(update, "\uD83D\uDDFA Qaysi yo'nalishda qatnaysiz", markup))
    }

    private fun chooseFirstTaxiRoute(update: Update): List<BotApiMethod<Message>> {
        taxiFrameRoute = update.message.text
        val regionName = taxiFrameRoute.substringAfterLast("-")
        taxiSubregionsToChooseFrom = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDFE2 ${it.nameLatin}" } as ArrayList<String>

        val markup = createReplyKeyboardMarkup(taxiSubregionsToChooseFrom)
        val text = "O'zingiz qatnaydigan tuman/shaharni " +
                "\n\n $taxiRoutesLimit ta tanlashingiz mumkin"

        return listOf(sendMessage(update, text, markup))
    }

    private fun chooseOtherTaxiRoutes(update: Update): List<BotApiMethod<Message>> {
        taxiRoutesLimit--
        if (taxiRoutesLimit == 0)
            return chooseTaxiCar(update)

        taxiSubregionsToChooseFrom.remove(update.message.text)

        val subRegionName = update.getStringAfter("\uD83D\uDFE2 ")
        taxiSubRegionNameSet.add(subRegionName)

        val markup = createReplyKeyboardMarkup(taxiSubregionsToChooseFrom)
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
                "\n\n\uD83D\uDEE3 Asosiy marshrut: $taxiFrameRoute" +
                "\n\n\uD83C\uDF07 Siz qatnaydigan tuman/shaharlar: ${taxiSubRegionNameSet.toString().substringAfter("[").substringBefore("]")}" +
                "\n\n\uD83D\uDE98 Moshinangiz rusumi: $carName"

        val replyMarkup = createReplyKeyboardMarkup(btnSaveDriverDetails, btnCancelDriverDetails)

        return listOf(sendMessage(update, replyText, replyMarkup))
    }

    private fun saveDriverDetails(update: Update): List<BotApiMethod<Message>> {
        driver.car = carRepository.findByNameLatin(carName)!!
        driver.telegramUser = user
        driver = driverService.saveOrUpdate(driver)

        val subRegionList = subRegionService.findAllByNames(taxiSubRegionNameSet)
        val destinationRegionName = taxiFrameRoute.substringAfter("-").substringBeforeLast("-")
        val destination = subRegionService.findMainSubRegion(destinationRegionName)
        routeService.updateDriverRoutes(subRegionList, destination, driver)

        val replyText = "✅ Ma'lumotlar saqlandi. " +
                "\n\uD83E\uDD1D Yo'nalishlaringizga mos yo'lovchi chiqsa, sizga darhol xabar beramiz. " +
                "\n\uD83D\uDCDE Qo'shimcha ma'lumotlar uchun +998 99 3972636 ga murojat qilishingiz mumkin" +
                "\n\uD83D\uDE4F @TaxiBekatBot dan foydalanganingiz uchun rahmat. Yo'lingzi bexatar bo'lsin"

        val markup = createReplyKeyboardMarkup(btnMainMenu)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun cancelDriverDetails(update: Update): List<SendMessage> {
        val replyText = "❌ Ma'lumot saqlanmadi" +
                "\n\n bosh menyuga qaytish uchun pasdagi tugmachani bosing \uD83D\uDC47"

        val markup = createReplyKeyboardMarkup(btnStartNewTrip)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun generateTripAnnouncement(): String =
        "\n ${trip.type!!.emoji} ${trip.type!!.nameLatin} " +
        "\n \uD83D\uDDFA ${trip.getTripStartPlace()} - ${trip.getTripEndPlace()} " +
        "\n \uD83D\uDCC5 ${trip.getTripDay()}-${trip.getTripMonth()}-${trip.getTripYear()}" +
        "\n \uD83D\uDCF1 Tel: ${formatPhoneNumber("${user.phone}")}" +
        "\n" +
        "\n #${trip.getTripStartPlace()?.substringBefore(" ")}${trip.getTripEndPlace()?.substringBefore(" ")}${trip.type}" +
        "\n"

    private fun clearVariables() {
        user = TelegramUser()
        trip = Trip()
        driver = Driver()

        clientRoutesCached = emptyList()
        fromRegionNames = emptyList()
        fromSubRegionNames = emptyList()
        toRegionNames = emptyList()
        toSubRegionNames = emptyList()

        taxiFrameRoute = ""
        taxiFrameRoutes = arrayListOf()
        taxiSubregionsToChooseFrom = arrayListOf()
        taxiSubRegionNameSet = arrayListOf()
        taxiRoutesLimit = 2
        carNames = arrayListOf()
        carName = ""
    }

    companion object {
        const val BEAN_PREFIX = "clientMessageService"

        //start
        val btnNeedTaxi = "${TripType.CLIENT.emoji} Menga haydovchi kerak"
        val btnNeedToSendPost = "${TripType.POST.emoji} Pochta jo'natmoqchiman"
        val btnIamTaxi = "${TripType.TAXI.emoji} Men haydovchiman"
        const val btnMainMenu = "\uD83C\uDFE0 Bosh sahifaga qaytish"

        //client route
        const val btnNewClientRoute = "➕ Boshqa yo'nalish"
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

        //channel links and names
        const val CHANNEL_ID_TAXI_BEKAT_TEST = "@taxi_bekat_test_chanel"
        const val CHANNEL_LINK_TAXI_BEKAT_TEST = "https://t.me/taxi_bekat_test_chanel"
    }

}