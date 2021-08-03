package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.entity.Trip
import me.timur.taxibekatbot.entity.TripCandidacy
import me.timur.taxibekatbot.enum.TripCandidacyStatus
import me.timur.taxibekatbot.enum.TripStatus
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
import me.timur.taxibekatbot.util.UpdateUtil.sendMessage
import me.timur.taxibekatbot.util.getMessageId
import me.timur.taxibekatbot.util.getStringAfter
import me.timur.taxibekatbot.util.getStringBefore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
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
        private val driverService: DriverService,
        private val tripCandidacyService: TripCandidacyService
): MessageService{
    @Value("\${bot.username}")
    private lateinit var botName: String
    private var user = TelegramUser()
    private var trip = Trip()
    private var driver = Driver()

    private var clientRoutesCached = emptyList<String>()
    private var fromRegionNames = emptyList<String>()
    private var fromSubRegionNames = emptyList<String>()
    private var toRegionNames = emptyList<String>()
    private var toSubRegionNames = emptyList<String>()
    private val ridersQuantityList = listOf("1", "2", "3", "4")

    private var taxiFrameRoute: String = ""
    private var taxiFrameRoutes = arrayListOf<String>()
    private var taxiSubregionsToChooseFrom = arrayListOf<String>()
    private var taxiSubRegionNameSet = arrayListOf<String>()
    private var taxiRoutesLimit = 2
    private var carNames = emptyList<String>()
    private var carName: String = ""


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
                ridersQuantityList.any { it == update.message.text } -> reviewTrip(update)

                //taxi
                update.message.text == btnIamTaxi -> chooseTaxiFrameRoute(update)
                taxiFrameRoutes.any { it == update.message.text } -> chooseFirstTaxiRoute(update)
                taxiSubregionsToChooseFrom.any{ it == update.message.text } -> chooseOtherTaxiRoutes(update)
                carNames.any { it == update.message.text } -> previewDriverData(update)
                update.message.text == btnSaveDriverDetails -> saveDriverDetails(update)
                update.message.text == btnCancelDriverDetails -> cancelDriverDetails(update)

                containsPhone(update) -> if (trip.type == TripType.CLIENT) chooseRidersQuantity(update) else reviewTrip(update)
                else -> listOf(sendMessage(update, "Kutilmagan xatolik"))
                }
            update.hasCallbackQuery() -> when {
                //CLIENT
                update.callbackQuery.data.contains(btnAcceptDriverRequest) -> acceptDriverRequest(update)
                //TAXI
                update.callbackQuery.data.contains(btnAcceptClientRequest) -> acceptClientRequest(update)
                update.callbackQuery.data.contains(btnDenyClientRequest) -> denyClientRequest(update)
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

        return if (clientRoutesCached.isEmpty())
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
        val replyText = "\uD83D\uDCC5 Qachon yo'lga chiqmoqchisiz?"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun requestContact(update: Update): List<SendMessage> {
        val dateInString = update.message.text
        trip.tripDate = if (dateInString == btnToday) LocalDate.now() else LocalDate.now().plusDays(1)

        val button = KeyboardButton("\uD83D\uDCF1Raqamni yuborish", true, false, null)

        val keyboard = KeyboardRow().apply { add(button) }
        val markup = ReplyKeyboardMarkup(listOf(keyboard), true, true, false)

        val replyText = "Telefon raqamingizni kodi bilan kiriting yoki " +
                "\"\uD83D\uDCF1Raqamni yuborish\" tugmachasini bosing"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun chooseRidersQuantity(update: Update): List<BotApiMethod<Message>> {
        telegramUserService.savePhone(update)

        val replyText = "Yo'lovchilar sonini tanlang"
        val markup = createReplyKeyboardMarkup(ridersQuantityList, 4)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun requestLocation(update: Update): List<BotApiMethod<Message>> {
        trip.ridersQuantity = update.message.text.toInt()

        val replyText = "Haydovchi sizni qayerdan olib ketishini xohlaysiz?"

        val buttonPitakdan = KeyboardButton(btnFromPitak)
        val buttonLocation = KeyboardButton("\uD83D\uDCCDHozirgi turgan joyimdan", false, true, null)

        val keyboard = KeyboardRow().apply { add(buttonPitakdan) }.apply { add(buttonLocation) }
        val markup = ReplyKeyboardMarkup(listOf(keyboard), true, true, false)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun reviewTrip(update: Update): List<SendMessage> {
        if (trip.type == TripType.CLIENT)
            trip.ridersQuantity = update.message.text.toInt()

        val replyText = generateTripAnnouncement(trip.id) +
                "\nE’lon berish uchun yoki yo'ovchi qidirish uchun quyidagi botdan foydalaning @$botName"

        val markup = createReplyKeyboardMarkup(btnSaveTrip, btnChangeTrip)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun getLocation(update: Update): String {
        val location = update.message.location
        return if (location == null)
            "pitak"
        else
            "${location.longitude}, ${location.latitude}"
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

        val replyTextClient =
                generateTripAnnouncement(trip.id) +
                "\n\uD83E\uDD1D Mos haydovchi toplishi bilan aloqaga chiqadi" +
                "\n\n\uD83D\uDE4F @TaxiBekatBot dan foydalanganingiz uchun rahmat. Yo'lingzi bexatar bo'lsin"

        val messageToClient = sendMessage(update, replyTextClient, createReplyKeyboardMarkup(btnMainMenu))
        val messageToForward = ForwardMessage(GROUP_ID_TAXI_BEKAT_TEST, getChatId(update), messageId-1)
        val messages = arrayListOf(messageToClient, messageToForward)

        val notificationForDrivers = notifyMatchingDrivers(trip)
        messages.addAll(notificationForDrivers)

        return messages
    }

    private fun notifyMatchingDrivers(newTrip: Trip): List<BotApiMethod<Message>> {
        val drivers = driverService.findAllByMatchingRoute(newTrip).toHashSet()

        val notifications = arrayListOf<BotApiMethod<Message>>()

        drivers.forEach {
            val text = "Quyidagi mijoz haydovchi qidirmoqda. " +
                    "\n ${generateTripAnnouncement(trip.id)}"

            val markup = InlineKeyboardMarkup().apply { this.keyboard = listOf(listOf(
                InlineKeyboardButton(btnAcceptClientRequest).apply { this.callbackData = btnAcceptClientRequest + newTrip.id },
                InlineKeyboardButton(btnDenyClientRequest).apply { this.callbackData = btnDenyClientRequest + newTrip.id },
                ))
            }
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

    private fun acceptDriverRequest(update: Update): List<BotApiMethod<Message>> {
        val tripId = update.callbackQuery.data.substringAfter("trip").toLong()

        val driverId = update.callbackQuery.data.substringAfter(btnAcceptDriverRequest).substringBefore("trip").toLong()
        val driver = driverService.findById(driverId)

        tripService.closeTrip(tripId, driver)

        val replyText = "Сиз #️⃣$tripId ракамли саёхатингизни амалга ошириш учун \uD83C\uDD94 $driverId ракамли хайдовчини танладингиз" +
                "\n\n \uD83D\uDE4F @TaxiBekatBot дан фойдаланганингиз учун рахмат. Йулингиз бехатар булсин"

        val clientMessage = sendMessage(update, replyText, createReplyKeyboardMarkup(btnMainMenu))

        val driverMessage = sendDriverAcceptanceNotification(driver, tripId)

        return listOf(clientMessage, driverMessage)
    }

    private fun sendDriverAcceptanceNotification(driver: Driver, tripId: Long): SendMessage {
        val chatId = driver.telegramUser.chatId
        val replyText = "#️⃣$tripId ракамли эълон буйича суровингизни йуловчи кабул килди" +
                "\n\n \uD83D\uDE4F @TaxiBekatBot дан фойдаланганингиз учун рахмат. Йулингиз бехатар булсин"
        return sendMessage(chatId!!, replyText, createReplyKeyboardMarkup(btnMainMenu))
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

    private fun acceptClientRequest(update: Update): List<BotApiMethod<Message>> {
        val candidacy = generateTripCandidacy(update)

        if (candidacy.trip.status != TripStatus.ACTIVE)
            return sendDriverTripNotActiveNotification(update, candidacy.trip.id!!)

        val messageForDriver = sendDriverAcceptanceAwaitNotification(update)
        val messageForClient = notifyClient(candidacy.driver, candidacy.trip)
        val deleteMsg = DeleteMessage(update.callbackQuery.message.chatId.toString(), candidacy.messageId) as BotApiMethod<Message>

        return listOf(messageForClient, deleteMsg, messageForDriver)
    }

    private fun sendDriverTripNotActiveNotification(update: Update, tripId: Long): List<BotApiMethod<Message>> {
        val replyText = "\uD83C\uDD94 $tripId ракамли эълон учирилди ёки йуловчи тмонидан бошка хайдовчини танланди" +
                "\n\n \uD83E\uDD1D бошкак мос эълон берилиши билан сизга хабар берамиз. Кунингиз хайирли утсин"

        val deleteMsg = DeleteMessage(update.callbackQuery.message.chatId.toString(), update.callbackQuery.message.messageId) as BotApiMethod<Message>
        return listOf(sendMessage(update, replyText), deleteMsg)
    }

    private fun sendDriverAcceptanceAwaitNotification(update: Update): SendMessage {
        val replyText = update.callbackQuery.message.text.substringAfter("Quyidagi mijoz haydovchi qidirmoqda.") +
                "\n\n✅ Siz e'lonni qabul qildingiz va javobingiz mijozga yuborildi" +
                "\n\n\uD83E\uDD1D Mijoz sizni tanlasa, sizga xabar beramiz" +
                "\n\n\uD83D\uDE4F Kuningiz yaxshi o'tsin"
        val markup = createReplyKeyboardMarkup(btnMainMenu)
        return sendMessage(update, replyText, markup)
    }

    private fun notifyClient(driver: Driver, trip: Trip): SendMessage {
        val replyText = "\uD83C\uDD94 ${driver.id} ракамли хайдовчи #️⃣${trip.id} ракамли саёхатингизни амалга оширишга" +
                "\n \uD83D\uDE99 $driver."

        val markup = InlineKeyboardMarkup().apply { this.keyboard = listOf(listOf(
            InlineKeyboardButton(btnAcceptDriverRequest).apply { this.callbackData = btnAcceptDriverRequest + driver.id + "trip" + trip.id },
            InlineKeyboardButton(btnDenyDriverRequest).apply { this.callbackData = btnDenyDriverRequest + driver.id + "trip" + trip.id },
        ))
        }

        val clientChatId = trip.telegramUser!!.chatId!!
        return sendMessage(clientChatId, replyText, markup)
    }

    private fun denyClientRequest(update: Update): List<BotApiMethod<Message>> {
        val tripCandidacy = generateTripCandidacy(update, TripCandidacyStatus.DENIED_BY_DRIVER)

        val replyText = "\n❌ Siz ${tripCandidacy.trip.id} raqamli e'lonni rad etdingiz" +
                "\n\n\uD83E\uDD1D Boshqa mos e'lonlar paydo bo'lsa sizga xabar beramiz" +
                "\n\n\uD83D\uDE4F Kuningiz yaxshi o'tsin"
        val markup = createReplyKeyboardMarkup(btnMainMenu)
        val messageForDriver = sendMessage(update, replyText, markup)

        val deleteMsg = DeleteMessage(update.callbackQuery.message.chatId.toString(), tripCandidacy.messageId) as BotApiMethod<Message>

        return listOf(deleteMsg, messageForDriver)
    }

    private fun generateTripCandidacy(update: Update, status: TripCandidacyStatus = TripCandidacyStatus.ACCEPTED_BY_DRIVER): TripCandidacy {
        val tripId = update.callbackQuery.data.substringAfter(btnDenyClientRequest).toLong()
        val theTrip = tripService.findById(tripId)
        val theDriver = driverService.findDriverByTelegramUser(update.callbackQuery.from.id)
        val messageId = update.callbackQuery.message.messageId

        return tripCandidacyService.save(
                TripCandidacy(theTrip, theDriver, messageId, status)
        )
    }

    //GENERAL METHODS
    private fun generateTripAnnouncement(tripId: Long?): String =
        "#️⃣$tripId raqamli e'lon" +
        "\n\n ${trip.type!!.emoji} ${trip.type!!.nameLatin} " +
        "\n \uD83D\uDDFA ${trip.getTripStartPlace()} - ${trip.getTripEndPlace()} " +
        "\n \uD83D\uDCC5 ${trip.getTripDay()}-${trip.getTripMonth()}-${trip.getTripYear()}" +
        "\n \uD83D\uDC65 Yo'lovchi soni: ${trip.ridersQuantity}" +
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
        val btnNeedTaxi = "${TripType.CLIENT.emoji} Мен йуловчиман"
        val btnNeedToSendPost = "${TripType.POST.emoji} Почта жунатмокчиман"
        val btnIamTaxi = "${TripType.TAXI.emoji} Men haydovchiman"
        const val btnMainMenu = "\uD83C\uDFE0 Bosh sahifaga qaytish"

        //client route
        const val btnNewClientRoute = "➕ Boshqa yo'nalish"
        //date
        const val btnToday = "Bugun"
        const val btnTomorrow = "Ertaga"
        val datesToChoseFrom = listOf(btnToday, btnTomorrow)
        const val btnFromPitak = "Pitakdan"
        //review announcement
        const val btnSaveTrip = "✅ E'lonni joylash"
        const val btnChangeTrip = "✏ E'lonni o'zgartirish"
        //deny trip
        const val btnStartNewTrip = "Ya'ngi e'lon berish"
        //notify-client
        const val btnAcceptDriverRequest = "✅ Haydovchini qabul qilish"
        const val btnDenyDriverRequest = "❌ Haydovchini rad qilish"

        //notify-drivers
        const val btnAcceptClientRequest = "✅ Qabul qilish"
        const val btnDenyClientRequest = "❌ Rad qilish"
        //driver-details
        const val btnSaveDriverDetails = "✅ Saqlash"
        const val btnCancelDriverDetails = "❌ Bekor qilish"

        //channel links and names
        const val CHANNEL_ID_TAXI_BEKAT_TEST = "@taxi_bekat_test_chanel"
        const val CHANNEL_LINK_TAXI_BEKAT_TEST = "https://t.me/taxi_bekat_test_chanel"
        const val GROUP_ID_TAXI_BEKAT_TEST = "-404646626"
        const val GROUP_LINK_TAXI_BEKAT_TEST = "https://t.me/joinchat/InD8kQkNoK02MGVi"
    }

}
