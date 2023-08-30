package monopoly.model
import monopoly.util.{Buyable}
import scalafx.scene.shape.Circle
import scalafx.collections.ObservableBuffer
import monopoly.{MonopolyClient}
import akka.actor.typed.ActorRef
import monopoly.protocol.JsonSerializable
import scalafx.beans.property.StringProperty

case class GamePlayer (val username : String, val playerNumber : Int, val playerMarker : Circle, val ref : ActorRef[MonopolyClient.Command])
  extends JsonSerializable{
    var location : Land = Start
    var money : Int = 2000
    //Net worth is the worth of all owned properties + houses + all the collected rent. Indicates how well the player is doing financially
    var netWorth : Int = 0
    var ownedLands = new ObservableBuffer[Land]()
    var inJail : Boolean = false
    var jailCounter : Int = 3

    def movePlayer(land : Land){
        val x = land.xCoordinate
        val y = land.yCoordinate
        val height = 120
        val width = 120
        playerMarker.setLayoutX(x+(width)/2 + 20)
        playerMarker.setLayoutY(y+(height)/2 + 20)
    }

    def movePlayersOnSameSquare(land : Land){
        val x = land.xCoordinate
        val y = land.yCoordinate
        val height = 120
        val width = 120
        playerMarker.setLayoutX(x+(width)/2 - 20)
        playerMarker.setLayoutY(y+(height)/2 - 20)
    }

    def canAfford(price : Int) : Boolean = {
        if (money >= price) {
            true
        }
        else {
            false
        }
    }

    def buyLand(land : Land) {
        money -= land.asInstanceOf[Buyable].price
        netWorth += land.asInstanceOf[Buyable].price
        ownedLands += land
        land.asInstanceOf[Buyable].isOwned = true
        land.asInstanceOf[Buyable].owner = this
    }

    def buyHouse(property : Property) {
        money -= property.housePrice
        netWorth += property.housePrice
        property.numberOfHouses += 1
    }

    def payRent(price : Int) {
        money -= price
    }

    def receiveRent(price : Int) {
        money += price
        netWorth += price
    }

    def checkLose : Boolean = {
        if (money < 0) {
            true
        }
        else {
            false
        }
    }

    def checkInJail : Boolean = {
        if (inJail == true){
            jailCounter -= 1
            if (jailCounter == 0) {
                jailCounter = 3
                inJail = false
            }
        }
        inJail
    }
}

