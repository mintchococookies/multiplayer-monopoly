package monopoly
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.typed._
import monopoly.protocol.JsonSerializable
import scalafx.collections.ObservableHashSet
import scalafx.application.Platform
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.Address
import scala.collection.mutable.ListBuffer

//Some sections in this code are referring to the Distributed Systems practical class Chat Program framework
object MonopolyClient {
    sealed trait Command extends JsonSerializable
    case object start extends Command
    final case object FindTheServer extends Command
    private case class ListingResponse(listing: Receptionist.Listing) extends Command
    private final case class MemberChange(event: MemberEvent) extends Command
    private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Command

    val members = new ObservableHashSet[Player]()
    var roomList = new ObservableHashSet[Room]()
    var leaderBoard = new ObservableHashSet[Score]()
    var defaultBehavior: Option[Behavior[MonopolyClient.Command]] = None
    var remoteOpt: Option[ActorRef[MonopolyServer.Command]] = None 
    var nameOpt: Option[String] = None

    //LOBBY PROTOCOL
    final case object Leave extends Command
    case class StartJoin(name: String) extends Command
    final case class Joined(list: Iterable[Player], roomList: Iterable[Room]) extends Command
    final case class DuplicateName(target: ActorRef[MonopolyClient.Command]) extends Command
    final case object DuplicateNameReceive extends Command
    final case object UpdateConnectionStatus extends Command
    final case class MemberList(list: Iterable[Player]) extends Command
    final case class RoomList(list: Iterable[Room]) extends Command
    final case class ScoreboardList(list: Iterable[Score]) extends Command
    final case object NewRoom extends Command
    final case class JoinRoom(room: ActorRef[MonopolyClient.Command]) extends Command
    final case class EnterRoom(target: ActorRef[MonopolyClient.Command], room: ActorRef[MonopolyClient.Command], playersInRoom: ListBuffer[Player]) extends Command
    final case class RemoveRoom(name: String, from: ActorRef[MonopolyClient.Command]) extends Command
    final case class StartGame(target: ActorRef[MonopolyClient.Command], playersInRoom: ListBuffer[Player]) extends Command
    
    //GAME PROTOCOL
    final case class  DiceAction(target: Player, diceValue: Int) extends Command
    final case class DiceActionReceive(diceValue: Int) extends Command
    final case class  BuyLand(target: Player) extends Command
    final case object BuyLandReceive extends Command
    final case class  BuyHouse(target: Player) extends Command
    final case object BuyHouseReceive extends Command
    final case class  ShowChanceDesc(target: Player, index: Int) extends Command
    final case class ShowChanceDescReceive(index: Int) extends Command
    final case class  ChanceAction(target: Player, index: Int) extends Command
    final case class ChanceActionReceive(index: Int) extends Command
    final case class  EndTurn(target: Player) extends Command
    final case object EndTurnReceive extends Command
    final case class SaveScore(username: String, netWorth: Int) extends Command
    final case class  ExitGame(target: Player) extends Command
    final case object ExitGameReceive extends Command

