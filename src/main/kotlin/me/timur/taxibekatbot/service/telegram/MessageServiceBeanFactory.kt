package me.timur.taxibekatbot.service.telegram

import me.timur.taxibekatbot.util.PhoneUtil
import me.timur.taxibekatbot.util.getStringBefore
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class MessageServiceBeanFactory
@Autowired constructor(
    private var beanFactory: BeanFactory
){
    fun getBean(update: Update): MessageService {
        val beanName: String = getBeanName(update)

        return beanFactory.getBean(beanName, MessageService::class.java)
    }

    private fun getBeanName(update: Update): String{
        return "clientMessageService"
    }
}