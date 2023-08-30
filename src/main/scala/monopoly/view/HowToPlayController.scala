package monopoly.view
import monopoly.Client
import scalafx.scene.control.{Button}
import scalafxml.core.macros.sfxml
import scalafx.stage.Stage
import scalafx.scene.layout.AnchorPane

@sfxml
class HowToPlayController (
    private var backButton : Button
) {
    var primaryStage : Stage = null
    var prevPage : AnchorPane = null

    def setPrevPage(page : AnchorPane) = {
       prevPage = page
    }
    
    def handleBack() = {
        println("Back to menu")
        Client.roots.setCenter(prevPage)
    }
}