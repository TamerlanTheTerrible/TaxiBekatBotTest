package me.timur.taxibekatbot.entity

import javax.persistence.*

@Entity
@Table(name = "route")
class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "driver")
    var driver: TelegramUser? = null

    @ManyToOne
    @JoinColumn(name = "home")
    var home: SubRegion? = null

    @ManyToOne
    @JoinColumn(name = "destination")
    var destination: SubRegion? = null

    constructor(
        home: SubRegion,
        destination: SubRegion
    ){
        this.home = home
        this.destination = destination
    }
}