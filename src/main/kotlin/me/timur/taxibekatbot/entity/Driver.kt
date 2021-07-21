package me.timur.taxibekatbot.entity

import org.telegram.telegrambots.meta.api.objects.User
import javax.persistence.*

@Entity
@Table(name = "driver")
class Driver() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}