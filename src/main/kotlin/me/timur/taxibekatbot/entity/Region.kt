package me.timur.taxibekatbot.entity

import javax.persistence.*


@Entity
@Table(name = "region")
class Region() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "name_cyrillic")
    var nameCyrillic: String? = null

    @Column(name = "name_latin")
    var nameLatin: String? = null

    constructor(id: Long?, nameCyrillic: String?, nameLatin: String?):this() {
        this.id = id
        this.nameCyrillic = nameCyrillic
        this.nameLatin = nameLatin
    }
}