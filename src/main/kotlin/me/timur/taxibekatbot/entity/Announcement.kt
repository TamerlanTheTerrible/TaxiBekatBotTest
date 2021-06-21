package me.timur.taxibekatbot.entity

import me.timur.taxibekatbot.entity.enum.AnnouncementType
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "announcement")
class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "date_created")
    var dateCreated: LocalDateTime? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "look_for")
    var announcementType: AnnouncementType? = null

    @Column(name = "trip_date")
    var tripDate: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "from_sub_region")
    var from: SubRegion? = null

    @ManyToOne
    @JoinColumn(name = "to_sub_region")
    var to: SubRegion? = null

    @ManyToOne
    @JoinColumn(name = "user")
    var telegramUser: TelegramUser? = null

    constructor(
        announcementType: AnnouncementType?,
        tripDate: LocalDate?,
        from: SubRegion?,
        to: SubRegion?,
        telegramUser: TelegramUser?
    ) {
        this.announcementType = announcementType
        this.tripDate = tripDate
        this.from = from
        this.to = to
        this.telegramUser = telegramUser
    }

    override fun toString(): String {
        return "Announcement(id=$id, dateCreated=$dateCreated, announcementType=$announcementType, tripDate=$tripDate, from=$from, to=$to, telegramUser=$telegramUser)"
    }


}