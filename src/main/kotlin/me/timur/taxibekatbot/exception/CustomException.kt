package me.timur.taxibekatbot.exception

import java.lang.RuntimeException

open class CustomException(message: String): RuntimeException(message)

class InvalidInputException(message: String): CustomException(message)

class DataNotFoundException(message: String): CustomException(message)

class TripClosedException(message: String? = null): CustomException(message ?: "E'lon yopilgan")
