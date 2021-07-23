package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.entity.Route
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RouteRepository: JpaRepository<Route, Long> {

    fun findAllByDriver(driver: Driver): List<Route>
}