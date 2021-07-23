package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Car
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CarRepository: JpaRepository<Car, Long> {

    fun findByNameLatin(name: String): Car?
}