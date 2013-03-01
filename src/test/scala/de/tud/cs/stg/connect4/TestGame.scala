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
  * Basic tests of the game logic and the artificial intelligence.
  *
  * @author Michael Eichberg
  */
@RunWith(classOf[JUnitRunner])
class TestGame extends FunSpec with ShouldMatchers /*with BeforeAndAfterAll */ {

    //
    // Test Fixture
    //

    val b0 = ConnectFourGame(Board6x7)
    val b1 = b0.makeMove(Mask.forSquare(4)) // White
    val b2 = b1.makeMove(Mask.forSquare(3)) // Black
    val b3 = b2.makeMove(Mask.forSquare(2)) // White
    val b4 = b3.makeMove(Mask.forSquare(2 + b0.cols * 1)) // Black
    val b5 = b4.makeMove(Mask.forSquare(2 + b0.cols * 2)) // White
    val b6 = b5.makeMove(Mask.forSquare(2 + b0.cols * 3)) // Black
    val b7 = b6.makeMove(Mask.forSquare(2 + b0.cols * 4)) // White
    val b8 = b7.makeMove(Mask.forSquare(2 + b0.cols * 5)) // Black
    val b9 = b8.makeMove(Mask.forSquare(0)) // White
    val b10 = b9.makeMove(Mask.forSquare(7)) // Black
    val b11 = b10.makeMove(Mask.forSquare(11)) // White
    val b12 = b11.makeMove(Mask.forSquare(1)) // Black
    val b13 = b12.makeMove(Mask.forSquare(18)) // White
    val b14 = b13.makeMove(Mask.forSquare(10)) // Black
    val bWHITEWins = b14.makeMove(Mask.forSquare(25)) // White
    val b15 = b14.makeMove(Mask.forSquare(6)) // White
    val bBLACKWins = b15.makeMove(Mask.forSquare(8)) // Black

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
    val bBlackCanWin = b0.
        makeMove(Mask.forSquare(4)).makeMove(Mask.forSquare(3)).
        makeMove(Mask.forSquare(5)).makeMove(Mask.forSquare(10)).
        makeMove(Mask.forSquare(6)).makeMove(Mask.forSquare(17))

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
    val bWhiteCanWin = b0.
        makeMove(Mask.forSquare(3)).makeMove(Mask.forSquare(1)).
        makeMove(Mask.forSquare(4)).makeMove(Mask.forSquare(2)).
        makeMove(Mask.forSquare(5))

    //Next Player: WHITE
    //Board:
    //5                
    //4                
    //3        ○        
    //2        ◼        
    //1      ○ ○        
    //0    ◼ ○ ◼   ◼   
    //
    //   0 1 2 3 4 5 6     
    val bWhiteWillWinInFiveMoves = b0.
        makeMove(Mask.forSquare(2)).makeMove(Mask.forSquare(1)).
        makeMove(Mask.forSquare(9)).makeMove(Mask.forSquare(3)).
        makeMove(Mask.forSquare(10)).makeMove(Mask.forSquare(17)).
        makeMove(Mask.forSquare(24)).makeMove(Mask.forSquare(5))

    //Next Player: WHITE
    //Board:
    //5                
    //4                
    //3        ○        
    //2        ◼        
    //1      ○ ○        
    //0    ◼ ○ ◼   ◼   
    //
    //   0 1 2 3 4 5 6     
    val bWhiteWillWinInSevenMoves = b0.
        makeMove(Mask.forSquare(3)).makeMove(Mask.forSquare(4)).
        makeMove(Mask.forSquare(0)).makeMove(Mask.forSquare(2)).
        makeMove(Mask.forSquare(11)).makeMove(Mask.forSquare(18)).
        makeMove(Mask.forSquare(9)).makeMove(Mask.forSquare(16)).
        makeMove(Mask.forSquare(23)).makeMove(Mask.forSquare(5))

    //
    // TESTS
    //

