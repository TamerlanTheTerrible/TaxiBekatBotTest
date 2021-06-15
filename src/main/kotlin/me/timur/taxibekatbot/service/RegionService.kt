package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Region
import me.timur.taxibekatbot.repository.RegionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RegionService
@Autowired constructor(
    private val regionRepository: RegionRepository
){

    fun findAll(): List<Region>{
        return regionRepository.findAll()
    }
}