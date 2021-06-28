package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.TelegramUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TelegramUserRepository: JpaRepository<TelegramUser, Long> {

    fun findByTelegramId(telegramId: Long): TelegramUser?
}