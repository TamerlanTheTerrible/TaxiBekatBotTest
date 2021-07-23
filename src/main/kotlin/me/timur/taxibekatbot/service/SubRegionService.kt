package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.exception.DataNotFoundException
import me.timur.taxibekatbot.repository.SubRegionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SubRegionService
@Autowired constructor(
    private val subRegionRepo: SubRegionRepository
){
    fun findAllByNames(names: Collection<String>): ArrayList<SubRegion> {
        val subRegions = ArrayList<SubRegion>()

        names.forEach {
            val subRegion = findByNameLatin(it)
            if (subRegion != null)
                subRegions.add(subRegion)
        }

        return subRegions
    }

    fun findByNameLatin(name: String): SubRegion? {
        return subRegionRepo.findByNameLatin(name)
    }

    fun findMainSubRegion(regionName: String): SubRegion {
        return findAllByRegionNameLatin(regionName)
            .firstOrNull { it.isCenter }
            ?: throw DataNotFoundException("Could not find main sub-region of region $regionName")
    }

    fun findAllByRegionNameLatin(name: String): List<SubRegion> {
        return subRegionRepo.findAllByRegionNameLatin(name)
    }

}