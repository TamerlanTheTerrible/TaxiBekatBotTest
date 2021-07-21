package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Route
import me.timur.taxibekatbot.entity.SubRegion
import me.timur.taxibekatbot.entity.TelegramUser
import me.timur.taxibekatbot.repository.RouteRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RouteService
@Autowired constructor(
    private val routeRepo: RouteRepository
){

    fun createRoutesFromSubRegions(
        subRegionSet: HashSet<SubRegion>,
        destination: SubRegion,
        telegramUser: TelegramUser
    ): List<Route> {

        val routeList = ArrayList<Route>()

        subRegionSet.forEach {
            routeList.add(
                Route(it, destination, telegramUser)
            )
        }

        return routeRepo.saveAll(routeList)
    }
}