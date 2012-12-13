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
  * Basic tests of the game board.
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

        it("when a player drops a men in a specific column it is placed in the lowest free square in the column then belongs to the player ") {
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

        it("should not be completely occupied if at least one square is empty") {
            b0.allSquaresOccupied should be(false)
            b9.allSquaresOccupied should be(false)
        }

        it("the game is not finished as long as no player has four connected men and some squares are still empty") {
            b1.state should be(NOT_FINISHED)
            b2.state should be(NOT_FINISHED)
            b3.state should be(NOT_FINISHED)
            b4.state should be(NOT_FINISHED)
            b5.state should be(NOT_FINISHED)
            b6.state should be(NOT_FINISHED)
            b7.state should be(NOT_FINISHED)
            b8.state should be(NOT_FINISHED)
            b9.state should be(NOT_FINISHED)
            b10.state should be(NOT_FINISHED)
        }

        it("a player wins the game if the player has four connected men in a column") {
            val result = bWHITEWins.state
            result should be((1l << 4 | 1l << 11 | 1l << 18 | 1l << 25))
            bWHITEWins.player(result) should be(Some(Player.WHITE))
        }

        it("a player wins the game if the player has four connected men in a row") {
            val result = bBLACKWins.state
            result should be((1l << 7 | 1l << 8 | 1l << 9 | 1l << 10))
            bBLACKWins.player(result) should be(Some(Player.BLACK))
        }

    }
}