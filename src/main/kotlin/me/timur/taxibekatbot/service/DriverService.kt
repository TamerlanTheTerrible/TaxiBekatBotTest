package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Trip
import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.repository.DriverRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DriverService
@Autowired constructor(
    private val driverRepository: DriverRepository,
    private val telegramUserService: TelegramUserService
){
    fun findAllByMatchingRoute(trip: Trip): List<Driver> {
        return driverRepository.findAllByMatchingRoute(
            trip.from!!.id!!,
            trip.to!!.id!!
        )
    }

    fun saveOrUpdate(driver: Driver): Driver {
        val driverFromDB = driverRepository.findByTelegramUser(driver.telegramUser)

        return if (driverFromDB == null)
            save(driver)
        else{
            driverFromDB.updateDriver(driver)
            save(driverFromDB)
        }
    }

    private fun save(driver: Driver): Driver {
        return driverRepository.save(driver)
    }

}