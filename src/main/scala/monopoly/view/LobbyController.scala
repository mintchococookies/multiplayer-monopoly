package monopoly.view

import akka.actor.typed.ActorRef
import monopoly.{MonopolyClient, User, Player, Game, Menu, Room}
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.Stage
import scalafx.Includes._
import scala.collection.mutable.ListBuffer
import scalafx.beans.property.StringProperty
import javafx.beans.binding.Bindings

@sfxml
class LobbyController(
  private val playerName: TextField,
  private var onlineList: ListView[Player],
  private val roomTable: TableView[Room],
  private val ownerColumn : TableColumn[Room, String],
  private val capacityColumn : TableColumn[Room, String],
  private var joinBtn: Button,
  private var startBtn: Button,
  private var connectionLabel: Label) {

  var primaryStage : Stage = null

  //Only enable the join room button when the player has selected a room from the list of available rooms
  startBtn.disable = true
  startBtn.disable <== (Bindings.isEmpty(roomTable.selectionModel().selectedItems))

  //Update the client's connection status of whether or not they are connected to the server
  def updateConnectionStatus(status: String){
    connectionLabel.text <== StringProperty("") + status
  }

  //Player joins lobby after entering their username
  def playerJoin(action: ActionEvent): Unit = {
    if (playerName.text() != "") {
      User.ownName = playerName.text()
      User.ownRef ! MonopolyClient.StartJoin(playerName.text())
      joinBtn.disable = true
    } else {
      new Alert(AlertType.Warning) {
        title = "Error"
        headerText = "Name must not be empty"
        contentText = "Please insert a name."
      }.showAndWait()
    }
  }

  //Display an error if the username the player enters is already in use by another client currently online
  def duplicateNameError() = {
    new Alert(AlertType.Warning) {
        title = "Error"
        headerText = "Username already exists"
        contentText = "Please try a different username."
      }.showAndWait()
    joinBtn.disable = false
  }

  //Update the list of players online in the lobby
  def updateList(x: Iterable[Player]): Unit ={
    onlineList.items = new ObservableBuffer[Player]() ++= x
  }

  //Update the list of available rooms in the lobby.
  def updateRoomList(x: Iterable[Room]): Unit = {
    var rooms = new ObservableBuffer[Room]() ++= x
    for(room <- rooms){
      room.ownerNameS = new StringProperty(room.ownerName)
      room.numberOfPlayersInRoom = new StringProperty((room.playersInRoom.size).toString + "/2")
    }
    //Only display the rooms whose games have not started (not enough players joined yet)
    rooms = rooms.filter(y => y.started == false)
    roomTable.items = rooms
    ownerColumn.cellValueFactory = {_.value.ownerNameS}
    capacityColumn.cellValueFactory = {_.value.numberOfPlayersInRoom}
  }

  //Handle the button action for the creation of a new room
  def handleCreate(action: ActionEvent): Unit = {
    User.ownRef ! MonopolyClient.NewRoom
  }

  //Handle joining a room selected from the list of available rooms
  def joinRoom(): Unit = {
    val selectedRoom = roomTable.selectionModel().selectedItem.value.ownerRef
    User.ownRef ! MonopolyClient.JoinRoom(selectedRoom)
  }

  //Load the game screen
  def gameLoad(room : ActorRef[MonopolyClient.Command], playersInRoom : ListBuffer[Player]): Unit = {
    println("Start")
    Game.load(room, playersInRoom)
  }

  def handleBack(action: ActionEvent) = {
    println("Back to menu")
    Menu.load()
  }
}
