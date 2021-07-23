package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Announcement
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.enum.AnnouncementType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AnnouncementRepository: JpaRepository<Announcement, Long> {

    fun findAllByAnnouncementTypeAndTripDateAndFromAndTo(
        type: AnnouncementType, date: LocalDate, from: SubRegion, to: SubRegion
    ): List<Announcement>

    @Query(nativeQuery = true,
        value = "select \n" +
                "(select sr.name_latin from sub_region sr where sr.id = a.from_sub_region) as from_sub_reg, \n" +
                "(select sr.name_latin from sub_region sr where sr.id = a.to_sub_region) as to_sub_reg\n" +
                "from announcement a\n" +
                "where a.user_id = :userId\n" +
                "and a.look_for = :lookFor\n" +
                "group by a.from_sub_region, a.to_sub_region \n" +
                "order by count(*) desc  \n" +
                "limit 3"
        )
    fun getMostPopularRoutesByUserAndAnnouncementType(userId: Long, lookFor: String): List<Array<Any>>
}