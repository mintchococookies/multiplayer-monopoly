package monopoly.model
import scalafx.beans.property.StringProperty

class ChanceCard (_name : String, _xCoordinate : Int, _yCoordinate : Int) extends Land (_name, _xCoordinate, _yCoordinate) {
    var chanceIndex : Int = 0

    //Get a random chance from the set of chances
    def randomChanceIndex : Int = {
        val random = new scala.util.Random
        chanceIndex = 1 + random.nextInt(5)
        chanceIndex
    }

    def chanceDescription(index: Int) : StringProperty = {
        StringProperty(ChanceCard.chanceList(index))
    }
}

object ChanceCard {
    val chanceList = Map(1 -> "Go to Jail", 2 -> "Advance to Mayfair", 3 -> "Advance to GO", 4 -> "Gain $100!", 5 -> "Lose $150!")
} 