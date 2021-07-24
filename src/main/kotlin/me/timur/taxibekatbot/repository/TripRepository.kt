package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Trip
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.enum.TripType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TripRepository: JpaRepository<Trip, Long> {

    fun findAllByTypeAndTripDateAndFromAndTo(
            type: TripType, date: LocalDate, from: SubRegion, to: SubRegion
    ): List<Trip>

    @Query(nativeQuery = true,
        value = "select \n" +
                "(select sr.name_latin from sub_region sr where sr.id = t.from_sub_region) as from_sub_reg, \n" +
                "(select sr.name_latin from sub_region sr where sr.id = t.to_sub_region) as to_sub_reg\n" +
                "from trip t\n" +
                "where t.user_id = :userId\n" +
                "and t.trip_type = :type\n" +
                "group by t.from_sub_region, t.to_sub_region \n" +
                "order by count(*) desc  \n" +
                "limit 3"
        )
    fun getMostPopularRoutesByUserAndType(userId: Long, type: String): List<Array<Any>>
}