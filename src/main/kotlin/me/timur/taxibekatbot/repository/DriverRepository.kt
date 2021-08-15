package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.entity.TelegramUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DriverRepository: JpaRepository<Driver, Long> {

    fun findByTelegramUser(user: TelegramUser): Driver?

    @Query(
        nativeQuery = true,
        value = "SELECT d.*\n" +
                "FROM driver d \n" +
                "JOIN route r ON r.driver = d.id \n" +
                "JOIN car c ON c.id = d.car_id \n" +
                "WHERE d.status IN ('ACTIVE', 'FREE_PERIODED')\n" +
                "AND r.deleted = FALSE \n" +
                "AND ((r.home = :fromSubregionId AND r.destination = :toSubregionId) OR (r.home = :toSubregionId AND r.destination = :fromSubregionId))" +
                "AND :preferredCars like CONCAT('%', c.name_latin, '%')"
    )
    fun findAllByMatchingRoute(fromSubregionId: Long, toSubregionId: Long, preferredCars: String): List<Driver>
}