    describe("connect 4 - ") {

        it("the board is empty when the game starts") {
            for (i ← 0 until b0.squares) {
                b0.occupiedInfo.isOccupied(i) should be(false)
            }
        }

        it("the white player always starts") {
            b0.playerInfo.turnOf() should be(Player.White)
        }

        it("when a player drops a man in a specific column it is placed in the lowest free square in that column") {
            b1.playerInfo.belongsTo(4) should be(Player.White)
            b2.playerInfo.belongsTo(3) should be(Player.Black)
            b3.playerInfo.belongsTo(2) should be(Player.White)
        }

        it("on an empty board the set of legal moves are those that put a man in a square in the lowest row") {
            val squaresOfLegalMoves = Set(0, 1, 2, 3, 4, 5, 6)
            b0.nextMoves.toSet should be(squaresOfLegalMoves.map(Mask.forSquare(_)))
            //b0.nextMoves.map(_.value).toSet should be(Set(1l, 1l << 1, 1l << 2, 1l << 3, 1l << 4, 1l << 5, 1l << 6))
        }

        it("legal moves are always those that put a man above an occupied square as long as a column is not full") {
            b9.nextMoves.map(_.value).toSet should be(Set(
                1l << 7 /*col 1*/ ,
                1l << 1 /*col 2*/ ,
                /*col 3 is full*/
                1l << 10 /*col 4*/ ,
                1l << 11 /*col 5*/ ,
                1l << 5 /*col 6*/ ,
                1l << 6 /*col 7*/ ))
        }

        it("the board is not completely occupied if at least one square is empty") {
            b0.allSquaresOccupied should be(false)
            b9.allSquaresOccupied should be(false)
        }

        it("the game is not finished as long as no player has four connected men and some squares are still empty") {
            import State.NotFinished
            b1.determineState should be(NotFinished)
            b2.determineState should be(NotFinished)
            b3.determineState should be(NotFinished)
            b4.determineState should be(NotFinished)
            b5.determineState should be(NotFinished)
            b6.determineState should be(NotFinished)
            b7.determineState should be(NotFinished)
            b8.determineState should be(NotFinished)
            b9.determineState should be(NotFinished)
            b10.determineState should be(NotFinished)
        }

        it("a player wins the game if the player has four connected men in a column") {
            val result = bWHITEWins.determineState
            result.getMask should be(Mask.forSquares(4, 11, 18, 25))
            bWHITEWins.playerInfo.belongTo(result.getMask) should be(Some(Player.White))
        }

        it("a player wins the game if the player has four connected men in a row") {
            val result = bBLACKWins.determineState
            result.getMask.value should be((1l << 7 | 1l << 8 | 1l << 9 | 1l << 10))
            bBLACKWins.playerInfo.belongTo(result.getMask) should be(Some(Player.Black))
        }

        it("if the black player has three men in a line that can be completed to a line of four connected "+
            "men then the ai should put a man in the square that prevents the black player from "+
            "(immediately) winning") {
            bBlackCanWin.evaluateMove(new bBlackCanWin.RootCacheManager, Mask.forSquare(11), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bBlackCanWin.evaluateMove(new bBlackCanWin.RootCacheManager, Mask.forSquare(12), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bBlackCanWin.evaluateMove(new bBlackCanWin.RootCacheManager, Mask.forSquare(13), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)

            Board6x7.squareId(bBlackCanWin.proposeMove(2)) should be(24)
        }

        it("if the white player has three men in a line which can be completed to a line of four connected "+
            "men the ai should make the move that prevents the white player from (immediately) winning") {
            bWhiteCanWin.evaluateMove(new bWhiteCanWin.RootCacheManager, Mask.forSquare(0), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bWhiteCanWin.evaluateMove(new bWhiteCanWin.RootCacheManager, Mask.forSquare(8), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bWhiteCanWin.evaluateMove(new bWhiteCanWin.RootCacheManager, Mask.forSquare(9), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bWhiteCanWin.evaluateMove(new bWhiteCanWin.RootCacheManager, Mask.forSquare(10), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bWhiteCanWin.evaluateMove(new bWhiteCanWin.RootCacheManager, Mask.forSquare(11), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bWhiteCanWin.evaluateMove(new bWhiteCanWin.RootCacheManager, Mask.forSquare(12), 3, -Int.MaxValue, Int.MaxValue) should be(-2147483647)
            bWhiteCanWin.evaluateMove(new bWhiteCanWin.RootCacheManager, Mask.forSquare(6), 3, -Int.MaxValue, Int.MaxValue) should be > -2147483647

            bWhiteCanWin.proposeMove(3) should be(Mask.forSquare(6))
            bWhiteCanWin.proposeMove(2) should be(Mask.forSquare(6))
            bWhiteCanWin.proposeMove(1) should be(Mask.forSquare(6))
        }

        it("an ai that looks ahead at least 5 moves should be able to detect how to force a win "+
            "for the following board:\n"+bWhiteWillWinInFiveMoves.toString) {
            bWhiteWillWinInFiveMoves.proposeMove(5) should be(Mask.forSquare(16))
            bWhiteWillWinInFiveMoves.proposeMove(6) should be(Mask.forSquare(16))
            bWhiteWillWinInFiveMoves.proposeMove(7) should be(Mask.forSquare(16))
            bWhiteWillWinInFiveMoves.proposeMove(8) should be(Mask.forSquare(16))
            bWhiteWillWinInFiveMoves.proposeMove(9) should be(Mask.forSquare(16))
            bWhiteWillWinInFiveMoves.proposeMove(10) should be(Mask.forSquare(16))
            bWhiteWillWinInFiveMoves.proposeMove(11) should be(Mask.forSquare(16))
            bWhiteWillWinInFiveMoves.proposeMove(12) should be(Mask.forSquare(16))
        }

        it("an ai that looks ahead at least 7 moves should be able to detect how to force a win "+
            "for the following board:\n"+bWhiteWillWinInSevenMoves.toString) {
            bWhiteWillWinInSevenMoves.proposeMove(7) should be(Mask.forSquare(10))
            bWhiteWillWinInSevenMoves.proposeMove(8) should be(Mask.forSquare(10))
            bWhiteWillWinInSevenMoves.proposeMove(9) should be(Mask.forSquare(10))
            bWhiteWillWinInSevenMoves.proposeMove(10) should be(Mask.forSquare(10))
            bWhiteWillWinInSevenMoves.proposeMove(11) should be(Mask.forSquare(10))
            bWhiteWillWinInSevenMoves.proposeMove(12) should be(Mask.forSquare(10))
        }

    }

    ignore("the better ai should win the game") {

        def playGame(playerAStrength: Int, playerBStrength: Int): ConnectFourGame = {
            var g = b0;

            do {
                var ng = g.makeMove(g.proposeMove(playerAStrength))
                if (ng.determineState.hasWinner) return ng;
                g = ng.makeMove(ng.proposeMove(playerBStrength))
            } while (!g.determineState.isFinished)
            fail("The game was won by the weak ai("+playerAStrength+" vs. "+playerBStrength+"):\n"+g.boardToString)
        }

        println(playGame(2, 1))
        println(playGame(3, 1))
        println(playGame(3, 2))
        println(playGame(4, 1))
        println(playGame(4, 2))
        println(playGame(4, 3))
        println(playGame(5, 1))
        println(playGame(5, 2))
        println(playGame(5, 3))
        println(playGame(5, 4))
        println(playGame(6, 1))
        println(playGame(6, 2))
        println(playGame(6, 3))
        println(playGame(6, 4))
        println(playGame(6, 5))
    }

}