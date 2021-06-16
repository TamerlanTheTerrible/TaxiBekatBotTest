package me.timur.taxibekatbot.entity

import javax.persistence.*

@Entity
@Table(name = "sub_region")
class SubRegion() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "name_latin")
    var nameLatin: String? = null

    @Column(name = "name_cyrillic")
    var nameCyrillic: String? = null

    @ManyToOne
    @JoinColumn(name = "region_id")
    var region: Region? = null

}