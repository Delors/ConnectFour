package de.tud.cs.stg.connect4
package ui

/**
  * Evaluates how good a certain scoring function is by playing with it against another ai with a different
  * scoring function.
  *
  * @author Michael Eichberg
  */
object AIvsAI {

    def setup(
        simpleAIConnect4Game: ConnectFourGame,
        mediumAIConnect4Game: ConnectFourGame): (Int, Int) ⇒ Option[Player] = {

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

        playRound
    }

    def main(args: Array[String]): Unit = {

        println("------------Random AI vs Square Weights---------")

        println("Winner: "+setup(ConnectFourGame(Board6x7, ConnectFourGame.randomScore()),
            ConnectFourGame(Board6x7, ConnectFourGame.scoreBasedOnSquareWeights))(5, 5));

        println("Winner: "+setup(ConnectFourGame(Board6x7, ConnectFourGame.scoreBasedOnSquareWeights),
            ConnectFourGame(Board6x7, ConnectFourGame.randomScore()))(5, 5));

        println("\n\n------------Square Weights vs. Lines of Three Connected Men---------")

        println("Winner: "+setup(ConnectFourGame(Board6x7, ConnectFourGame.scoreBasedOnSquareWeights),
            ConnectFourGame(Board6x7, ConnectFourGame.scoreBasedOnLinesOfThreeConnectedMen))(5, 5));

        println("Winner: "+setup(ConnectFourGame(Board6x7, ConnectFourGame.scoreBasedOnLinesOfThreeConnectedMen),
            ConnectFourGame(Board6x7, ConnectFourGame.scoreBasedOnSquareWeights))(5, 5));

        println("\n\n------------Finished---------")
    }

}