package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RegionRepository: JpaRepository<Region, Long> {
}