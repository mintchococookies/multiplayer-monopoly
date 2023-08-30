package monopoly
import akka.actor.typed.ActorRef
import monopoly.protocol.JsonSerializable
import scala.collection.mutable.ListBuffer
import scalafx.beans.property.StringProperty

case class Player(name: String, ref: ActorRef[MonopolyClient.Command]) extends JsonSerializable {
  override def toString: String = {
    name
  }
  var name2 = name
}

//To store the actor reference and name for the user in this client locally
object User {
  var ownRef: ActorRef[MonopolyClient.Command] = null
  var ownName: String = ""
}

case class Room(val ownerName: String, val ownerRef: ActorRef[MonopolyClient.Command]) extends JsonSerializable{
  val roomCapacity: Int = 2
  var playersInRoom = new ListBuffer[Player]
  var ownerNameS : StringProperty = null
  var numberOfPlayersInRoom : StringProperty = null
  var started : Boolean = false

  def addPlayer(player: Player): Unit = {
    playersInRoom += player
  }

  def removePlayer(player: Player): Unit = {
    playersInRoom -= player
  }

  def reset(): Unit = {
    playersInRoom = ListBuffer[Player]()
  }

  override def toString: String = {
    s"${playersInRoom}"
  }
}

case class Score(val username: String, val netWorth: Int){
  var rankS: StringProperty = null
  var usernameS: StringProperty = null
  var netWorthS: StringProperty = null
}