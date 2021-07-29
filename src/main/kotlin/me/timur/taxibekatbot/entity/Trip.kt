package me.timur.taxibekatbot.entity

import me.timur.taxibekatbot.enum.TripStatus
import me.timur.taxibekatbot.enum.TripType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import javax.persistence.*

@Entity
@Table(name = "trip")
class Trip() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "date_created")
    var dateCreated: LocalDateTime? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type")
    var type: TripType? = null

    @Column(name = "trip_date")
    var tripDate: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "from_sub_region")
    var from: SubRegion? = null

    @ManyToOne
    @JoinColumn(name = "to_sub_region")
    var to: SubRegion? = null

    @Column(name = "riders_quantity")
    var ridersQuantity: Int = 0

    @ManyToOne
    @JoinColumn(name = "user_id")
    var telegramUser: TelegramUser? = null

    @Column(name = "telegram_message_id")
    var telegramMessageId: Int? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: TripStatus = TripStatus.ACTIVE

    @ManyToOne
    @JoinColumn(name = "confirmed_driver")
    var driver: Driver? = null


    constructor(
            tripType: TripType?,
            tripDate: LocalDate?,
            from: SubRegion?,
            to: SubRegion?,
            telegramUser: TelegramUser?,
            messageId: Int
    ):this() {
        this.type = tripType
        this.tripDate = tripDate
        this.from = from
        this.to = to
        this.telegramUser = telegramUser
        this.telegramMessageId = messageId
    }

    @Transient
    fun getTripStartPlace(): String? = from?.nameLatin
    @Transient
    fun getTripEndPlace(): String? = to?.nameLatin
    @Transient
    fun getClientPhone(): String? = telegramUser?.phone
    @Transient
    fun getTripDay(): Int? = tripDate?.dayOfMonth
    @Transient
    fun getTripMonth(): Month? = tripDate?.month
    @Transient
    fun getTripYear(): Int? = tripDate?.year

    override fun toString(): String {
        return "Announcement(id=$id, dateCreated=$dateCreated, announcementType=$type, tripDate=$tripDate, from=$from, to=$to, telegramUser=$telegramUser)"
    }


}