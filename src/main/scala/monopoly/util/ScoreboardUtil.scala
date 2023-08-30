package monopoly.util
import scala.io.Source
import scalafx.collections.ObservableHashSet
import monopoly.Score
import java.io.FileWriter
import java.io.PrintWriter

object ScoreboardUtil {
    val fileName = "scoreboard.csv"
    val scoreList = new ObservableHashSet[Score]()

    def getScores() = {
        val scores = Source.fromFile(fileName).getLines.toList
        for (score <- scores){
            val contents = score.split(",")
            scoreList += Score(contents(0), contents(1).toInt)
        }
        scoreList
    }

    //Helper functions to save the score to the CSV file
    def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B = try { f(param) } finally { param.close() }

    def appendToFile(fileName:String, textData:String) = using (new FileWriter(fileName, true)){ 
        fileWriter => using (new PrintWriter(fileWriter)) {
        printWriter => printWriter.print(textData)
        }
    }

    def updateScores(username: String, netWorth: Int) = {
        appendToFile(fileName, ("\n" + username.toString + "," + netWorth.toString))
    }

    
}