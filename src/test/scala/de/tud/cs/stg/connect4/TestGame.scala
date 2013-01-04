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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import org.scalatest.matchers._

/**
  * Basic tests of the game logic and the ai.
  *
  * @author Michael Eichberg
  */
@RunWith(classOf[JUnitRunner])
class TestGame extends FunSpec with ShouldMatchers /*with BeforeAndAfterAll */ {

    //
    // Test Fixture
    //

    val connectFour = new ConnectFour(Configuration6x7)
    import connectFour._

    val b0 = new Game
    val b1 = b0.makeMove(4) // White
    val b2 = b1.makeMove(3) // Black
    val b3 = b2.makeMove(2) // White
    val b4 = b3.makeMove(2 + connectFour.COLS * 1) // Black
    val b5 = b4.makeMove(2 + connectFour.COLS * 2) // White
    val b6 = b5.makeMove(2 + connectFour.COLS * 3) // Black
    val b7 = b6.makeMove(2 + connectFour.COLS * 4) // White
    val b8 = b7.makeMove(2 + connectFour.COLS * 5) // Black
    val b9 = b8.makeMove(0) // White
    val b10 = b9.makeMove(7) // Black
    val b11 = b10.makeMove(11) // White
    val b12 = b11.makeMove(1) // Black
    val b13 = b12.makeMove(18) // White
    val b14 = b13.makeMove(10) // Black
    val bWHITEWins = b14.makeMove(25) // White
    val b15 = b14.makeMove(6) // White
    val bBLACKWins = b15.makeMove(8) // Black

    //Next Player: WHITE
    //Board:
    //5                
    //4                
    //3                
    //2        ◼       
    //1        ◼       
    //0        ◼ ○ ○ ○ 
    //
    //   0 1 2 3 4 5 6 
    val bBlackCanWin = b0.makeMove(4).makeMove(3).makeMove(5).makeMove(10).makeMove(6).makeMove(17)

    //Next Player: BLACK
    //Board:
    //5                
    //4                
    //3                
    //2                
    //1                
    //0    ◼ ◼ ○ ○ ○   
    //
    //   0 1 2 3 4 5 6     
    val bWhiteCanWin = b0.makeMove(3).makeMove(1).makeMove(4).makeMove(2).makeMove(5)

    //
    // Test
    //

    describe("connect 4 - ") {

        it("the game board is empty when the game starts") {
            for (i ← 0 until connectFour.SQUARES) {
                b0.isOccupied(i) should be(false)
            }
        }

        it("the white player always starts") {
            b0.turnOfPlayer should be(Player.WHITE.id)
        }

        it("when a player drops a men in a specific column it is placed in the lowest free square in the column which then should belong to the player ") {
            b1.playerId(4) should be(Player.WHITE.id)
            b2.playerId(3) should be(Player.BLACK.id)
            b3.playerId(2) should be(Player.WHITE.id)
        }

        it("on an empty board the set of legal moves are those that put a men in the squares of the lowest row") {
            b0.legalMoves.toSet should be(Set(0, 1, 2, 3, 4, 5, 6))
        }

        it("legal moves are always those that put a men about a occupied square as long as a column is not full") {
            b9.legalMoves.toSet should be(Set(7 /*col 1*/ , 1 /*col 2*/ , 10 /*col 4*/ , 11 /*col 5*/ , 5 /*col 6*/ , 6 /*col 7*/ ))
        }

        it("the board is not completely occupied if at least one square is empty") {
            b0.allSquaresOccupied should be(false)
            b9.allSquaresOccupied should be(false)
        }

        it("the game is not finished as long as no player has four connected men and some squares are still empty") {
            b1.determineState should be(NOT_FINISHED)
            b2.determineState should be(NOT_FINISHED)
            b3.determineState should be(NOT_FINISHED)
            b4.determineState should be(NOT_FINISHED)
            b5.determineState should be(NOT_FINISHED)
            b6.determineState should be(NOT_FINISHED)
            b7.determineState should be(NOT_FINISHED)
            b8.determineState should be(NOT_FINISHED)
            b9.determineState should be(NOT_FINISHED)
            b10.determineState should be(NOT_FINISHED)
        }

        it("a player wins the game if the player has four connected men in a column") {
            val result = bWHITEWins.determineState
            result should be((1l << 4 | 1l << 11 | 1l << 18 | 1l << 25))
            bWHITEWins.player(result) should be(Some(Player.WHITE))
        }

        it("a player wins the game if the player has four connected men in a row") {
            val result = bBLACKWins.determineState
            result should be((1l << 7 | 1l << 8 | 1l << 9 | 1l << 10))
            bBLACKWins.player(result) should be(Some(Player.BLACK))
        }

        it("if the black player has three men in a line which can be completed to a line of four connected men the AI should make the move that prevents the black player from (immediately) winning") {
            -bBlackCanWin.makeMove(11).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bBlackCanWin.makeMove(12).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bBlackCanWin.makeMove(13).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)

            -bBlackCanWin.makeMove(11).negamax(3, 4, -Int.MaxValue, -78, "") should be >= -78
            -bBlackCanWin.makeMove(12).negamax(3, 4, -Int.MaxValue, -78, "") should be >= -78
            -bBlackCanWin.makeMove(13).negamax(3, 4, -Int.MaxValue, -78, "") should be >= -78

            bBlackCanWin.proposeMove(2) should be(24)
        }

        it("if the white player has three men in a line which can be completed to a line of four connected men the AI should make the move that prevents the white player from (immediately) winning") {
            -bWhiteCanWin.makeMove(0).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bWhiteCanWin.makeMove(8).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bWhiteCanWin.makeMove(9).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bWhiteCanWin.makeMove(10).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bWhiteCanWin.makeMove(11).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bWhiteCanWin.makeMove(12).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be(-2147483647)
            -bWhiteCanWin.makeMove(6).negamax(3, 4, -Int.MaxValue, Int.MaxValue, "") should be > -2147483647

            bWhiteCanWin.proposeMove(3) should be(6)
            bWhiteCanWin.proposeMove(2) should be(6)
            bWhiteCanWin.proposeMove(1) should be(6)
        }
    }
}