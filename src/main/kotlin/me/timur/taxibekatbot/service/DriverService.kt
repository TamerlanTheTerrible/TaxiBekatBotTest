package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Driver
import me.timur.taxibekatbot.entity.TelegramUser
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