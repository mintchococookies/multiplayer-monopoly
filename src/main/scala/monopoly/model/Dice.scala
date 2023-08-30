package monopoly.model
import scala.util.Random

object Dice {
    private val random : Random = new Random
    var diceImageURL : String = ""

    def value : Int = {
        val x = 1 + random.nextInt(6)
        diceImageURL = "images/dice" + x + ".png"
        x
    }
}