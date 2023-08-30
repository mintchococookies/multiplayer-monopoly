package monopoly.model
import scalafx.beans.property.StringProperty
import monopoly.protocol.JsonSerializable

abstract class Land (val nameS : String, val xCoordinate : Int, val yCoordinate : Int) 
extends JsonSerializable {
    //var name = new StringProperty(nameS)
    var name = nameS

    def description : StringProperty = {
        StringProperty("Unowned Land")
    }
}

