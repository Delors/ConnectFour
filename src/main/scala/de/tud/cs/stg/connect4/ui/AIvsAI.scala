package de.tud.cs.stg.connect4
package ui

/**
  * Evaluates how good a certain scoring function is by playing with it against another ai.
  *
  * @author Michael Eichberg
  */
object AIvsAI {

    def main(args: Array[String]): Unit = {
        val simpleAIConnect4Game = ConnectFourGame(Board6x7, ConnectFourGame.randomScore())
        val mediumAIConnect4Game = ConnectFourGame(Board6x7, ConnectFourGame.scoreBasedOnSquareWeights)

        def playRound(simpleAIStrength: Int, mediumAIStrength: Int): Option[Player] = {
            var saiGame = simpleAIConnect4Game
            var maiGame = mediumAIConnect4Game
            do {
                {
                    val simpleAIMove = saiGame.proposeMove(simpleAIStrength);
                    saiGame = saiGame.makeMove(simpleAIMove);
                    println(saiGame.toString)
                    maiGame = maiGame.makeMove(simpleAIMove);

                    var state = saiGame.determineState()
                    if (state.isFinished) { if (state.hasWinner) return Some(Player.White) else return None }
                }

                {
                    val mediumAIMove = maiGame.proposeMove(mediumAIStrength);
                    maiGame = maiGame.makeMove(mediumAIMove);
                    println(maiGame.toString)
                    saiGame = saiGame.makeMove(mediumAIMove);

                    var state = maiGame.determineState()
                    if (state.isFinished) { if (state.hasWinner) return Some(Player.Black) else return None }
                }

            } while (true)
            None // dead code, but required to satisfy the compiler...
        }

        println("Winner: "+playRound(5, 5));
    }

}