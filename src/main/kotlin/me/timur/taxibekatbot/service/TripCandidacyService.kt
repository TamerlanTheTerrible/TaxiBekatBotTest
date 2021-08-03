/**
 * Created by Timur on 03/08/21
 */

package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.TripCandidacy
import me.timur.taxibekatbot.repository.TripCandidacyRepository
import org.jvnet.hk2.annotations.Service
import org.springframework.beans.factory.annotation.Autowired

@Service
class TripCandidacyService
@Autowired constructor(
        private val tripCandidacyRepository: TripCandidacyRepository
        ){

    fun save(tripCandidacy: TripCandidacy): TripCandidacy {
        return tripCandidacyRepository.save(tripCandidacy)
    }


}
