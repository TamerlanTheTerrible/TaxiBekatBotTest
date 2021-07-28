package me.timur.taxibekatbot.util

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

object KeyboardUtils {

    fun createReplyKeyboardMarkup(vararg texts: String, columns: Int = 2): ReplyKeyboardMarkup {
        return createReplyKeyboardMarkup(texts.asList(), columns)
    }

    fun createReplyKeyboardMarkup(texts: Collection<String>, columns: Int = 2): ReplyKeyboardMarkup {
        val keyboard = createKeyboard(texts, columns)
        return ReplyKeyboardMarkup(keyboard, true, true, false)
    }

    fun createKeyboard(vararg texts: String, columns: Int = 2): ArrayList<KeyboardRow> {
        return createKeyboard(texts.asList(), columns)
    }

    fun createKeyboard(texts: Collection<String>, columns: Int = 2): ArrayList<KeyboardRow> {
        val keyboard = ArrayList<KeyboardRow>()
        var keyboardRow = KeyboardRow()

        texts.forEachIndexed { index, text ->
            if (index % columns == 0){
                keyboard.add(keyboardRow)
                keyboardRow = KeyboardRow()
            }

            keyboardRow.add(KeyboardButton(text))
        }

        keyboard.add(keyboardRow)
        return keyboard
    }

    fun createKeyboardRow(text: String): KeyboardRow{
        val keyboardRow = KeyboardRow()
        keyboardRow.add(KeyboardButton(text))
        return keyboardRow
    }
}
