package monopoly.model
import scalafx.beans.property.StringProperty
import monopoly.util.Buyable
import monopoly.protocol.JsonSerializable

class Utility (_name : String, _xCoordinate : Int, _yCoordinate : Int, val price : Int, val rent : Int) extends Land (_name, _xCoordinate, _yCoordinate) with Buyable with JsonSerializable {

    override def description : StringProperty = {
        if (isOwned == false) {
            StringProperty("Unowned Utility")
        }
        else {
            StringProperty("Utility Owned by " + owner.username)
        }
    }

    //Rent for utilities is counted based on how many utilities the player owns. 
    //If the player owns both utilities, the rent of $100 is doubled.
    //The rent for owning both utilities is not hardcoded so more utilities can be added to the game easily.
    override def calculateRent : Int = {   
        var utilityCount : Int = 0

        for (land <- owner.ownedLands) {
            if (land.isInstanceOf[Utility]) {
                utilityCount += 1
            }
        }
        rent * utilityCount
    }
}
