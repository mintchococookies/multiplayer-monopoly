package monopoly.model
import monopoly.Client
import monopoly.util.Buyable
import scalafx.collections.ObservableBuffer
import scalafx.scene.shape.Rectangle
import scalafx.Includes._
import scalafx.beans.property.StringProperty
import scalafx.scene.paint.Color

object Board {

    var landList = new ObservableBuffer[Land]()

    private def populateBoard(index : Int) : Land = {
        index match {
            case 0 => Start 
            case 1 => new Property("Old Kent Road", 47, 504, 50, 5, 50, 25)
            case 2 => new Property("Euston Road", 47, 384, 50, 7, 50, 25)
            case 3 => new ChanceCard("Chance", 47, 264)
            case 4 => new Property("Pall Mall", 47, 144, 100, 10, 100, 40)
            case 5 => Jail
            case 6 => new Property("Whitehall", 167, 22, 100, 12, 100, 40)
            case 7 => new Property("Trafalgar Square", 287, 22, 150, 14, 100, 50)
            case 8 => new Utility("Electric Company", 407, 22, 150, 100)
            case 9 => new Property("Leicester Avenue", 527, 22, 150, 16, 100, 50)
            case 10 => FreeParking
            case 11 => new Property("Coventry Street", 649, 142, 200, 18, 200, 100)
            case 12 => new Property("Oxford Street", 649, 262, 200, 20, 200, 100)
            case 13 => new ChanceCard("Chance", 649, 382)
            case 14 => new Property("Bond Street", 649, 502, 250, 22, 200, 150)
            case 15 => GoToJail
            case 16 => new Property("Regent Street", 528, 624, 250, 24, 200, 150)
            case 17 => new Utility("Water Works", 408, 624, 150, 100)
            case 18 => new Property("Park Lane", 288, 624, 300, 26, 200, 200)
            case 19 => new Property("Mayfair", 168, 624, 300, 28, 200, 200)
        }
    }

    def initializeBoard() {
        landList = new ObservableBuffer[Land]()
        for (index <- 0 to 19){
            var property = populateBoard(index)
            landList += property
        }
    }

    def getLandIndex(land : Land) : Int = {
        landList.indexOf(land)
    }

    def addOwnedLandMarker(land : Land) = {
        val landMarker = new Rectangle(){
            fill = land.asInstanceOf[Buyable].owner.playerMarker.getFill
        }

        if ((getLandIndex(land) <= 4) || (getLandIndex(land) >= 11 && getLandIndex(land) <= 14)){
            landMarker.setWidth(20)
            landMarker.setHeight(120)
        }
        else {
            landMarker.setWidth(120)
            landMarker.setHeight(20)
        }
        
        if (getLandIndex(land) >= 11 && getLandIndex(land) <= 14) {
            landMarker.setLayoutY(land.yCoordinate)
            landMarker.setLayoutX(land.xCoordinate + 100)
        }
        else if (getLandIndex(land) >= 16) {
            landMarker.setLayoutY(land.yCoordinate + 100)
            landMarker.setLayoutX(land.xCoordinate)
        }
        else {
            landMarker.setLayoutY(land.yCoordinate)
            landMarker.setLayoutX(land.xCoordinate)
        }
        Client.gameRoots.getChildren().add(landMarker)
    }

    def addOwnedHouseMarker(land : Land) = {
        val houseMarker = new Rectangle(){
            fill = Color.web("GREEN", 0.5d)
            width = 20
            height = 20
        }
        if (getLandIndex(land) <= 4) {
            houseMarker.setLayoutX(land.xCoordinate + 100)
            houseMarker.setLayoutY(land.yCoordinate - 26 + (30*(land.asInstanceOf[Property].numberOfHouses)-1))
        }
        else if (getLandIndex(land) >= 6 && getLandIndex(land) <= 9) {
            houseMarker.setLayoutY(land.yCoordinate + 100)
            houseMarker.setLayoutX(land.xCoordinate - 26 + (30*(land.asInstanceOf[Property].numberOfHouses)-1))
        }
        else if (getLandIndex(land) >= 15 && getLandIndex(land) <= 19) {
            houseMarker.setLayoutX(land.xCoordinate -26 + (30*(land.asInstanceOf[Property].numberOfHouses)-1))
            houseMarker.setLayoutY(land.yCoordinate)
        }
        else {
            houseMarker.setLayoutX(land.xCoordinate)
            houseMarker.setLayoutY(land.yCoordinate - 26 + (30*(land.asInstanceOf[Property].numberOfHouses)-1))
        }
        Client.gameRoots.getChildren().add(houseMarker)
    }

}

object Start extends Land("Start", 47, 624) {
    override def description : StringProperty = {
        StringProperty("Passed GO. Received $200!")
    }
}
object Jail extends Land("Jail", 47, 22){
    override def description : StringProperty = {
        StringProperty("Visiting Jail")
    }
}
object GoToJail extends Land("Go To Jail", 647, 624){
    override def description : StringProperty = {
        StringProperty("Skip Your Turn for 3 rounds.")
    }
}
object FreeParking extends Land("Free Parking", 647, 22){
    override def description : StringProperty = {
        StringProperty("Free Parking")
    }
}
