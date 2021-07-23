package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.FrameRoute
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FrameRouteRepository: JpaRepository<FrameRoute, Long> {

    fun findAllByDeletedFalse(): List<FrameRoute>
}