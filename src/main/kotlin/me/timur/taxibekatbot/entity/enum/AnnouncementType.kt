package me.timur.taxibekatbot.entity.enum

enum class AnnouncementType {
    TAXI,
    CLIENT,
    POST;

    companion object {

        fun findByName(name: String): AnnouncementType {
            return when (name) {
                TAXI.name -> TAXI
                CLIENT.name -> CLIENT
                else -> POST
            }
        }

    }
}
