package monopoly

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.adapter._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory
import monopoly.protocol.JsonSerializable
import scalafx.collections.ObservableHashSet
import monopoly.util.ScoreboardUtil

//Some sections in this code are referring to the Distributed Systems practical class Chat Program framework
object MonopolyServer {

  sealed trait Command extends JsonSerializable

  //Server Message Protocol
  case class JoinGame(name: String, from: ActorRef[MonopolyClient.Command]) extends Command
  case class CreateRoom(name: String, from: ActorRef[MonopolyClient.Command]) extends Command
  case class Leave(name: String, from: ActorRef[MonopolyClient.Command]) extends Command
  case class ReceiveJoinRoom(name: String, from: ActorRef[MonopolyClient.Command], room: ActorRef[MonopolyClient.Command]) extends Command
  case class ReceiveRemoveRoom(name: String, from: ActorRef[MonopolyClient.Command]) extends Command
  case class ReceiveSaveScore(username: String, netWorth: Int) extends Command

  //State value
  val ServerKey: ServiceKey[MonopolyServer.Command] = ServiceKey("MonopolyServer")
  
  //All the user actors in the server
  val members = new ObservableHashSet[Player]()

  //All the room actors in the server
  val rooms = new ObservableHashSet[Room]()

  //List of scores for the scoreboard
  var scores = new ObservableHashSet[Score]()

  val b1 : Option[Behavior[MonopolyServer.Command]] =None

  members.onChange{(ns, _) =>
    for(member <- ns){
      member.ref ! MonopolyClient.MemberList(members.toList)
    }
  }

  rooms.onChange{(ns, _) =>
    for(member <- members){
      member.ref ! MonopolyClient.RoomList(rooms.toList)
    }
  }

  scores.onChange{(ns, _) =>
    for(member <- members){
      member.ref ! MonopolyClient.ScoreboardList(scores.toList)
    }
  }


  def apply(): Behavior[MonopolyServer.Command] = Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(ServerKey, context.self)
      
      Behaviors.receiveMessage { message =>
        message match {
            case JoinGame(name, from) =>
              var duplicateName = false
              for (m <- members){
                if (m.name == name){
                  duplicateName = true
                }
              }
              if (duplicateName == false){
                members += Player(name, from)
                from ! MonopolyClient.Joined(members.toList, rooms.toList)
              } else {
                from ! MonopolyClient.DuplicateName(from)
              }
              scores.clear()
              scores ++= ScoreboardUtil.getScores()
              Behaviors.same
            //Handle a client leaving the server
            case Leave(name, from) =>
              //Remove any rooms which the client owns
              for(r <- rooms){
                if (r.ownerRef == from){
                  rooms -= Room(name, from)
                }
              }
              members -= Player(name, from)
              Behaviors.same
            //Create a new room actor on the server
            case CreateRoom(name, from) =>
              val newRoom = Room(name, from)
              newRoom.addPlayer(Player(name, from))
              rooms += newRoom
              from ! MonopolyClient.EnterRoom(from, from, newRoom.playersInRoom)
              Behaviors.same
            //Handle a client joining an existing room on the server
            case ReceiveJoinRoom(name, from, room) =>
              for(r <- rooms){
                if (r.ownerRef == room){
                    r.addPlayer(Player(name, from))
                }
                from ! MonopolyClient.EnterRoom(from, room, r.playersInRoom)
                //Start the game for all the players in the room once there are enough players in the room
                if (r.playersInRoom.size == 2){ 
                  for (player <- r.playersInRoom){
                    player.ref ! MonopolyClient.StartGame(player.ref, r.playersInRoom)
                  }
                  r.started = true
                }
              }
              for(member <- members){
                member.ref ! MonopolyClient.RoomList(rooms.toList)
              }
              Behaviors.same
            //Remove a room from the server
            case ReceiveRemoveRoom(name, from) =>
              rooms -= Room(name, from)
              Behaviors.same
            //Save the players usernames and net worths to the scoreboard CSV file
            case ReceiveSaveScore(username, netWorth) =>
              scores += Score(username, netWorth)
              ScoreboardUtil.updateScores(username, netWorth)
              Behaviors.same
            case _ =>
              Behaviors.unhandled
        }
      }
    }
}

object Server extends App {
  val config = ConfigFactory.load()
  val mainSystem = akka.actor.ActorSystem("HelloSystem", MyConfiguration.askDevConfig().withFallback(config))
  val typedSystem: ActorSystem[Nothing] = mainSystem.toTyped
  val cluster = Cluster(typedSystem)
  cluster.manager ! Join(cluster.selfMember.address)
  AkkaManagement(mainSystem).start()
  ClusterBootstrap(mainSystem).start()
  mainSystem.spawn(MonopolyServer(), "MonopolyServer")  
}
