/**
 * Created by Timur on 03/08/21
 */

package me.timur.taxibekatbot.entity

import me.timur.taxibekatbot.enum.TripCandidacyStatus
import javax.persistence.*

@Entity
@Table(name = "trip_candidance")
class TripCandidacy{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "trip_id")
    var trip: Trip

    @OneToOne
    @JoinColumn(name = "driver")
    var driver: Driver

    @Column(name = "message_id")
    var messageId: Int

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: TripCandidacyStatus

    constructor(
            trip: Trip,
            driver: Driver,
            messageId: Int,
            status: TripCandidacyStatus = TripCandidacyStatus.ACCEPTED_BY_DRIVER){
        this.trip = trip
        this.driver = driver
        this.messageId = messageId
        this.status = status
    }
}
