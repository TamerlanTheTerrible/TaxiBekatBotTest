package me.timur.taxibekatbot.util

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

class KeyboardUtils

fun ArrayList<*>.generateInlineKeyboardMarkup(text: String, callbackData: String): InlineKeyboardMarkup {
    val keyboard = this.generateInlineKeyboard(text, callbackData)
    return InlineKeyboardMarkup().apply { this.keyboard = keyboard }
}

fun ArrayList<*>.generateInlineKeyboard(text: String, callbackData: String): ArrayList<List<InlineKeyboardButton>>{
    val keyBoardList = ArrayList<List<InlineKeyboardButton>>()

    this.forEach {
        keyBoardList.add(
            listOf(
                InlineKeyboardButton(text).apply { this.callbackData = callbackData }
            )
        )
    }
    return keyBoardList
}
