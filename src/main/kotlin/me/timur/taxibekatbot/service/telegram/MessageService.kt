package me.timur.taxibekatbot.service.telegram

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

interface MessageService {

    fun generateMessage(update: Update): List<BotApiMethod<Message>>
}