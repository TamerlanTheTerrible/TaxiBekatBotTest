package me.timur.taxibekatbot

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableEncryptableProperties
class TaxiBekatBOtApplication

fun main(args: Array<String>) {
    runApplication<TaxiBekatBOtApplication>(*args)
}
