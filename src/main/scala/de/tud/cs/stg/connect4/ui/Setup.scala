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
  * Basic setup (size of the board and the kind of generated debug information) of a connect four game.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class Setup( final val connectFour: ConnectFour) {

    import java.lang.System.in
    import State._
    import connectFour._

    private def playerMove(game: Game, aiStrength: Int): (Game, State) = {
        val state = game.determineState
        if (state.isNotFinished) {
            println(game);
            { print("Choose column: "); val b = in.read(); println; b } match {
                case 'p' ⇒ { // let the ai make a proposal
                    val proposal = connectFour.column(game.proposeMove(aiStrength))
                    println("\nProposal: "+proposal)
                    return playerMove(game, aiStrength)
                }
                case 'm' ⇒ { // let the ai make the move
                    return aiMove(game.makeMove(game.proposeMove(aiStrength)), aiStrength)
                }
                case c if c >= '0' && c < ('0' + connectFour.cols) ⇒ {
                    game.lowestFreeSquareInColumn(c - '0') match {
                        case Some(squareId) ⇒ return aiMove(game.makeMove(1l << squareId), aiStrength: Int)
                        case _              ⇒ println("The column has no empty square.")
                    }
                }
                case 'a' ⇒ return (game, NotFinished)
                case _ ⇒ println(
                    "Please enter:\n"+
                        "\tp - to get a proposal for a reasonable move.\n"+
                        "\tm - to let the ai make the next move for you.\n"+
                        "\t[0.."+connectFour.maxColIndex+"] - to drop a man in the respective column.")
            }
            return playerMove(game, aiStrength)
        }
        else {
            return (game, state)
        }
    }

    private def aiMove(game: Game, aiStrength: Int): (Game, State) = {
        val state = game.determineState
        if (state.isNotFinished) {
            val startTime = System.currentTimeMillis()
            val theMove = game.proposeMove(aiStrength)
            val requiredTime = (System.currentTimeMillis() - startTime) / 1000.0d
            println("Found the move ("+board.column(theMove)+") in: "+requiredTime+" secs.")
            return playerMove(game.makeMove(theMove), aiStrength)
        }
        else {
            return (game, state)
        }
    }

    /**
      * Starts a new game using the current game configuration.
      */
    def playGame() {
        print("Strength of the ai [1..9](Default: 3(weak))?"); val s = in.read(); println
        var aiStrength: Int = if (s >= '1' && s <= '9') s - '0' else 3
        println("The strength of the ai is set to "+aiStrength+".")

        print("Do you want to start (y/n - Default: n(o))?"); val c = in.read(); println
        (if (c == 'y') playerMove(new Game, aiStrength) else aiMove(new Game, aiStrength)) match {
            case (_, NotFinished) ⇒ println("Game aborted.")
            case (_, Drawn)       ⇒ println("This game is drawn.")
            case (game, state) ⇒ {
                val mask = state.getMask
                val Some(player) = game.playerInfo.belongTo(mask)
                println(player+" has won!\n"+connectFour.maskToString(mask))
            }
        }
    }
}

