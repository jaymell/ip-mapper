import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam

@Controller
class GeolocationController {
    companion object : KLogging()

    @Value("\${maxmind.key}")
    lateinit var maxmindKey: String

    @Value("\${maxmind.accountId}")
    lateinit var maxmindAccountId: String

    @RequestMapping("/geolocate", method = [RequestMethod.GET])
    fun handleRequest(@RequestParam ip: String) {

    }
}