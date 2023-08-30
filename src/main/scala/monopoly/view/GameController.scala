package monopoly.view
import monopoly.{Client, Lobby, MonopolyClient, Player, Room, Scoreboard, User}
import monopoly.model._
import monopoly.util._
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.shape.{Circle, Rectangle}
import scalafx.scene.control.{Alert, Button, ButtonType, Label}
import scalafx.scene.image.{Image, ImageView}
import scalafx.beans.property.StringProperty
import scalafx.stage.Stage
import akka.actor.typed.ActorRef

import scala.collection.mutable.ListBuffer

@sfxml
class GameController (
    //Player markers
    private val playerM1 : Circle,
    private val playerM2 : Circle,

    private var diceImage : ImageView,

    private var currentPlayerLabel : Label,
    private var currentPlayerMoneyLabel : Label,
    private var currentPlayerNetWorthLabel : Label,
    private var currentPlayerMarker : Circle,

    private var player1Name : Label,
    private var player1MoneyLabel : Label,
    private var player1NetWorthLabel : Label,
    private var player2Name : Label,
    private var player2MoneyLabel : Label,
    private var player2NetWorthLabel : Label,

    private var actionLabel : Label,
    private var landLabel : Label,
    private var landDescriptionLabel : Label,
    private var landInformationLabel : Label,
    private var affordLabel : Label,
    
    private var landInformationBox : Rectangle,
    private var landRentLabel : Label,
    private var landPriceLabel : Label,
    private var propertyRentHouseLabel : Label,
    private var propertyHousePriceLabel : Label,

    private var rollDiceButton : Button,
    private var buyLandButton : Button,
    private var buyHouseButton : Button,
    private var endTurnButton : Button,
    private var takeChanceButton : Button
    ) {
    
    var primaryStage : Stage = null

    //Initialize the objects used in the game
    var board = Board
    var dice = Dice
    var toIndex : Int = 0
    var player1Username : String = "Player 1"
    var player2Username : String = "Player 2"
    var player1 : GamePlayer = null //local player
    var player1Ref : ActorRef[MonopolyClient.Command] = null
    var player2 : GamePlayer = null
    var player2Ref : ActorRef[MonopolyClient.Command] = null
    var currentPlayer : GamePlayer = null
    var room : Room = null
    var host : GamePlayer = null
    var playersInRoom : ListBuffer[Player] = null
    var chance: ChanceCard = null
    var cIndex : Int = 0

    //Every client in the game room maintains their own instance of the game. The game state for every client updates
    //in the exact same way each time the current player makes a change by rolling the dice and performing player actions.
    //Every client will update the player actions by considering it done by the currentPlayer. Buttons are only enabled for the
    //current player by checking their actor reference with that of the current player. Thus, actions can only be performed by the current player.
    //For example, Client 1 and Client 2 know that Client 1 is the current player. Client 1 clicks on roll dice and moves 5 spaces.
    //Client 1 will notify all the other client's in the room that they rolled a 5. Thus, Client 2 will register that the current player
    //(Client 1), moves 5 spaces and update that accordingly in their own instance of the game.
    //In general, every function linked to a button click (handle_xx) will notify all the clients in the room to perform the required update
    //to the game state.

    def initializeGame(playerUsername: String, roomOwnerRef: ActorRef[MonopolyClient.Command], playersInRoom: ListBuffer[Player]) = {
        board.initializeBoard()
        clearLandInfo()
        rollDiceButton.setDisable(true)
        endTurnButton.setDisable(true)
        buyLandButton.setVisible(false)
        buyHouseButton.setVisible(false)
        takeChanceButton.setVisible(false)

        room = new Room(playerUsername, roomOwnerRef)
        this.player1Username = playerUsername        
        this.player2Username = "Waiting For Player..."
        player1 = new GamePlayer(player1Username, 1, playerM1, roomOwnerRef)
        player2 = new GamePlayer(player2Username, 2, playerM2, player2Ref)
        this.playersInRoom = ListBuffer(Player(playerUsername, roomOwnerRef))
        currentPlayer = player1
        displayPlayerInfo()
    }

    def start(playersInRoom: ListBuffer[Player]) {
        this.playersInRoom = playersInRoom
        for(player <- playersInRoom){
            if (player.ref == User.ownRef){
                player.name2 = player.name + " (Me)"
            }
            //player 1 is the room owner
            if (player.ref == room.ownerRef) {
                this.player1Username = player.name2
                this.player1Ref = player.ref
            }
            else {
                this.player2Username = player.name2
                this.player2Ref = player.ref
            }
        }
        player1 = new GamePlayer(player1Username, 1, playerM1, player1Ref)
        player2 = new GamePlayer(player2Username, 2, playerM2, player2Ref)
        currentPlayer = player1
        displayPlayerInfo()
        startTurn(currentPlayer)
    }

    def startTurn(player : GamePlayer) {
        //only enable the buttons for the current player's window
        if (player.ref == User.ownRef){
            if (player.checkInJail){
                actionLabel.text <== StringProperty(player.username) + StringProperty(" is in jail for ") + player.jailCounter + StringProperty(" rounds.")
                rollDiceButton.setDisable(true)
                endTurnButton.setDisable(false)
            }
            else { 
                rollDiceButton.setDisable(false)
            }
        }
    }

    def endTurn() {
        clearLandInfo()
        if (currentPlayer == player1){
            currentPlayer = player2
            currentPlayerMarker.setFill(player2.playerMarker.getFill)
        }
        else if (currentPlayer == player2){
            currentPlayer = player1
            currentPlayerMarker.setFill(player1.playerMarker.getFill)
        }
        displayPlayerInfo()
        startTurn(currentPlayer)
    }

    def checkGameWon() {
        //Check if the current player has lost
        if (currentPlayer.checkLose){
            if (currentPlayer == player1){
                winGame(player2)
            }
            else if (currentPlayer == player2) {
                winGame(player1)
            }
        }
    }

    def winGame(player : GamePlayer) {
        //display diff popups in the windows of the winner and loser
        val exitButton = new ButtonType("View Scoreboard")
        if (User.ownRef == player.ref) {
            if (player.username == player1.username){
                User.ownRef ! MonopolyClient.SaveScore(player1.username.substring(0, player1.username.length - 5), player1.netWorth)
                User.ownRef ! MonopolyClient.SaveScore(player2.username, player2.netWorth)
            } else if (player.username == player2.username) {
                User.ownRef ! MonopolyClient.SaveScore(player1.username, player1.netWorth)
                User.ownRef ! MonopolyClient.SaveScore(player2.username.substring(0, player2.username.length - 5), player2.netWorth)
            }
                  
            val alert = new Alert(Alert.AlertType.Information) {
            initOwner(Client.stage)
            title = "Congratulations!"
            headerText = "You won!"
            contentText = "Player 1 (" + player1.username + ")\nMoney: $" + player1.money + "\nNet Worth: $" + player1.netWorth + "\n\nPlayer 2 (" + player2.username + ")\nMoney: $" + player2.money + "\nNet Worth: $" + player2.netWorth
            buttonTypes = Seq(exitButton)
            }.showAndWait()
        } else {
            val alert = new Alert(Alert.AlertType.Information) {
            initOwner(Client.stage)
            title = "Better Luck Next Time!"
            headerText = "You lost!"
            contentText = "Player 1 (" + player1.username + ")\nMoney: $" + player1.money + "\nNet Worth: $" + player1.netWorth + "\n\nPlayer 2 (" + player2.username + ")\nMoney: $" + player2.money + "\nNet Worth: $" + player2.netWorth
            buttonTypes = Seq(exitButton)
            }.showAndWait()
        }       
        User.ownRef ! MonopolyClient.RemoveRoom(player1.username, player1.ref)
        Scoreboard.load()
    }

    def playerAction(land : Land) = {
        //If the player landed on a buyable piece of land
        if (land.isInstanceOf[Buyable]) {
            val currentLand : Buyable = land.asInstanceOf[Buyable]
            //Allow the client who is the current player to buy house and property
            if (User.ownRef == currentPlayer.ref){
                //If the land is not owned
                if (!currentLand.isOwned) {
                    //If the player can afford the land, allow them to buy it
                    if (currentPlayer.canAfford(currentLand.price)){
                        if (currentLand.isInstanceOf[Property]){
                            buyLandButton.setText("Buy Property")
                        }
                        else if (currentLand.isInstanceOf[Utility]){
                            buyLandButton.setText("Buy Utility")
                        }
                        buyLandButton.setVisible(true)
                    }
                    //If the player cannot afford the land, do not allow them to buy it
                    else {
                        affordLabel.text <== StringProperty("You Cannot Afford This")
                        affordLabel.setVisible(true)
                    }
                }
                //If a property is owned by the player who landed on it
                else if (currentLand.owner == currentPlayer && currentLand.isInstanceOf[Property]){
                    //If the player can afford a house on the property, allow them to buy one (max 4 houses per property)
                    if (currentPlayer.canAfford(currentLand.asInstanceOf[Property].housePrice) && currentLand.asInstanceOf[Property].numberOfHouses < currentLand.asInstanceOf[Property].maxHouses){
                        buyHouseButton.setVisible(true)
                    }
                    //If the player cannot afford a house on the property, do not allow them to buy one
                    else {
                        affordLabel.text <== StringProperty("You Cannot Afford a House")
                        affordLabel.setVisible(true)
                    }
                }
            }
            
            //If the player is not the land owner, pay rent to the owner
            if (currentLand.isOwned && currentLand.owner != currentPlayer){
                if (User.ownRef == currentPlayer.ref) {
                    actionLabel.text <== StringProperty("$ ") + currentLand.calculateRent + StringProperty(" rent paid to ") + currentLand.owner.username + StringProperty(" at ")
                }
                else {
                    actionLabel.text <== StringProperty("$ ") + currentLand.calculateRent + StringProperty(" rent received from ") + currentPlayer.username + StringProperty(" at ")
                }
                currentPlayer.payRent(currentLand.calculateRent)
                currentLand.owner.receiveRent(currentLand.calculateRent)
            }
            displayPlayerInfo()
        }
        
        //If the player lands on jail
        if (land == GoToJail){
            moveToJail()
        }

        //If the player lands on chance
        if (land.isInstanceOf[ChanceCard]){
            chance = land.asInstanceOf[ChanceCard]
            if (currentPlayer.ref == User.ownRef){
                cIndex = chance.randomChanceIndex
                for (player <- playersInRoom) { 
                    if (player.ref != currentPlayer.ref) {
                        User.ownRef ! MonopolyClient.ShowChanceDesc(player, cIndex)
                    }
                }      
                showChanceDesc(cIndex)
                takeChanceButton.setVisible(true)
                endTurnButton.setDisable(true)
            }
        }
    }

    def movePlayers(index : Int) = {
        val land = board.landList(index)
        //Move the player marker to the new index. If both players are on the same space, place the player next to the other player.
        currentPlayer.location = land
        if (player1.location != player2.location) {
            currentPlayer.movePlayer(land)
        }
        else {
            currentPlayer.movePlayersOnSameSquare(land)
        }
        displayLandInfo(currentPlayer.location)
    }

    def moveToJail() = {
        movePlayers(5)
        currentPlayer.location = Jail
        currentPlayer.inJail = true
        landDescriptionLabel.text <== StringProperty("In Jail for 3 rounds")
    }

    def displayLandInfo(land : Land) = {
        if (land.isInstanceOf[Buyable]){
            landInformationLabel.setVisible(true)
            landInformationBox.setVisible(true)
            landPriceLabel.text <== StringProperty("Price: $ ") + land.asInstanceOf[Buyable].price
        }
        if (land.isInstanceOf[Property]){
            propertyRentHouseLabel.text <== StringProperty("Rent per House: $ ") + land.asInstanceOf[Property].houseRent
            propertyHousePriceLabel.text <== StringProperty("House Costs: $ ") + land.asInstanceOf[Property].housePrice
            landRentLabel.text <== StringProperty("Rent: $ ") + land.asInstanceOf[Buyable].rent
        }
        else if (land.isInstanceOf[Utility]){
            propertyRentHouseLabel.text <== StringProperty("Rent: Rent x Utilities Owned")
            landRentLabel.text <== StringProperty("Rent per Utility: $ ") + land.asInstanceOf[Buyable].rent
        }
        actionLabel.text <== StringProperty("") + currentPlayer.username + StringProperty(" landed on")
        landLabel.text <== StringProperty("") + land.name
        if (!land.isInstanceOf[ChanceCard]){
            landDescriptionLabel.text <== land.description
        }
    }

    def showChanceDesc(index: Int) = {
        landDescriptionLabel.text <== chance.chanceDescription(index)
    }

    def clearLandInfo() = {
        landInformationLabel.setVisible(false)
        landInformationBox.setVisible(false)
        affordLabel.setVisible(false)
        buyLandButton.setVisible(false)
        buyHouseButton.setVisible(false)
        actionLabel.text <== StringProperty("")
        landLabel.text <== StringProperty("")
        landDescriptionLabel.text <== StringProperty("")
        landPriceLabel.text <== StringProperty("")
        landRentLabel.text <== StringProperty("")
        propertyRentHouseLabel.text <== StringProperty("")
        propertyHousePriceLabel.text <== StringProperty("")
    }

    def displayPlayerInfo() = {
        player1Name.text <== StringProperty(player1.username)
        player2Name.text <== StringProperty(player2.username)
        player1MoneyLabel.text <== StringProperty(player1.money.toString)
        player1NetWorthLabel.text <== StringProperty(player1.netWorth.toString)
        player2MoneyLabel.text <== StringProperty(player2.money.toString)
        player2NetWorthLabel.text <== StringProperty(player2.netWorth.toString)
        currentPlayerLabel.text <== StringProperty(currentPlayer.username) + StringProperty("'s Turn")
        currentPlayerMoneyLabel.text <== StringProperty(currentPlayer.money.toString)
        currentPlayerNetWorthLabel.text <== StringProperty(currentPlayer.netWorth.toString)
    }

    def handleExitGame(action: ActionEvent){
        println("Back to lobby")
        //Notify the player in the room that the other player left
        for (player <- playersInRoom) { 
            if (player.ref != User.ownRef) {
                User.ownRef ! MonopolyClient.ExitGame(player)
            }
        }
        //Get the server to remove the room from the list of rooms on the server
        User.ownRef ! MonopolyClient.RemoveRoom(player1.username, player1.ref)
        Lobby.load()
    }
    def exitGame(){
        playersInRoom = new ListBuffer[Player]()
        val exitButton = new ButtonType("Exit Room")
        val alert = new Alert(Alert.AlertType.Information) {
            initOwner(Client.stage)
            title = "Room Closed"
            headerText = "The other player has left the game."
            contentText = "Please join another room."
            buttonTypes = Seq(exitButton)
            }.showAndWait()
        Lobby.load()
    }
    //Function to handle a player exiting the game by closing the window
    def forceExit(){
        if (playersInRoom != null) {
            for (player <- playersInRoom) { 
                if (player.ref != User.ownRef) {
                    User.ownRef ! MonopolyClient.ExitGame(player)
                }
            }
        }
    }

    def handleRollDice(action: ActionEvent) {
        rollDiceButton.setDisable(true)
        endTurnButton.setDisable(false)

        //Current player (player who clicks the roll dice button) rolls the dice to get the index of the land to move to
        val diceValue = dice.value

        //Update the board for all clients based on the dice roll
        for (player <- playersInRoom) {
            User.ownRef ! MonopolyClient.DiceAction(player, diceValue)
        } 
    }
    def diceAction(diceValue: Int) {
        toIndex = board.getLandIndex(currentPlayer.location) + diceValue
        if (toIndex > 19) {
            currentPlayer.money += 200
            toIndex = toIndex - 19
        }
        if (toIndex == 0) {
            currentPlayer.money += 200
        }
        displayPlayerInfo()
        diceImage.setImage(new Image(getClass().getResourceAsStream("images/dice" + diceValue + ".png")))
        movePlayers(toIndex)
        playerAction(board.landList(toIndex))
        //Check if the game has been won
        checkGameWon()
    }

    def handleEndTurn(action: ActionEvent) {
        //End the current player's turn on the game state maintained by all the clients
        for (player <- playersInRoom) { 
            if (player.ref != currentPlayer.ref) {
                User.ownRef ! MonopolyClient.EndTurn(player)
            }
        } 
        if (currentPlayer.ref == User.ownRef){
            endTurnButton.setDisable(true)
        }
        endTurn()
    }

    def handleBuyLand(action: ActionEvent) {
        //Update the game state for all the clients in the game that the current player bought the land they landed on
        for (player <- playersInRoom) { 
            if (player.ref != currentPlayer.ref) {
                User.ownRef ! MonopolyClient.BuyLand(player)
            }
        } 
        buyLand()
    }
    def buyLand() {
        currentPlayer.buyLand(currentPlayer.location)
        board.addOwnedLandMarker(currentPlayer.location)
        if (currentPlayer.ref == User.ownRef){
            endTurnButton.setDisable(true)
        }
        endTurn()
    }

    def handleBuyHouse(action: ActionEvent) {
        //Update the game state for all the clients in the game that the current player bought a house on the land they landed on
        for (player <- playersInRoom) { 
            if (player.ref != currentPlayer.ref) {
                User.ownRef ! MonopolyClient.BuyHouse(player)
            }
        } 
        buyHouse()
    }
    def buyHouse() {
        currentPlayer.buyHouse(currentPlayer.location.asInstanceOf[Property])
        board.addOwnedHouseMarker(currentPlayer.location)
        for (player <- playersInRoom) { 
            if (player.ref != currentPlayer.ref) {
                User.ownRef ! MonopolyClient.EndTurn(player)
            }
        }
        if (currentPlayer.ref == User.ownRef){
            endTurnButton.setDisable(true)
        } 
        endTurn()
    }

    def handleTakeChance(action : ActionEvent) {
        takeChanceButton.setVisible(false)
        //Show all the clients the chance drawn by the current player and perform the chance on the current player
        for (player <- playersInRoom) { 
            if (player.ref != currentPlayer.ref) {
                User.ownRef ! MonopolyClient.ChanceAction(player, cIndex)
            }
        } 
        chanceAction(cIndex)
        endTurnButton.setDisable(false)
    }
    def chanceAction(index : Int) = {
        index match {
            case 1 => {
                moveToJail()
            }
            case 2 => {
                movePlayers(19)
                playerAction(board.landList(19))
            }
            case 3 => {
                movePlayers(0)
                playerAction(board.landList(0))
            }
            case 4 => currentPlayer.receiveRent(100)
            case 5 => currentPlayer.payRent(150)
        }
        displayPlayerInfo()
        checkGameWon()
    }
}