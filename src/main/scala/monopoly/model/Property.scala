package monopoly.model
import scalafx.beans.property.StringProperty
import monopoly.util.Buyable
import monopoly.protocol.JsonSerializable

//class for the buyable properties on the board
class Property (_name : String, _xCoordinate : Int, _yCoordinate : Int, val price : Int, val rent : Int, var housePrice : Int, var houseRent : Int) 
    extends Land (_name, _xCoordinate, _yCoordinate) with Buyable with JsonSerializable {
    var numberOfHouses : Int = 0
    val maxHouses : Int = 4

    override def description : StringProperty = {
        if (isOwned == false) {
            StringProperty("Unowned Property")
        }
        else {
            StringProperty("Property Owned by " + owner.username.toString)
        }
    }

    //The rent for a property is calculated based on how many houses the player owns on that property
    override def calculateRent : Int = {
        if (numberOfHouses > 0) {
            rent + houseRent*numberOfHouses
        }
        else {
            rent
        }
    }
}
