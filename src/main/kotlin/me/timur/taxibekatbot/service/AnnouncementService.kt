package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Announcement
import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.enum.AnnouncementType
import me.timur.taxibekatbot.repository.AnnouncementRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AnnouncementService
@Autowired constructor(
    private val announcementRepository: AnnouncementRepository
){
    fun save(announcement: Announcement): Announcement {
        return announcementRepository.save(announcement)
    }

    fun matchAnnouncement(anc: Announcement): List<Announcement> {
        return announcementRepository.findAllByAnnouncementTypeAndTripDateAndFromAndTo(
            type = anc.announcementType!!,
            date = anc.tripDate!!,
            from = anc.from!!,
            to = anc.to!!
        )
    }

    fun getMostPopularRoutesByUserAndAnnouncementType(
        user: TelegramUser,
        type: AnnouncementType
    ): ArrayList<String> {
        val results = announcementRepository.getMostPopularRoutesByUserAndAnnouncementType(user.id!!, type.name)
        val destinations = ArrayList<String>()
        results.forEach {
            destinations.add("${it[0]}-${it[1]}")
        }
        return destinations
    }

}