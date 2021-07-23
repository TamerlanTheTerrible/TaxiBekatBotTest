package me.timur.taxibekatbot.util

import me.timur.taxibekatbot.util.KeyboardUtils.createInlineButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

object KeyboardUtils {
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
