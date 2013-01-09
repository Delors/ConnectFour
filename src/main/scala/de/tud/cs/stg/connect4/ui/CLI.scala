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

    class Setup( final val connectFour: ConnectFour) {

        import connectFour._

        private var aiStrength: Int = 3

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
                    case c if c >= '0' && c < ('0' + connectFour.COLS) ⇒ {
                        game.lowestFreeSquareInColumn(c - '0') match {
                            case Some(squareId) ⇒ return aiMove(game.makeMove(squareId), aiStrength: Int)
                            case _              ⇒ println("The column has no empty square.")
                        }
                    }
                    case 'a' ⇒ return (game, State.notFinished)
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
            {
                print("Strength of the ai [1..9](Default: 3(weak))?"); val c = in.read(); println
                if (c >= '1' && c <= '9') {
                    aiStrength = c - '0';
                }
                else {
                    aiStrength = 3
                }
                println("Strength of the AI is set to "+aiStrength+".")
            }

            {
                print("Do you want to start (y/n)?"); val c = in.read(); println
                (if (c == 'y') playerMove(new Game, aiStrength) else aiMove(new Game, aiStrength)) match {
                    case (_, State.notFinished) ⇒ println("Game aborted.")
                    case (_, State.drawn)        ⇒ println("This game is drawn.")
                    case (game, state) ⇒ {
                        val mask = state.getMask
                        val Some(player) = game.player(mask)
                        println(player+" has won!\n"+connectFour.maskToString(mask))
                    }
                }
            }
        }
    }

    { // main
        print("Output the search tree (g) or debug info (d)(Default: None)?"); val c = in.read(); println
        val setup = new Setup(
            c match {
                case 'g' ⇒ new ConnectFour(Board6x7, false, true)
                case 'd' ⇒ new ConnectFour(Board6x7, true, false)
                case _   ⇒ new ConnectFour(Board6x7, false, false)
            }
        )

        do {
            setup.playGame()
        } while ({ print("Do you want to play a game (y)?"); val c = in.read(); println; c == 'y' })
        println("Good Bye!")
    }

}