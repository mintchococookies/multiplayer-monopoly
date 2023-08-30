package monopoly.util
import monopoly.model.GamePlayer

//Trait implemented for the Property and Utility types of Land that players can buy and own
trait Buyable {
    val price : Int
    val rent : Int
    var isOwned : Boolean = false
    var owner : GamePlayer = null
    
    def calculateRent : Int = 0
}