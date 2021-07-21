package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.repository.TelegramUserRepository
import me.timur.taxibekatbot.util.PhoneUtil
import me.timur.taxibekatbot.util.getChatId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class TelegramUserService
@Autowired constructor(
    private val telegramUserRepository: TelegramUserRepository
){
    fun getUser(update: Update): TelegramUser {
        val user = getTelegramUser(update)

        return findByTelegramId(user.id)
            ?: save(TelegramUser(user))
    }

    fun savePhone(update: Update) {
        val tgUser = getUser(update)
        tgUser.phone = PhoneUtil.getFullPhoneNumber(update)
        save(tgUser)
    }

    fun saveUser(update: Update): TelegramUser {
        val user = getTelegramUser(update)

        val telegramUser = findByTelegramId(user.id) ?: TelegramUser(user)
        telegramUser.chatId = update.getChatId()

        return save(telegramUser)
    }

    private fun getTelegramUser(update: Update) =
        if (update.hasMessage()) update.message.from
        else update.callbackQuery.from

    fun findByTelegramId(telegramId: Long): TelegramUser? {
        return telegramUserRepository.findByTelegramId(telegramId)
    }

    fun save(telegramUser: TelegramUser): TelegramUser {
        return telegramUserRepository.save(telegramUser)
    }
}