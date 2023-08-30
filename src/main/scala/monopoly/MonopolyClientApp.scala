package monopoly
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.Includes._
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import javafx.{scene => jfxs}
import scalafx.stage.Stage
import monopoly.view.{GameController, HowToPlayController, LobbyController, MenuController, ScoreboardController}
import scalafx.scene.layout.AnchorPane
import scalafx.scene.image.Image
import akka.cluster.typed._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.adapter._
import akka.discovery.{Discovery, Lookup, ServiceDiscovery}
import akka.discovery.ServiceDiscovery.Resolved
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import java.net.InetAddress

object Client extends JFXApp {
    
    //This section is referring to the OOP practical class Address App framework
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    val config = ConfigFactory.load()
    val mainSystem = akka.actor.ActorSystem("HelloSystem", MyConfiguration.askDevConfig().withFallback(config))
    val greeterMain: ActorSystem[Nothing] = mainSystem.toTyped

    val cluster = Cluster(greeterMain)
    val discovery: ServiceDiscovery = Discovery(mainSystem).discovery

    val userRef = mainSystem.spawn(MonopolyClient(), "MonopolyClient")

    def joinPublicSeedNode(): Unit = {
        val lookup: Future[Resolved] =
        discovery.lookup(Lookup("wm.hep88.com").withPortName("hellosystem").withProtocol("tcp"), 1.second)

        lookup.foreach (x => {
            val result = x.addresses
            result map { x =>
                val address = akka.actor.Address("akka", "HelloSystem", x.host, x.port.get)
                cluster.manager ! JoinSeedNodes(List(address))
            }
        })
    }

    def joinLocalSeedNode(): Unit = {
        //Uncomment the following 3 lines for Hamachi
        //val ipRaw = Array(25,38,166,183)
        //val inetAddress = InetAddress.getByAddress(ipRaw.map(x => x.toByte))
        //val address = akka.actor.Address("akka", "HelloSystem", inetAddress.getHostAddress, 20000)

        //Uncomment the following 1 line for running locally
        val address = akka.actor.Address("akka", "HelloSystem", MyConfiguration.localAddress.get.getHostAddress, 20000)

        cluster.manager ! JoinSeedNodes(List(address))
    }
    joinLocalSeedNode()

    //This section is referring to practical class Address App framework
    val rootResource = getClass.getResource("view/RootLayout.fxml")

    //To store the instance of the current loaded fxml file so it does not have to be reloaded when a "Back" button is pressed to return to the previously loaded page
    var subRoots : AnchorPane = null
    var gameRoots : AnchorPane = null

    var loader = new FXMLLoader(rootResource, NoDependencyResolver)
    loader.load()

    val roots = loader.getRoot[jfxs.layout.BorderPane]

    val cssResource = getClass.getResource("view/Theme.css")
    roots.stylesheets = List(cssResource.toExternalForm)
    
    //Initialize the primary stage
    stage = new PrimaryStage {
        title = "Monopoly"
        scene = new Scene(width= 1200, height=900) {
            root = roots
            icons += new Image(getClass.getResourceAsStream("view/images/monopoly_icon.png"))
        }
        resizable = false
    }
    
    stage.onCloseRequest = handle( {
        mainSystem.terminate
    })

    Menu.load()
}

//Objects to load the various fxml
object Menu {
    val resource = getClass.getResource("view/Menu.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)
    loader.load()
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    var control = loader.getController[MenuController#Controller]()
    //this.roots.setCenter(roots)
    Client.subRoots = roots

    def load(): Unit = {
        User.ownRef = Client.userRef
        Client.roots.setCenter(roots)
    }
}

object HowToPlay {
    val resource = getClass.getResource("view/HowToPlay.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)
    loader.load()
    val roots  = loader.getRoot[jfxs.layout.AnchorPane]
    var control = loader.getController[HowToPlayController#Controller]()
    
    def load(): Unit = {
        val howToPlay = new Stage() {
        initOwner(Client.stage)
        scene = new Scene(width= 1200, height=900) {
            root = roots
            }
        }
        //Store the previous page when the How to Play page is accessed so that the game continues where it left off when the user clicks on Help > How to Play from the toolbar
        control.setPrevPage(Client.subRoots)
        Client.roots.setCenter(roots)
        control.primaryStage = howToPlay
    }
}

object Lobby {
    val resource = getClass.getResource("view/Lobby.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)
    loader.load()
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    var control = loader.getController[LobbyController#Controller]()

    def load(): Unit = {
        User.ownRef = Client.userRef
        Client.roots.setCenter(roots)
    }
}

object Game {
    val resource = getClass.getResource("view/Game.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)
    loader.load()
    Client.gameRoots = loader.getRoot[jfxs.layout.AnchorPane]
    val control = loader.getController[GameController#Controller]()
    val game = new Stage() {
        initOwner(Client.stage)
        scene = new Scene(width= 1200, height=900) {
            root = Client.gameRoots
            }
        }
    
    def load(room : ActorRef[MonopolyClient.Command], playersInRoom: ListBuffer[Player]): Unit = {
        Client.roots.setCenter(Client.gameRoots)
        control.primaryStage = game
        control.initializeGame(User.ownName, room, playersInRoom)
        Client.subRoots = Client.gameRoots
    }
    Client.stage.onCloseRequest = handle( {
        Game.control.forceExit()
        Client.mainSystem.terminate
    })
}

object Scoreboard {
    val resource = getClass.getResource("view/Scoreboard.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)
    loader.load()
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    var control = loader.getController[ScoreboardController#Controller]()

    def load(): Unit = {
        User.ownRef = Client.userRef
        Client.roots.setCenter(roots)
    }
}