/* License (BSD Style License):
 * Copyright (c) 2012,2013
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.stg.connect4
package ui

/**
  * Implementation of a lightweight command-line interface to play connect four against the computer.
  *
  * The command-line interface enables the human player to let the computer make a proposal, to let the
  * computer make the move on behalf of the player or to let the player make the move on her/his own.
  *
  * The interface requires that a font with a fixed-width is used (i.e., a monospaced font); otherwise the
  * board may be rendered in a way that hinders comprehension by a human player.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
object CLI extends scala.App {

    import java.lang.System.in
    import State._

    private def readCharacterValue() = {
        val value = in.read();
        if (value != '\n') println
        value
    }

    private def readIntValue(min: Int = 0, max: Int = 9, default: Int) = {
        val value = readCharacterValue()
        if (value >= '0' + min && value <= '0' + max)
            value - '0'
        else
            default
    }

    /**
      * Starts a new game using the given connect four game as the basis.
      */
    def playGame(connectFourGame: ConnectFourGame, aiStrength: Int) {
        import connectFourGame._

        def playerMove(connectFourGame: ConnectFourGame, aiStrength: Int): (ConnectFourGame, State) = {
            val state = connectFourGame.determineState
            if (state.isNotFinished) {
                println(connectFourGame)
                print("Choose column: ")
                readCharacterValue() match {
                    case 'p' ⇒ { // let the ai make a proposal
                        val proposal = connectFourGame.column(connectFourGame.proposeMove(aiStrength))
                        println("\nProposal: "+proposal)
                        return playerMove(connectFourGame, aiStrength)
                    }
                    case 'm' ⇒ { // let the ai make the move
                        return aiMove(connectFourGame.makeMove(connectFourGame.proposeMove(aiStrength)), aiStrength)
                    }
                    case c if c >= '0' && c < ('0' + connectFourGame.cols) ⇒ {
                        connectFourGame.lowestFreeSquareInColumn(c - '0') match {
                            case Some(squareId) ⇒ return aiMove(connectFourGame.makeMove(Mask.forSquare(squareId)), aiStrength: Int)
                            case _              ⇒ println("The column has no empty square.")
                        }
                    }
                    case 'a' ⇒ return (connectFourGame, NotFinished)
                    case _ ⇒ println(
                        "Please enter:\n"+
                            "\tp - to get a proposal for a reasonable move.\n"+
                            "\tm - to let the ai make the next move for you.\n"+
                            "\t[0.."+connectFourGame.maxColIndex+"] - to drop a man in the respective column.")
                }
                return playerMove(connectFourGame, aiStrength)
            }
            else {
                return (connectFourGame, state)
            }
        }

        def aiMove(connectFourGame: ConnectFourGame, aiStrength: Int): (ConnectFourGame, State) = {
            val state = connectFourGame.determineState
            if (state.isNotFinished) {
                val startTime = System.currentTimeMillis()
                val theMove = connectFourGame.proposeMove(aiStrength)
                val requiredTime = (System.currentTimeMillis() - startTime) / 1000.0d
                println("Found the move ("+connectFourGame.board.column(theMove)+") in: "+requiredTime+" secs.")
                return playerMove(connectFourGame.makeMove(theMove), aiStrength)
            }
            else {
                return (connectFourGame, state)
            }
        }

        print("Do you want to start (y/n - Default: n)?")
        (if (readCharacterValue() == 'y')
            playerMove(connectFourGame, aiStrength)
        else
            aiMove(connectFourGame, aiStrength)
        ) match {
            case (_, NotFinished) ⇒ println("Game aborted.")
            case (_, Drawn)       ⇒ println("This game is drawn.")
            case (game, state) ⇒ {
                val mask = state.getMask
                val Some(player) = game.playerInfo.belongTo(mask)
                println(player+" has won!\n"+connectFourGame.maskToString(mask))
            }
        }
    }

    println("Welcome to connect four 2013!")

    print("Number of rows [4..8](Default: 6; rows*columns <= 56)?")
    private val rows: Int = readIntValue(min = 4, max = 8, default = 6)
    print("Number of columns [4..8](Default: 7; rows*columns <= 56)?")
    private val cols: Int = readIntValue(min = 4, max = 8, default = 7)
    private val board = new Board(rows, cols)

    print("Output the search tree (g) or debug info (d)(Default: <None>)?")
    private val connectFourGame =
        readCharacterValue() match {
            case 'g' ⇒ ConnectFourGame withDotOutput (board)
            case 'd' ⇒ ConnectFourGame withDebug (board)
            case _   ⇒ ConnectFourGame(board)
        }

    do { // main loop
        print("Strength of the ai [1..9](Default: 4(weak))?")
        var aiStrength: Int = readIntValue(1, 9, 4)
        println("The strength of the ai is set to "+aiStrength+".")

        playGame(connectFourGame, aiStrength)
    } while ({ print("Do you want to play a game (y/n) (Default: n)?"); readCharacterValue() == 'y' })

    println("Good Bye!")
}