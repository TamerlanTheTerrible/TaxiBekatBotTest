package me.timur.taxibekatbot.entity

import me.timur.taxibekatbot.enum.DriverStatus
import org.telegram.telegrambots.meta.api.objects.User
import javax.persistence.*

@Entity
@Table(name = "driver")
class Driver() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne
    @JoinColumn(name = "user_id")
    lateinit var telegramUser: TelegramUser

    @OneToOne
    @JoinColumn(name = "car_id")
    lateinit var car: Car

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: DriverStatus = DriverStatus.ACTIVE

    constructor(
        telegramUser: TelegramUser,
        car: Car,
        status: DriverStatus = DriverStatus.ACTIVE
    ):this() {
        this.telegramUser = telegramUser
        this.car = car
        this.status = status
    }

    fun updateDriver(driver: Driver): Driver {
        this.car = driver.car
        return this
    }
}