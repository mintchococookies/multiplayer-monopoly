# Online Multiplayer Monopoly in Scala and ScalaFX
Online Multiplayer GUI-based Monopoly Game developed with Akka in Scala and ScalaFX

![alt text](https://github.com/mintchococookies/multiplayer-monopoly/blob/main/monopoly-capture.png)

# Features
## Menu
- View the Game Menu.
- Start the Game.
- View the How to Play page.
- Exit the program.

## Toolbar
- Exit the program.
- View the How to Play page.

## Game
- Input usernames before starting the game. The players will be identified by these usernames in the game. If a player does not input a username when starting the game, they will be referred to as Player 1 or Player 2 in the game.
- View the players’ money and net worth.
- View which player’s turn it is.
- Roll the dice to move around the board.
- View the price and rental information of a property or utility when landing on it.
- Choose to buy an unowned property when landing on it if they can afford it.
- Choose to buy a house on an owned property when landing on it if they can afford it. A maximum of 4 houses can be bought on each property.
- Choose to buy an unowned utility when landing on it if they can afford it.
- View which properties and utilities have been bought by which player via markers on the board.
- View the bought houses on each property via markers on the board.
- Pay rent to the owner of a property or utility when landing on it.
- Receive rent from the player who lands on their owned property or utility.
- Receive $200 each time they pass the start square (GO).
- Take a randomly generated chance when landing on a chance card square.
- Go to Jail and miss 3 rounds.
- End their turn.
- When a player has won, view a dialog stating which player has won alongside their money and net worth.

# Description
This two-player online Monopoly Java Desktop game was developed using the Model-View-Controller (MVC) architectural pattern, Akka middleware framework, and Json API. In addition, the game was developed in the Object-oriented Programming style to ensure systematic development and maintenance. Using the Akka middleware, the program can be hosted on a server which will allow multiple clients to join and play the game together through the internet or locally within the same network. 

The program simulates the gameplay of the widely known Monopoly board game. The two players will take turns rolling a dice and moving across the squares on the board according to the dice number. At each square, the player will be able to perform actions specific to the type of square, such as buying a property, paying rent, or going to jail. The players are given $1200 when they begin the game. Each time they pass the Start (GO) square, they are given an additional $200. The money is used to perform transactions such as buying properties and houses and paying rent. The game ends when a player does not have enough money to fulfil a transaction, i.e., has less than $0 in funds. Once the game ends, the players can view the ranking of their final net worth in the scoreboard which is maintained on the server.

In terms of scalability, the program was written to be scalable whereby its architecture is able handle many client requests concurrently as is demonstrated by the ability for two people to play the game at the same time. Besides that, many games can be ongoing at the same time and the ongoing games are all stored in the server in the rooms list where the room has a started variable value of true. The program was tested with two games (4 clients) playing at the same time with successful results. No multithreading issues were encountered as all the clients can play at the same time without lag. This indicates that the program can handle instances where there are multiple clients interacting with the server simultaneously. The Akka framework can facilitate the scalability of the program by placing client actors joining the server into the cluster node which can contain many client actors.

In terms of reliability, the distributed system can handle the situations such as sudden disconnection, other clients leaving the game when it is on-going, and other clients becoming unreachable. The program accounts for a player leaving suddenly through many ways. When one of the players leave the game by clicking on the exit game button, clicking on the close window button at the top of the window, or due to a sudden cut off from network connection, the other player in the room will receive an alert window saying that the other player has left and ask them to join a different room. Furthermore, when a player who has created a room is waiting for another player to join their room closes the application, the room they created will be removed from the server. These are achieved using the Akka cluster receptionist which stores the actor references of each client and notifies the server when any client disconnects from the cluster. The server then notifies all the clients who are affected by the disconnection of the specific client to enact the respective error handling mechanisms.
