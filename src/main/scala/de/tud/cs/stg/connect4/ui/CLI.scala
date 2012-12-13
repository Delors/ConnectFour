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

    // TODO Enable the user to choose the board size.
    val connectFour = new ConnectFour(Configuration6x7)
    //val connectFour = new ConnectFour(Configuration4x4)
    import connectFour._

    private val din = new java.io.DataInputStream(java.lang.System.in)

    private def playerMove(game: Game, aiStrength: Int): (Game, State) = {
        val state = game.state
        if (state == NOT_FINISHED) {
            println(game);
            { print("Choose column: "); val b = din.readByte(); println; b } match {
                case 'p' ⇒ { // let the ai make a proposal
                    val proposal = connectFour.column(game.bestMove(aiStrength))
                    println("\nProposal: "+proposal)
                    return playerMove(game, aiStrength)
                }
                case 'm' ⇒ { // let the ai make the move
                    return aiMove(game.makeMove(game.bestMove(aiStrength)), aiStrength)
                }
                case c if c >= '0' && c < ('0' + connectFour.COLS) ⇒ {
                    game.lowestFreeSquareInColumn(c - '0') match {
                        case Some(squareId) ⇒ return aiMove(game.makeMove(squareId), aiStrength: Int)
                        case _              ⇒ println("The column has no empty square.")
                    }
                }
                case 'a' ⇒ return (game, NOT_FINISHED)
                case _ ⇒ println(
                    "Please enter:\n"+
                        "\tp - to get a proposal for a reasonable move.\n"+
                        "\tm - to let the ai make the next move for you.\n"+
                        "\t[0.."+connectFour.MAX_COL_INDEX+"] - to drop a man in the respective column.")
            }
            return playerMove(game, aiStrength)
        }
        else {
            return (game, state)
        }
    }

    private def aiMove(game: Game, aiStrength: Int): (Game, State) = {
        val state = game.state
        if (state == NOT_FINISHED)
            playerMove(game.makeMove(game.bestMove(aiStrength)), aiStrength)
        else
            (game, state)
    }

    /**
      * Starts a new game.
      */
    def playGame() {
        val aiStrength = connectFour.RECOMMENDED_AI_STRENGTH;
        print("Do you want to start (y)?"); val in = din.readByte(); println
        (if (in == 'y') playerMove(new Game, aiStrength) else aiMove(new Game, aiStrength)) match {
            case (_, NOT_FINISHED) ⇒ println("Game aborted.")
            case (_, DRAWN)        ⇒ println("This game is drawn.")
            case (game, mask) ⇒ {
                val Some(player) = game.player(mask)
                println(player+" has won!\n"+connectFour.maskToString(mask))
            }
        }
    }

    do {
        playGame()
    } while ({ print("Do you want to play a game (y)?"); val b = din.readByte(); println; b == 'y' })

    println("Good Bye!")
}