    val unreachables = new ObservableHashSet[Address]()
    unreachables.onChange{(ns, _) =>
        Platform.runLater {
            Lobby.control.updateList(members.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address)))
            if (unreachables.contains(User.ownRef)){
                Lobby.control.updateConnectionStatus("Disconnected")
            }
            //If a player becomes unreachable during a game, notify the other player in the room that the player has left the game.
            for (room <- roomList){
                for (player <- room.playersInRoom.toList.filter(y => unreachables.contains(y.ref.path.address))){
                    User.ownRef ! MonopolyClient.ExitGame(player)
                }
            }
        }
    }

    members.onChange{(ns, _) =>
        Platform.runLater {
            Lobby.control.updateList(ns.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address)))
        }  
    }

    roomList.onChange{(ns, _) =>
        Platform.runLater {
            Lobby.control.updateRoomList(roomList)
        }
    }

    leaderBoard.onChange{(ns, _) =>
        Platform.runLater {
            Scoreboard.control.updateLeaderBoard(leaderBoard)
        }
    }

    def messageStarted(): Behavior[MonopolyClient.Command] =
        Behaviors.receive[MonopolyClient.Command] { (context, message) =>
            message match {
                case ReachabilityChange(reachabilityEvent) =>
                    reachabilityEvent match {
                        case UnreachableMember(member) =>
                            unreachables += member.address
                            Behaviors.same
                        case ReachableMember(member) =>
                            unreachables -= member.address
                            Behaviors.same
                    }
                // LOBBY PROTOCOL //
                //Update the list of members in the online list
                case MemberList(list: Iterable[Player]) =>
                    members.clear()
                    members ++= list
                    Behaviors.same
                //Update the list of rooms in the available rooms list
                case RoomList(list: Iterable[Room]) =>
                    roomList.clear()
                    roomList ++= list
                    Behaviors.same
                //Update the list of scores in the scoreboard
                case ScoreboardList(list: Iterable[Score]) =>
                    leaderBoard.clear()
                    leaderBoard ++= list
                    Behaviors.same
                //Handle the client leaving the server
                case Leave =>
                    for (name <- nameOpt; remote <- remoteOpt) {
                        remote ! MonopolyServer.Leave(name, context.self)
                    }
                    Behaviors.same
                //Update the client's connection status in the lobby when they have connected to the server
                case UpdateConnectionStatus =>
                    Platform.runLater {
                        Lobby.control.updateConnectionStatus("Connected")
                    }
                    Behaviors.same
                //Allow a client to create a new room on the server
                case NewRoom =>
                    remoteOpt.map(_ ! MonopolyServer.CreateRoom(User.ownName, context.self))
                    Behaviors.same
                //Tell the server that a client joined an existing room
                case JoinRoom(room: ActorRef[MonopolyClient.Command]) =>
                    remoteOpt.map(_ ! MonopolyServer.ReceiveJoinRoom(User.ownName, context.self, room))
                    Behaviors.same
                //Allow a client to enter a room by loading the game screen
                case EnterRoom(target, room, playersInRoom) =>
                    Platform.runLater {
                        Lobby.control.gameLoad(room, playersInRoom)
                    }
                    Behaviors.same
                //Remove a room from the server
                case RemoveRoom(name, from) =>
                    remoteOpt.map(_ ! MonopolyServer.ReceiveRemoveRoom(name, from))
                    Behaviors.same
                //Start the game with the players who have entered the room
                case StartGame(target, playersInRoom) =>
                    Platform.runLater {
                        Game.control.start(playersInRoom)
                    }
                    Behaviors.same
                // GAME PROTOCOL //
                //Notify clients to perform the current player's required action in the game based on the value rolled with the dice
                case DiceAction(target, diceValue) =>
                    target.ref ! MonopolyClient.DiceActionReceive(diceValue)
                    Behaviors.same
                case DiceActionReceive(diceValue) =>
                    Platform.runLater {
                        Game.control.diceAction(diceValue)
                    }
                    Behaviors.same
                //Notify clients that the current player bought the land they landed on
                case BuyLand(target) =>
                    target.ref ! MonopolyClient.BuyLandReceive
                    Behaviors.same
                case BuyLandReceive =>
                    Platform.runLater {
                        Game.control.buyLand()
                    }
                    Behaviors.same
                //Notify clients that the current player bought a house on the land they landed on
                case BuyHouse(target) =>
                    target.ref ! MonopolyClient.BuyHouseReceive
                    Behaviors.same
                case BuyHouseReceive =>
                    Platform.runLater {
                        Game.control.buyHouse()
                    }
                    Behaviors.same
                //Notify clients of the contents of the random chance card drawn by the current player
                case ShowChanceDesc(target, index) =>
                    target.ref ! MonopolyClient.ShowChanceDescReceive(index)
                    Behaviors.same
                case ShowChanceDescReceive(index) =>
                    Platform.runLater {
                        Game.control.showChanceDesc(index)
                    }
                    Behaviors.same
                //Notify clients to perform the required action for the current player's drawn chance card
                case ChanceAction(target, index) =>
                    target.ref ! MonopolyClient.ChanceActionReceive(index)
                    Behaviors.same
                case ChanceActionReceive(index) =>
                    Platform.runLater {
                        Game.control.chanceAction(index)
                    }
                    Behaviors.same
                //Notify clients that the current player has ended their turn
                case EndTurn(target) =>
                    target.ref ! MonopolyClient.EndTurnReceive
                    Behaviors.same
                case EndTurnReceive =>
                    Platform.runLater {
                        Game.control.endTurn()
                    }
                    Behaviors.same
                //Notify clients that a player in the room has left the game
                case ExitGame(target) =>
                    target.ref ! MonopolyClient.ExitGameReceive
                    Behaviors.same
                case ExitGameReceive =>
                    Platform.runLater {
                        Game.control.exitGame()
                    }
                    Behaviors.same
                //Notify the server to save the players usernames and net worths to the scoreboard CSV file
                case SaveScore(username, netWorth) =>
                    remoteOpt.map(_ ! MonopolyServer.ReceiveSaveScore(username, netWorth))
                    Behaviors.same
                }
            }.receiveSignal {
                case (context, PostStop) =>
                    for (name <- nameOpt; remote <- remoteOpt){
                    remote ! MonopolyServer.Leave(name, context.self)
                    }
                    defaultBehavior.getOrElse(Behaviors.same)
            }

            def apply(): Behavior[MonopolyClient.Command] = Behaviors.setup { context =>
                Upnp.bindPort(context)

                val reachabilityAdapter = context.messageAdapter(ReachabilityChange)
                Cluster(context.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

                val listingAdapter: ActorRef[Receptionist.Listing] =
                    context.messageAdapter { listing =>
                        println(s"listingAdapter:listing: ${listing.toString}")
                        MonopolyClient.ListingResponse(listing)
                    }
                context.system.receptionist ! Receptionist.Subscribe(MonopolyServer.ServerKey, listingAdapter)
                defaultBehavior = Some(Behaviors.receiveMessage { message =>
                    message match {
                        case MonopolyClient.start =>
                            context.self ! FindTheServer
                            Behaviors.same
                        case FindTheServer =>
                            println(s"Client Hello: got a FindTheServer message")
                            context.system.receptionist !
                            Receptionist.Find(MonopolyServer.ServerKey, listingAdapter)
                            Behaviors.same
                        case ListingResponse(MonopolyServer.ServerKey.Listing(listings)) =>
                            val xs: Set[ActorRef[MonopolyServer.Command]] = listings
                            for (x <- xs) {
                                remoteOpt = Some(x)
                            }
                            Behaviors.same
                        //Notify the server that a client is joining the lobby
                        case StartJoin(name) =>
                            nameOpt = Option(name)
                            remoteOpt.map(_ ! MonopolyServer.JoinGame(name, context.self))
                            Behaviors.same
                        //Notify the client that the username they input is already in use by another client currently online
                        case DuplicateName(target) =>
                            target ! MonopolyClient.DuplicateNameReceive
                            Behaviors.same
                        case DuplicateNameReceive => 
                            Platform.runLater {
                                Lobby.control.duplicateNameError()
                            }
                            Behaviors.same
                        //Notify the clients of the current online list and available room list once they have joined the lobby
                        case MonopolyClient.Joined(x, y) =>
                            members.clear()
                            members ++= x
                            roomList.clear()
                            roomList ++= y
                            for (n <- x){
                                n.ref ! MonopolyClient.UpdateConnectionStatus
                            }
                            messageStarted()
                        case ReachabilityChange(reachabilityEvent) =>
                            reachabilityEvent match {
                                case UnreachableMember(member) =>
                                    unreachables += member.address
                                    Behaviors.same
                                case ReachableMember(member) =>
                                    unreachables -= member.address
                                    Behaviors.same
                            }
                        case _ =>
                            Behaviors.unhandled
                    }
                })
                defaultBehavior.get
            }
        }

