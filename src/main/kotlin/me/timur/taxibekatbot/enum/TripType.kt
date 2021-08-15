package me.timur.taxibekatbot.enum

enum class TripType(val nameLatin: String, val emoji: String) {
    TAXI("Такси","\uD83D\uDE95"),
    CLIENT("Йуловчи","\uD83D\uDE4B\uD83C\uDFFB\u200D♂"),
    POST("Почта","\uD83D\uDCEC");

    companion object {

        fun findByName(name: String): TripType {
            return when (name) {
                TAXI.name -> TAXI
                CLIENT.name -> CLIENT
                POST.name -> POST
                else -> POST
            }
        }

    }
}
