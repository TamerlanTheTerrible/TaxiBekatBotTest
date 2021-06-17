package me.timur.taxibekatbot.repository

import me.timur.taxibekatbot.entity.Announcement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnnouncementRepository: JpaRepository<Announcement, Long> {
}