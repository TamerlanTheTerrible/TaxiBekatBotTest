package me.timur.taxibekatbot.service

import me.timur.taxibekatbot.entity.Driver
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

    fun updateDriverRoutes(
            subRegionList: ArrayList<SubRegion>,
            destination: SubRegion,
            driver: Driver
    ){
        deletePreviousRoutesOfDriver(driver)
        createRoutesFromSubRegions(subRegionList, destination, driver)
    }

    private fun createRoutesFromSubRegions(
        subRegionSet: Collection<SubRegion>,
        destination: SubRegion,
        driver: Driver
    ): List<Route> {
        val routeList = ArrayList<Route>()
        subRegionSet.forEach {
            routeList.add(Route(it, destination, driver))
        }
        return routeRepo.saveAll(routeList)
    }

    private fun deletePreviousRoutesOfDriver(driver: Driver) {
        val previousRoutes = routeRepo.findAllByDriver(driver)
        if (!previousRoutes.isNullOrEmpty()){
            previousRoutes.forEach { it.deleted = true }
            routeRepo.saveAll(previousRoutes)
        }
    }

}
