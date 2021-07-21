package me.timur.taxibekatbot.entity

import javax.persistence.*

@Entity
@Table(name = "car")
class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "name_latin")
    var nameLatin: String? = null
}