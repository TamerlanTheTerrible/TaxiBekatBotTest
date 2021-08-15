package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Trip
import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.exception.DataNotFoundException
import me.timur.taxibekatbot.repository.DriverRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class DriverService
@Autowired constructor(
    private val driverRepository: DriverRepository,
    private val telegramUserService: TelegramUserService
){

    fun findDriverByTelegramUser(userId: Long): Driver {
        val user = telegramUserService.findByTelegramId(userId)
        return driverRepository.findByTelegramUser(user) ?:
        throw DataNotFoundException("Could not find driver by TelegramUser ${user.id}")
    }

    fun getDriverChat(driverId: Long): String {
        val driver = findById(driverId)
        val user = telegramUserService.findByTelegramId(driver.telegramUser.id!!)
        return user.chatId ?:
        throw DataNotFoundException("Could not find chat for driver $driverId")
    }

    fun findById(id: Long): Driver {
        return driverRepository.findByIdOrNull(id) ?:
        throw DataNotFoundException("Could not find driver by id $id")
    }

    fun findAllByMatchingRoute(trip: Trip): List<Driver> {
        return driverRepository.findAllByMatchingRoute(
            trip.from!!.id!!,
            trip.to!!.id!!,
            trip.preferredCars!!
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