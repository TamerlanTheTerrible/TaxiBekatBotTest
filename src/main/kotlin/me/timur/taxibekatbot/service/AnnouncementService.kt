package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Announcement
import me.timur.taxibekatbot.entity.enum.AnnouncementType
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
            type = if(anc.announcementType!! == AnnouncementType.TAXI) AnnouncementType.CLIENT else AnnouncementType.TAXI,
            date = anc.tripDate!!,
            from = anc.from!!,
            to = anc.to!!
        )
    }
}