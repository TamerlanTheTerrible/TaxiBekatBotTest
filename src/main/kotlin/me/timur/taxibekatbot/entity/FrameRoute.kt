package me.timur.taxibekatbot.entity

import javax.persistence.*

@Entity
@Table(name="frame_route")
class FrameRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "home")
    var home: Region? = null

    @ManyToOne
    @JoinColumn(name = "destination")
    var destination: Region? = null

    @Column(name="deleted")
    var deleted: Boolean = false
}