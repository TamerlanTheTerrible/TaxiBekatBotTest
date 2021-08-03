/**
 * Created by Timur on 03/08/21
 */

package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.TripCandidacy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TripCandidacyRepository: JpaRepository<TripCandidacy, Long> {
}
