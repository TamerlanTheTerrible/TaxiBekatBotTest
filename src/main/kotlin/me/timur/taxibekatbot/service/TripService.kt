package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.entity.Trip
import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.entity.TripCandidacy
import me.timur.taxibekatbot.enum.TripCandidacyStatus
import me.timur.taxibekatbot.enum.TripStatus
import me.timur.taxibekatbot.enum.TripType
import me.timur.taxibekatbot.exception.DataNotFoundException
import me.timur.taxibekatbot.repository.TripCandidacyRepository
import me.timur.taxibekatbot.repository.TripRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class TripService
@Autowired constructor(
    private val tripRepository: TripRepository,
    private val tripCandidacyRepository: TripCandidacyRepository
){

    fun findById(tripId: Long): Trip {
        return tripRepository.findByIdOrNull(tripId) ?:
        throw DataNotFoundException("Could not find trip with id $tripId")
    }

    fun getMostPopularRoutesByUserAndType(user: TelegramUser, type: TripType): ArrayList<String> {
        val results = tripRepository.getMostPopularRoutesByUserAndType(user.id!!, type.name)
        val destinations = ArrayList<String>()
        results.forEach {
            destinations.add("${it[0]}-${it[1]}")
        }
        return destinations
    }

    fun closeTrip(trip: Trip, driver: Driver){
        trip.status = TripStatus.NOT_ACTIVE
        trip.driver = driver
        save(trip)

        denyOtherTripCandidacies(trip, driver)
    }

    private fun denyOtherTripCandidacies(trip: Trip, driver: Driver): ArrayList<TripCandidacy> {
        val deniedCandidates = ArrayList<TripCandidacy>()
        val tripCandidates = tripCandidacyRepository.findAllByTrip(trip)
        tripCandidates.forEach {
            if (it.driver.id == driver.id)
                it.status = TripCandidacyStatus.ACCEPTED_BY_CLIENT

            it.status = TripCandidacyStatus.DENIED_BY_CLIENT
            deniedCandidates.add(it)
        }
        return deniedCandidates
    }

    fun save(trip: Trip): Trip {
        return tripRepository.save(trip)
    }

    fun saveCandidacy(tripCandidacy: TripCandidacy): TripCandidacy {
        return tripCandidacyRepository.save(tripCandidacy)
    }

}
