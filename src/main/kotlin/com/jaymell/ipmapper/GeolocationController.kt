package com.jaymell.ipmapper

import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class GeolocationController {
    companion object : KLogging()

    @Autowired
    private lateinit var geolocator: Geolocator

    @RequestMapping("/geolocate", produces = ["application/json"])
    fun handleRequest(@RequestParam ip: String): ResponseEntity<String> {
        try {
           val ipLocation = geolocator.geolocate(ip).toString()
           return ResponseEntity<String>(ipLocation, HttpStatus.OK)
        } catch (e: InvalidIpLocationException) {
            return ResponseEntity<String>("", HttpStatus.BAD_REQUEST)
        }
    }
}