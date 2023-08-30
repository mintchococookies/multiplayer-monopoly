package monopoly.view
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.application.Platform
import monopoly.{User, MonopolyClient, Lobby, HowToPlay, Scoreboard}

@sfxml
class MenuController {
    def handleStart(action: ActionEvent) {
        println("Lobby")
        Lobby.load()
    }

    def handleScoreboard(action: ActionEvent) {
        println("Scoreboard")
        Scoreboard.load()
    }

    def handleHowToPlay(action: ActionEvent){
        println("How to Play")
        HowToPlay.load()
    }

    def handleExit(action: ActionEvent){
        println("Exit")
        User.ownRef ! MonopolyClient.Leave
        Platform.exit();
        System.exit(0);
    }
} 
