package me.timur.taxibekatbot.exception

import java.lang.RuntimeException

class CustomException

class InvalidInputException(message: String): RuntimeException() {
}