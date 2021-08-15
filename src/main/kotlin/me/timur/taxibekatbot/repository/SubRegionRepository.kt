package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.SubRegion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubRegionRepository: JpaRepository<SubRegion, Long> {

    fun findAllByRegionNameLatinOrderByCenterDesc(regionName: String): List<SubRegion>

    fun findByNameLatin(name: String): SubRegion?

}
