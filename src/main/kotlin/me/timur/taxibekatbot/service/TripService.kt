package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Trip
import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.enum.TripType
import me.timur.taxibekatbot.repository.TripRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TripService
@Autowired constructor(
    private val tripRepository: TripRepository
){
    fun save(trip: Trip): Trip {
        return tripRepository.save(trip)
    }

    fun matchAnnouncement(anc: Trip): List<Trip> {
        return tripRepository.findAllByTypeAndTripDateAndFromAndTo(
            type = anc.type!!,
            date = anc.tripDate!!,
            from = anc.from!!,
            to = anc.to!!
        )
    }

    fun getMostPopularRoutesByUserAndType(
        user: TelegramUser,
        type: TripType
    ): ArrayList<String> {
        val results = tripRepository.getMostPopularRoutesByUserAndType(user.id!!, type.name)
        val destinations = ArrayList<String>()
        results.forEach {
            destinations.add("${it[0]}-${it[1]}")
        }
        return destinations
    }

}