package monopoly.view

import monopoly.{Menu, Score}
import scalafx.scene.control.{TableColumn, TableView}
import scalafxml.core.macros.sfxml
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.StringProperty

@sfxml
class ScoreboardController (
  private val playerTable : TableView[Score],
  private val rankColumn : TableColumn[Score, String],
  private val nameColumn : TableColumn[Score, String],
  private val netWorthColumn : TableColumn[Score, String]
  ) {

  def handleBack() = {
    println("Back to menu")
    Menu.load()
  }

  def updateLeaderBoard(x: Iterable[Score]): Unit = {
    var counter = 0
    var scores = new ObservableBuffer[Score]() ++= x
    scores = scores.sortBy(_.netWorth)(Ordering[Int].reverse)
    for (score <- scores){
      counter += 1
      score.rankS = new StringProperty(counter.toString)
      score.usernameS = new StringProperty(score.username)
      score.netWorthS = new StringProperty(score.netWorth.toString)
    }
    playerTable.items = scores
    rankColumn.cellValueFactory = {_.value.rankS}
    nameColumn.cellValueFactory = {_.value.usernameS}
    netWorthColumn.cellValueFactory  = {_.value.netWorthS}
  }
}