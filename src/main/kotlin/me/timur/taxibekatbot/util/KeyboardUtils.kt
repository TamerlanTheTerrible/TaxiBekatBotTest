package me.timur.taxibekatbot.util

import me.timur.taxibekatbot.util.KeyboardUtils.createInlineButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

object KeyboardUtils {

    fun createReplyKeyboardMarkup(vararg texts: String): ReplyKeyboardMarkup {
        return createReplyKeyboardMarkup(texts.asList())
    }

    fun createReplyKeyboardMarkup(texts: Collection<String>): ReplyKeyboardMarkup {
        val keyboard = createKeyboard(texts)
        return ReplyKeyboardMarkup(keyboard, true, true, false)
    }

    fun createKeyboard(vararg texts: String): ArrayList<KeyboardRow> {
        return createKeyboard(texts.asList())
    }

    fun createKeyboard(texts: Collection<String>): ArrayList<KeyboardRow> {
        val keyboard = ArrayList<KeyboardRow>()
        var keyboardRow = KeyboardRow()

        texts.forEachIndexed { index, text ->
            if (index % 2 == 0){
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

    fun createInlineButtonList(text: String, callback: String): List<InlineKeyboardButton> =
        listOf(createInlineButton(text, callback))

    fun createInlineButton(text: String, callback: String): InlineKeyboardButton =
        InlineKeyboardButton(text).apply { callbackData = callback }
}

fun List<*>.toInlineKeyBoard(
    callbackDataPrefix: String? = null,
    fieldName: String? = null
): InlineKeyboardMarkup{

    val keyBoardList = ArrayList<List<InlineKeyboardButton>>()
    var keyBoardRow = ArrayList<InlineKeyboardButton>()
    val keyboardTextField = fieldName ?: "nameLatin"

    this.forEachIndexed { index, it ->
        val fieldValue = InvokeGetter.invokeGetter(it!!, keyboardTextField).toString()
        val cbData = if (callbackDataPrefix == null) fieldValue else "${callbackDataPrefix}${fieldValue}"

        keyBoardRow.add(createInlineButton(fieldValue, cbData))
        if (index % 2 == 1) {
            keyBoardList.add(keyBoardRow)
            keyBoardRow = ArrayList()
        }
        else if (index == this.size - 1)
            keyBoardList.add(keyBoardRow)
    }

    return InlineKeyboardMarkup().apply { this.keyboard = keyBoardList }
}
