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

    @Column(name = "driver_message_id")
    var driverMessageId: Int

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: TripCandidacyStatus

    @Column(name = "client_message_id")
    var clientMessageId: Int? = null

    constructor(
            trip: Trip,
            driver: Driver,
            messageId: Int,
            status: TripCandidacyStatus = TripCandidacyStatus.ACCEPTED_BY_DRIVER){
        this.trip = trip
        this.driver = driver
        this.driverMessageId = messageId
        this.status = status
    }
}
