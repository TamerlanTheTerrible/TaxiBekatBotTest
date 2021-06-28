package me.timur.taxibekatbot.entity

import org.telegram.telegrambots.meta.api.objects.User
import javax.persistence.*

@Entity
@Table(name = "telegram_user")
class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "telegram_id")
    var telegramId: Long? = null

    @Column(name = "username")
    var userName: String? = null

    @Column(name = "last_name")
    var lastName: String? = null

    @Column(name = "first_name")
    var firstName: String? = null

    @Column(name = "phone")
    var phone: String? = null

    constructor(user: User){
        this.telegramId = user.id
        this.userName = user.userName
        this.lastName = user.lastName
        this.firstName = user.firstName
    }

}