package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.exception.DataNotFoundException
import me.timur.taxibekatbot.repository.TelegramUserRepository
import me.timur.taxibekatbot.util.PhoneUtil
import me.timur.taxibekatbot.util.getChatId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class TelegramUserService
@Autowired constructor(
    private val telegramUserRepository: TelegramUserRepository
){

    fun findById(id: Long): TelegramUser {
        return telegramUserRepository.findByIdOrNull(id)
                ?: throw DataNotFoundException("Could not found TelegramUser with id $id")
    }

    fun getOrSave(update: Update): TelegramUser {
        val user = getTelegramUser(update)

        return findByTelegramId(user.id)
            ?: save(TelegramUser(user))
    }

    fun saveUser(update: Update): TelegramUser {
        val user = getTelegramUser(update)

        val telegramUser = findByTelegramId(user.id) ?: TelegramUser(user)
        telegramUser.chatId = update.getChatId()

        return save(telegramUser)
    }

    fun findByTelegramId(telegramId: Long): TelegramUser? {
        return telegramUserRepository.findByTelegramId(telegramId)
    }

    fun savePhone(update: Update) {
        val tgUser = getOrSave(update)
        tgUser.phone = PhoneUtil.getFullPhoneNumber(update)
        save(tgUser)
    }

    private fun getTelegramUser(update: Update) =
        if (update.hasMessage()) update.message.from
        else update.callbackQuery.from


    private fun save(telegramUser: TelegramUser): TelegramUser {
        return telegramUserRepository.save(telegramUser)
    }
}