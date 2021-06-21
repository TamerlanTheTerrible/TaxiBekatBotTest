package me.timur.taxibekatbot.entity

import javax.persistence.*

@Entity
@Table(name = "telegram_user")
class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "username")
    var userName: String? = null

    @Column(name = "last_name")
    var lastName: String? = null

    @Column(name = "first_name")
    var firstName: String? = null
    override fun toString(): String {
        return "TelegramUser(id=$id, userName=$userName, lastName=$lastName, firstName=$firstName)"
    }


}