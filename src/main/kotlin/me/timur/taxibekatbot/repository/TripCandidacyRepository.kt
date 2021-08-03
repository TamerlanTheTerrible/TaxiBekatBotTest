/**
 * Created by Timur on 03/08/21
 */

package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.entity.Trip
import me.timur.taxibekatbot.entity.TripCandidacy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TripCandidacyRepository: JpaRepository<TripCandidacy, Long> {

    fun findAllByTrip(trip: Trip): List<TripCandidacy>

    fun findByTripAndDriver(trip: Trip, driver: Driver): TripCandidacy?
}
