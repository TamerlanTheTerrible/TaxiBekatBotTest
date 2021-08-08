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
import me.timur.taxibekatbot.util.UpdateUtil.sendMessage
import me.timur.taxibekatbot.util.getMessageId
import me.timur.taxibekatbot.util.getStringAfter
import me.timur.taxibekatbot.util.getStringBefore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
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
        private val driverService: DriverService
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
                update.callbackQuery.data.contains(btnDenyDriverRequest) -> denyDriverRequest(update)
                //TAXI
                update.callbackQuery.data.contains(btnAcceptClientRequest) -> acceptClientRequest(update)
                update.callbackQuery.data.contains(btnDenyClientRequest) -> denyClientRequest(update)
                else -> listOf(sendMessage(update, "Кутилмаган хатолик"))
            }
            else -> listOf(sendMessage(update, "Кутилмаган хатолик"))
        }

        return messages
    }

    //CLIENT
    private fun commandStart(update: Update): List<SendMessage> {
        clearVariables()
        user = telegramUserService.saveUser(update)

        val markup = createReplyKeyboardMarkup(btnNeedTaxi, btnNeedToSendPost, btnIamTaxi)
        val responseText = "Ассалому алайкум. Нима кидираяпсиз?"
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
            listOf(sendMessage(update, "\uD83D\uDDFA Йуналишни танланг", markup))
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
        val replyText = "\uD83D\uDCC5 Качон йулга чикмокчисиз?"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun requestContact(update: Update): List<SendMessage> {
        val dateInString = update.message.text
        trip.tripDate = if (dateInString == btnToday) LocalDate.now() else LocalDate.now().plusDays(1)

        val button = KeyboardButton("\uD83D\uDCF1Ракамни юбориш", true, false, null)

        val keyboard = KeyboardRow().apply { add(button) }
        val markup = ReplyKeyboardMarkup(listOf(keyboard), true, true, false)

        val replyText = "Телефон ракамингизни тулик коди билан киритинг ёки " +
                "\"\uD83D\uDCF1Ракамни юбориш\" тугмасини босинг"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun chooseRidersQuantity(update: Update): List<BotApiMethod<Message>> {
        telegramUserService.savePhone(update)

        val replyText = "Йуловчилар сонини танланг"
        val markup = createReplyKeyboardMarkup(ridersQuantityList, 4)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun requestLocation(update: Update): List<BotApiMethod<Message>> {
        trip.ridersQuantity = update.message.text.toInt()

        val replyText = "Хайдовчи сизни каердан олиб кетишини хохлайсиз?"

        val buttonPitakdan = KeyboardButton(btnFromPitak)
        val buttonLocation = KeyboardButton("\uD83D\uDCCD Хозирги турган жойимдан", false, true, null)

        val keyboard = KeyboardRow().apply { add(buttonPitakdan) }.apply { add(buttonLocation) }
        val markup = ReplyKeyboardMarkup(listOf(keyboard), true, true, false)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun reviewTrip(update: Update): List<SendMessage> {
        if (trip.type == TripType.CLIENT)
            trip.ridersQuantity = update.message.text.toInt()

        val replyText = generateTripAnnouncement(trip.id) +
                "\nЭълон бериш учун ёки йуловчи кидириш учун куйидаги ботдан фойдаланинг @$botName"

        val markup = createReplyKeyboardMarkup(btnSaveTrip, btnChangeTrip)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun getLocation(update: Update): String {
        val location = update.message.location
        return if (location == null)
            "питак"
        else
            "${location.longitude}, ${location.latitude}"
    }

    private fun denyTrip(update: Update): List<SendMessage> {
        val markup = createReplyKeyboardMarkup(btnStartNewTrip)
        val replyText = "❌ Узгаришлар бекор килинди" +
                "\n\n Янги эълон бериш учун пастдаги тугмани босинг \uD83D\uDC47"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun saveTrip(update: Update): List<BotApiMethod<Message>> {
        trip.telegramMessageId = update.getMessageId()
        trip.telegramUser = user
        tripService.save(trip)

        val replyTextClient =
                "#️⃣${trip.id} ракамли эълонингиз жойланди" +
                "\n\n\uD83E\uDD1D Мос хайдовчи топилиши билан сизга хабар берамиз" +
                "\n\n\uD83D\uDE4F @TaxiBekatBot дан фойдаланганингиз учун рахмат. Йулингиз бехатар булсин"
        val messageToClient = sendMessage(update, replyTextClient, createReplyKeyboardMarkup(btnMainMenu))

        val forwardTextGroup = generateTripAnnouncement(trip.id) +
                "\nЭълон бериш учун ёки йуловчи кидириш учун куйидаги ботдан фойдаланинг @$botName"

        val messageToForward = sendMessage(GROUP_ID_TAXI_BEKAT_TEST, forwardTextGroup)
        val messages: ArrayList<BotApiMethod<Message>> = arrayListOf(messageToClient, messageToForward)

        val notificationForDrivers = notifyMatchingDrivers(trip)
        messages.addAll(notificationForDrivers)

        return messages
    }

    private fun notifyMatchingDrivers(newTrip: Trip): List<BotApiMethod<Message>> {
        val drivers = driverService.findAllByMatchingRoute(newTrip).toHashSet()

        val notifications = arrayListOf<BotApiMethod<Message>>()

        drivers.forEach {
            val text = "Куйидаги мижоз хайдовчи кидирмокда: " +
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

        return listOf(sendMessage(update, "Кайси вилоятдан", replyMarkup))
    }

    private fun chooseFromSubRegion(update: Update): List<SendMessage> {
        val regionName = update.message.text.substringAfter("\uD83D\uDD34 ")
        fromSubRegionNames = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDD34 ${it.nameLatin}" }
        val replyMarkup = createReplyKeyboardMarkup(fromSubRegionNames)

        return listOf(sendMessage(update, "Кайси шахар/тумандан", replyMarkup))
    }

    private fun chooseToRegion(update: Update): List<SendMessage> {
        val sebRegionName = update.message.text.substringAfter("\uD83D\uDD34 ")
        trip.from = subRegionService.findByNameLatin(sebRegionName)

        toRegionNames = regionRepository.findAll().map { "\uD83D\uDD35 ${it.nameLatin!!}"}
        val replyMarkup = createReplyKeyboardMarkup(toRegionNames)

        return listOf(sendMessage(update, "Кайси вилоятга", replyMarkup))
    }

    private fun chooseToSubRegion(update: Update): List<SendMessage> {
        val regionName = update.message.text.substringAfter("\uD83D\uDD35 ")
        toSubRegionNames = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDD35 ${it.nameLatin}" }
        val replyMarkup = createReplyKeyboardMarkup(toSubRegionNames)

        return listOf(sendMessage(update, "Кайси шахар туманга", replyMarkup))
    }

    private fun acceptDriverRequest(update: Update): List<BotApiMethod<Message>> {
        val tripId = update.callbackQuery.data.substringAfter("trip").toLong()
        val trip = tripService.findById(tripId)

        val driverId = update.callbackQuery.data.substringAfter(btnAcceptDriverRequest).substringBefore("trip").toLong()
        val driver = driverService.findById(driverId)

        val deniedCandidacies = tripService.closeTripAndReturnDeniedCandidacies(trip, driver)
        val deniedDriversMessages = notifyDeniedDrivers(deniedCandidacies)

        val acceptedDriverMessage = sendDriverAcceptanceNotification(driver, tripId)

        val msgListToDelete = deleteNotifications(update, deniedDriversMessages)
        val clientMessage = generateAfterTripCloseMessage(trip, driver)

        return arrayListOf<BotApiMethod<Message>>(clientMessage, acceptedDriverMessage)
            .apply { addAll(deniedDriversMessages) }.apply { addAll(msgListToDelete) }
    }
//    TODO("Remove deleteNotifications logic, instead unable accept another driver, if trip is closed")
    private fun deleteNotifications(update: Update, deniedDriversMessages: List<BotApiMethod<Message>>): List<BotApiMethod<Message>> {
        val chatId = update.callbackQuery.message.chatId.toString()
        val messageId = update.callbackQuery.message.messageId
        var messagesAmount = deniedDriversMessages.size
        val messagesToDelete = arrayListOf<BotApiMethod<Message>>()

        while (messagesAmount > -1) {
            val messageToDelete = DeleteMessage(chatId, messageId-messagesAmount) as BotApiMethod<Message>
            messagesToDelete.add(messageToDelete)
            messagesAmount--
        }

        return messagesToDelete
    }

    private fun notifyDeniedDrivers(deniedCandidacies: ArrayList<TripCandidacy>): List<BotApiMethod<Message>> {
        val messages = arrayListOf<BotApiMethod<Message>>()

        deniedCandidacies.forEach {
            messages.addAll(notifyDeniedDrivers(it.driver, it.trip))
        }

        return messages
    }

    private fun generateAfterTripCloseMessage(trip: Trip, driver: Driver): SendMessage {
        val replyText =
            "Сиз #️⃣${trip.id} ракамли саёхатингизни амалга ошириш учун \uD83C\uDD94 ${driver.id} ракамли хайдовчини танладингиз" +
                    "\n\n \uD83D\uDE4F @TaxiBekatBot дан фойдаланганингиз учун рахмат. Йулингиз бехатар булсин"

        return sendMessage(trip.telegramUser!!.chatId!!, replyText, createReplyKeyboardMarkup(btnMainMenu))
    }

    private fun sendDriverAcceptanceNotification(driver: Driver, tripId: Long): SendMessage {
        val chatId = driver.telegramUser.chatId
        val replyText = "#️⃣$tripId ракамли эълон буйича суровингизни йуловчи кабул килди" +
                "\n\n \uD83D\uDE4F @TaxiBekatBot дан фойдаланганингиз учун рахмат. Йулингиз бехатар булсин"
        return sendMessage(chatId!!, replyText, createReplyKeyboardMarkup(btnMainMenu))
    }

    private fun denyDriverRequest(update: Update): List<BotApiMethod<Message>> {
        val tripId = update.callbackQuery.data.substringAfter("trip").toLong()
        val trip = tripService.findById(tripId)

        val driverId = update.callbackQuery.data.substringAfter(btnDenyDriverRequest).substringBefore("trip").toLong()
        val driver = driverService.findById(driverId)

        return notifyDeniedDrivers(driver, trip)
    }

    private fun notifyDeniedDrivers(driver: Driver, trip: Trip): List<BotApiMethod<Message>> {
        val replyText = "Афсуски #️⃣${trip.id} ракамли саёхатни амалга ошириш учун йуловчи бошка хайдовчини танлади" +
                "\n\n Яна мос эълонлар берилиши билан сизга хабар берамиз"

        val notification = sendMessage(driver.telegramUser.chatId!!, replyText, createReplyKeyboardMarkup(btnMainMenu))

//        val tripCandidacy = tripService.findTripCandidacyByDriver(trip, driver)
//        val deleteMessage = DeleteMessage(driver.telegramUser.chatId!!, tripCandidacy.messageId) as BotApiMethod<Message>
        return listOf(notification)
    }


    //TAXI
    private fun chooseTaxiFrameRoute(update: Update): List<BotApiMethod<Message>> {
        val frameRoutes = frameRouteRepository.findAll()

        frameRoutes.forEach {
            val home = it.home!!.nameLatin
            val destination = it.destination!!.nameLatin
            taxiFrameRoutes.add("$home-$destination")
        }

        val markup = createReplyKeyboardMarkup(taxiFrameRoutes)
        return listOf(sendMessage(update, "\uD83D\uDDFA Кайси йуналишда катнайсиз", markup))
    }

    private fun chooseFirstTaxiRoute(update: Update): List<BotApiMethod<Message>> {
        taxiFrameRoute = update.message.text
        val regionName = taxiFrameRoute.substringBefore("-")
        taxiSubregionsToChooseFrom = subRegionService.findAllByRegionNameLatin(regionName).map { "\uD83D\uDFE2 ${it.nameLatin}" } as ArrayList<String>

        val markup = createReplyKeyboardMarkup(taxiSubregionsToChooseFrom)
        val text = "Узингиз катнайдиган шахар/туманни" +
                "\n\n $taxiRoutesLimit та танлашингиз мумкин"

        return listOf(sendMessage(update, text, markup))
    }

    private fun chooseOtherTaxiRoutes(update: Update): List<BotApiMethod<Message>> {
        val subRegionName = update.getStringAfter("\uD83D\uDFE2 ")
        taxiSubRegionNameSet.add(subRegionName)

        if (--taxiRoutesLimit == 0)
            return chooseTaxiCar(update)

        taxiSubregionsToChooseFrom.remove(update.message.text)

        val markup = createReplyKeyboardMarkup(taxiSubregionsToChooseFrom)
        val text = "Узингиз катнайдиган шахар/туманни" +
                "\n\n яна $taxiRoutesLimit та танлашингиз мумкин"

        return listOf(sendMessage(update, text, markup))
    }

    private fun chooseTaxiCar(update: Update): List<BotApiMethod<Message>> {
        carNames = carRepository.findAll().map { it.nameLatin!! }
        val markup = createReplyKeyboardMarkup(carNames)
        val replyText = "Мошинангиз русумини танланг"

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun previewDriverData(update: Update): List<BotApiMethod<Message>> {
        carName = update.message.text

        val replyText = "Агар маълумотлар тугри булса саклаш тугмасини босинг:" +
                "\n\n\uD83D\uDEE3 Асосий маршрут: $taxiFrameRoute" +
                "\n\n\uD83C\uDF07 Сиз катнайдиган шахар/туманлар: ${taxiSubRegionNameSet.toString().substringAfter("[").substringBefore("]")}" +
                "\n\n\uD83D\uDE98 Мошинангиз русуми: $carName"

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

        val replyText = "✅ Маълумотлар сакланди. " +
                "\n\uD83E\uDD1D Йуналишларингизга мос йуловчи чикса сизга дархол хаьар берамиз." +
                "\n\uD83D\uDCDE Кушимча маълумот учун +998 99 3972636 га мурожат килишингиз мумкин." +
                "\n\uD83D\uDE4F @TaxiBekatBot дан фойдаланганингиз учун рахмат. Йулингиз бехатар булсин!"

        val markup = createReplyKeyboardMarkup(btnMainMenu)

        return listOf(sendMessage(update, replyText, markup))
    }

    private fun cancelDriverDetails(update: Update): List<SendMessage> {
        val replyText = "❌ Маълумотлар сакланмади" +
                "\n\n Бош сахифага кайтиш учун пастдаги тугмани босинг \uD83D\uDC47"

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
        val replyText = update.callbackQuery.message.text.substringAfter("Куйидаги мижоз хайдовчи кидирмокда: ") +
                "\n\n✅ Эълонни кабул килганлигингиз хакида мижозга хабар юборилди" +
                "\n\n\uD83E\uDD1D Мижоз сизни танласа сизга хабар берамиз" +
                "\n\n\uD83D\uDE4F Кунингиз хайирли утсин"
        val markup = createReplyKeyboardMarkup(btnMainMenu)
        return sendMessage(update, replyText, markup)
    }

    private fun notifyClient(driver: Driver, trip: Trip): SendMessage {
        val replyText = "\uD83C\uDD94 ${driver.id} ракамли хайдовчи #️⃣${trip.id} ракамли саёхатингизни амалга оширишга тайёр" +
                "\n \uD83D\uDE99 ${driver.car.nameLatin}" +
                "\n \uD83D\uDCDE ${driver.telegramUser.phone}"

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

        val replyText = "\n❌ Сиз ${tripCandidacy.trip.id} ракамли эълонни рад этдингиз" +
                "\n\n\uD83E\uDD1D Бошка мос эълонлар пайдо булиши билан сизга хабар берамиз" +
                "\n\n\uD83D\uDE4F Кунингиз хайирли утсин"
        val markup = createReplyKeyboardMarkup(btnMainMenu)
        val messageForDriver = sendMessage(update, replyText, markup)

        val deleteMsg = DeleteMessage(update.callbackQuery.message.chatId.toString(), tripCandidacy.messageId) as BotApiMethod<Message>

        return listOf(deleteMsg, messageForDriver)
    }

    private fun generateTripCandidacy(update: Update, status: TripCandidacyStatus = TripCandidacyStatus.ACCEPTED_BY_DRIVER): TripCandidacy {
        val stringAfter = if (status == TripCandidacyStatus.ACCEPTED_BY_DRIVER)
            btnAcceptClientRequest
        else
            btnDenyClientRequest

        val tripId = update.callbackQuery.data.substringAfter(stringAfter).toLong()
        val theTrip = tripService.findById(tripId)
        val theDriver = driverService.findDriverByTelegramUser(update.callbackQuery.from.id)
        val messageId = update.callbackQuery.message.messageId

        return tripService.saveCandidacy(
                TripCandidacy(theTrip, theDriver, messageId, status)
        )
    }

    //GENERAL METHODS
    private fun generateTripAnnouncement(tripId: Long?): String {
        var announcementMessage = if(tripId != null) "#️⃣$tripId ракамли эълон" else ""

        announcementMessage += "\n\n ${trip.type!!.emoji} ${trip.type!!.nameLatin} " +
                "\n \uD83D\uDDFA ${trip.getTripStartPlace()} - ${trip.getTripEndPlace()} " +
                "\n \uD83D\uDCC5 ${trip.getTripDay()}-${trip.getTripMonth()}-${trip.getTripYear()}" +
                "\n \uD83D\uDC65 Йуловчилар сони: ${trip.ridersQuantity}" +
                "\n \uD83D\uDCF1 Тел: ${formatPhoneNumber("${user.phone}")}" +
                "\n" +
                "\n #${trip.getTripStartPlace()?.substringBefore(" ")}${trip.getTripEndPlace()?.substringBefore(" ")}${trip.type}" +
                "\n"

        return announcementMessage
    }


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
        val btnIamTaxi = "${TripType.TAXI.emoji} Мен хайдовчиман"
        const val btnMainMenu = "\uD83C\uDFE0 Бош сахифага кайтиш"

        //client route
        const val btnNewClientRoute = "➕ Бошка йуналиш"
        //date
        const val btnToday = "Бугун"
        const val btnTomorrow = "Эртага"
        val datesToChoseFrom = listOf(btnToday, btnTomorrow)
        const val btnFromPitak = "Питакдан"
        //review announcement
        const val btnSaveTrip = "✅ Эълонни жойлаш"
        const val btnChangeTrip = "✏ Эълонни узгартириш"
        //deny trip
        const val btnStartNewTrip = "Янги эълон бериш"
        //notify-client
        const val btnAcceptDriverRequest = "✅ Хайдовчини кабул килиш"
        const val btnDenyDriverRequest = "❌ Хайдовчини рад этиш"

        //notify-drivers
        const val btnAcceptClientRequest = "✅ Кабул килиш"
        const val btnDenyClientRequest = "❌ Рад этиш"
        //driver-details
        const val btnSaveDriverDetails = "✅ Саклаш"
        const val btnCancelDriverDetails = "❌ Бекор килиш"

        //channel links and names
        const val CHANNEL_ID_TAXI_BEKAT_TEST = "@taxi_bekat_test_chanel"
        const val CHANNEL_LINK_TAXI_BEKAT_TEST = "https://t.me/taxi_bekat_test_chanel"
        const val GROUP_ID_TAXI_BEKAT_TEST = "-404646626"
        const val GROUP_LINK_TAXI_BEKAT_TEST = "https://t.me/joinchat/InD8kQkNoK02MGVi"
    }

}
