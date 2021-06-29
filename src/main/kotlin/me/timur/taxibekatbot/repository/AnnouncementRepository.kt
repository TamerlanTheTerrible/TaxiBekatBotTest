package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Announcement
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.entity.enum.AnnouncementType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AnnouncementRepository: JpaRepository<Announcement, Long> {

    fun findAllByAnnouncementTypeAndTripDateAndFromAndTo(
        type: AnnouncementType, date: LocalDate, from: SubRegion, to: SubRegion
    ): List<Announcement>
}