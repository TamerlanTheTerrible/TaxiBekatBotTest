package me.timur.taxibekatbot.enum

enum class AnnouncementType(val nameLatin: String, val emoji: String) {
    TAXI("Taksi","\uD83D\uDE95"),
    CLIENT("Yo'lovchi","\uD83D\uDE4B\uD83C\uDFFB\u200D♂"),
    POST("Pochta","\uD83D\uDCEC");

    companion object {

        fun findByName(name: String): AnnouncementType {
            return when (name) {
                TAXI.name -> TAXI
                CLIENT.name -> CLIENT
                POST.name -> POST
                else -> POST
            }
        }

    }
}
