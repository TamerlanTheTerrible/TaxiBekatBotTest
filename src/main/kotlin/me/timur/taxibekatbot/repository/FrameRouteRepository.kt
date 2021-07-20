package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.FrameRoute
import org.springframework.data.jpa.repository.JpaRepository

interface FrameRouteRepository: JpaRepository<FrameRoute, Long> {

    fun findAllByDeletedFalse(): List<FrameRoute>
}