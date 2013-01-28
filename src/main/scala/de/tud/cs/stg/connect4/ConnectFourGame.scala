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

import scala.language.existentials
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

/**
  * Represents the current game state: which fields are occupied by which player and which player has
  * to make the next move. The game state object is immutable. I.e., every update creates a new instance.
  *
  *  For a detailed discussion of "Connect Four" go to:
  * [[http://www.connectfour.net/Files/connect4.pdf Connect Four - Master Thesis]]
  *
  * ==Overview==
  * This is an implementation of the game "Connect Four" that uses the minimax algorithm to implement the
  * artificial intelligence.
  *
  * This implementation is primarily used for teaching purposes (w.r.t. the minimax/negamax algorithm), but the
  * ai is still strong enough to make it a reasonable opponent for a human player. However, the evaluation
  * function to assess the game state can be considered trivial at best unless the board is extremely small
  * (4x4) - in such a case the ai plays perfectly.
  *
  * In the following we use the following terminology:
  *  - The game is always played by exactly two ''PLAYER''s on a ''BOARD'' that typically has 6 ''ROWS'' x
  *     7 ''COLUMNS'' = 42 SQUARES.
  *  - The first player is called the ''WHITE'' player and has the integer identifier 0.
  *  - The second player is called the ''BLACK'' player and has the integer identifier 1.
  *  - Given a board with 6 rows and 7 columns each player has 21 MEN. Dropping a man in a column is called a ''MOVE''.
  *
  * The squares are indexed as follows if we have six rows and seven columns:
  * <pre>
  *  35 36 37 38 39 40 41   (top-level row)
  *  28 29 30 31 32 33 34
  *  21 22 23 24 25 26 27
  *  14 15 16 17 18 19 20
  *  7  8  9  10 11 12 13
  *  0  1  2  3  4  5  6    (bottom row)
  * </pre>
  *
  * ==Getting Started==
  * You first have to create an instance of this class and specify the type of board on which you want to play.
  * {{{
  * val connectFourGame = ConnectFourGame(Board6x7)
  * }}}
  * To update the game state you either call:
  * {{{
  * connectFourGame = connectFourGame.makeMove(<SQUARE_MASK>).
  * }}}
  * after the user has chosen the square, or if it is the ai's turn:
  * {{{
  * connectFourGame = connectFourGame.makeMove(game.proposeMove(aiStrength))
  * }}}
  * To evaluate the game state, i.e., to determine whether some user has won or the game is drawn, call:
  * {{{
  * connectFourGame.determineState()
  * }}}
  *
  * ==Implementation Details==
  * Two long values are use to encode the game state. The game state encompasses the information
  * which squares are occupied (encoded using the first (ROWS x COLUMNS) bits of the first long value) and
  * – if so – by which player a square is occupied (using the first ROWS x COLUMNS bits of the second long
  * value). Hence, a specific bit of the second long value has a meaning if – and only if – the same
  * bit is set in the first long value.
  * Additionally, the information is stored which player has to make the next move (using the most
  * significant bit (the 64th) of the second long value). The other bits of the long values are not used.
  *
  * Overall, this design enables a reasonable performance and facilitates the exploration of a couple million
  * game states per second (Java7, Intel Core i7 3GHZ and 8Gb Ram for the JVM).
  *
  * For example, to efficiently check whether the game is finished, all winning conditions are encoded using
  * special bit masks and these masks are just matched (by means of the "&" operator)
  * against both long values to determine whether a certain player has won the game.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
trait ConnectFourGame {

    protected[connect4]type This <: ConnectFourGame

    /**
      * Creates a new ConnectFourGame object.
      */
    protected[connect4] def newConnectFourGame(board: Board, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo): This

    /**
      * The board on which the game will be played.
      */
    val board: Board

    /**
      * Encodes which squares are occupied.
      */
    protected[connect4] val occupiedInfo: OccupiedInfo

    /**
      * In combination with `occupiedInfo` encodes the information which player occupies a square and also
      * encodes the information which player has to make the next move.
      */
    protected[connect4] val playerInfo: PlayerInfo

    /**
      * Scores the current board. The value is positive if the white player has an advantage and
      * negative if the black player has an advantage. The value will be in the range (-Int.MaxValue..
      * Int.MaxValue) unless the white or the black player will definitively win. In that case the value is
      * either Int.MaxValue or -Int.MaxValue.
      *
      * ==Note==
      * The value Int.MinValue == -Int.MaxValue-1 is reserved for internal purposes.
      *
      * @return The score of the current board [-Int.MaxValue..Int.MaxValue].
      */
    def score(): Int

    /**
      * Assesses the current board form the perspective of the current player. The value will be between
      * -Int.MaxValue and Int.MaxValue. A positive value indicates that the current player has an
      * advantage. A negative value indicates that the opponent has an advantage. If the value
      * is (-)Int.MaxValue the current(opponent) player can/will win. If the value is 0 either the game is
      * drawn or the ai was not able to determine if any player has an advantage.
      *
      * Subclasses are explicitly allowed to implement ''fail-soft alpha-beta pruning''. Hence, two moves
      * that are rated equally are not necessarily equally good when the second move was evaluated given
      * some alpha-beta bounds.
      *
      * ==Precondition==
      * Before the immediate last move the game was not already finished. I.e., the return value is
      * not defined when the game was already won by a player before the last move.
      *
      * @param lastMove The last move that was made and which led to the current board.
      * @param depth The remaining number of levels of the search tree that should be explored.
      * @param alpha The best value that the current player can achieve (Initially -Int.MaxValue).
      * @param beta The best value that the opponent can achieve (Initially Int.MaxValue).
      */
    protected[connect4] def negamax(lastMove: Mask, depth: Int, alpha: Int, beta: Int): Int

    /**
      * Creates a new `ConnectFourGame` object and initializes it with the updated board.
      *
      * ==Note==
      * This method was introduced as a hook to enable subclasses to intercept calls to the `negamax` which
      * are generally not called on this object but on the new `ConnectFourGame` object which encapsulates
      * the updated game logic.
      */
    protected[connect4] def evaluateMove(nextMove: Mask, depth: Int, alpha: Int, beta: Int): Int

    /**
      * Determines the current state ([[de.tud.cs.stg.connect4.State]]) of the game.
      *
      * Either returns:
      *  - DRAWN (0l) if the game is finished (i.e., all squares are occupied), but no player has won.
      *  - NOT_FINISHED (-1l) if no player has won so far and some squares are empty.
      *  - a mask (some positive value >= 15 (00000....00001111)) that identifies the squares with
      *     the four connected men.
      *
      * The result is only well defined iff the game was not already finished before the last move.
      *
      * ==Note==
      * Every call to this method (re)analyses the board.
      */
    def determineState(): State

    /**
      * Determines the current state ([[de.tud.cs.stg.connect4.State]]) given the last move. This method is
      * generally more efficient than the parameter-less `determineState` method.
      *
      * The result is only well defined iff the game was not already finished before the last move.
      *
      * ==Note==
      * Every call to this method (re)analyses the board given the last move
      */
    def determineState(lastMove: Mask): State

    /**
      * True if all squares of the board are occupied.
      */
    def allSquaresOccupied(): Boolean

    /**
      * Iterates over the masks that select the squares where the current player is allowed to put its
      * next man. We always start with the mask that selects the lowest free square in the middle column
      * as this is often the most relevant one (move ordering).
      */
    def nextMoves(): scala.collection.Iterator[Mask]

    /**
      * Returns `Some(squareId)` of the lowest square in the respective column that is free or `None`
      * otherwise.
      *
      * ==Note==
      * This method is not optimized and is therefore not intended to be used by the ai – it can, however,
      * be used in an interactive game.
      *
      * @param column A valid column identifier ([0..MAX_COL_INDEX]).
      * @return The id of the lowest square in the given column that is free.
      */
    def lowestFreeSquareInColumn(column: Int): Option[Int]

    /**
      * Creates a new `ConnectFourGame` object by putting a man in the given square and updating the
      * information which player has to make the next move.
      *
      * ==Prerequisites==
      * The squares below the square have to be occupied and the specified square has to be empty.
      * However, both is not checked.
      *
      * @param square The mask that masks the respective square.
      * @return The updated game object.
      */
    def makeMove(squareMask: Mask): This

    /**
      * Proposes a ''move'' given the current game state. The negamax algorithm is used to determine it.
      *
      * @param aiStrength The strength of the ai. The strength determines the number of rounds the
      *     the ai looks ahead; a round consists of one move by each player. The `aiStrength` should be at
      *     least 3 for a game that is not too easy.
      */
    def proposeMove(aiStrength: Int): Mask

    /**
      * Returns a human-readable representation of the board that is suitable for console output.
      *
      * E.g.
      * <pre>
      * 5      ◼
      * 4      ○
      * 3      ◼
      * 2      ○   ○
      * 1  ◼ ◼ ◼ ◼ ○
      * 0  ○ ◼ ○ ◼ ○   ○
      *
      *    0 1 2 3 4 5 6
      * </pre>
      */
    def boardToString(): String

}

trait Debug extends ConnectFourGame {

    protected[connect4]type This <: ConnectFourGame with Debug

    protected[connect4] var initialSearchDepth: Int

    protected[connect4] abstract override def evaluateMove(nextMove: Mask, depth: Int, alpha: Int, beta: Int): Int = {
        val score = super.evaluateMove(nextMove, depth, alpha, beta)

        if (initialSearchDepth - 1 == depth) {
            val LOST = -Int.MaxValue
            val WON = Int.MaxValue
            println("Move: "+board.column(nextMove)+" => "+
                {
                    score match {
                        case `WON`  ⇒ "will win"
                        case `LOST` ⇒ "will loose"
                        case v      ⇒ String.valueOf(v)+" (better if larger than a previous move)"
                    }
                }
            )
        }
        score
    }

    abstract override def proposeMove(aiStrength: Int): Mask = {
        initialSearchDepth = aiStrength * 2
        super.proposeMove(aiStrength)
    }
}

//trait DotOutput extends ConnectFourGame {
//
//    protected[connect4]type This <: ConnectFourGame with DotOutput
//
//    protected[connect4] var initialSearchDepth: Int
//
//    protected[connect4] var nodeLabel: String
//
//    protected[connect4] var bestScore: Int
//
//    protected[connect4] abstract override def evaluateMove(nextMove: Mask, depth: Int, alpha: Int, beta: Int): Int = {
//        val score = super.evaluateMove(nextMove, depth, alpha, beta)
//        if (score > bestScore)
//            bestScore = score;
//        score
//    }
//
//    abstract override def proposeMove(aiStrength: Int): Mask = {
//        nodeLabel = ""
//        bestScore = Int.MinValue
//        initialSearchDepth = aiStrength * 2
//
//        println("digraph connect4{ ordering=out;node [shape=record,style=filled];")
//        val mask = super.proposeMove(aiStrength)
//        println("\"root\" [label="+bestScore+"];\n}")
//        mask
//    }
//
//}
protected[connect4] class SimpleConnectFourGame protected[connect4] (
        board: Board,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo) extends ConnectFourGameLike(board, occupiedInfo, playerInfo) {

    type This = SimpleConnectFourGame

    final def newConnectFourGame(board: Board, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo): SimpleConnectFourGame = {
        new SimpleConnectFourGame(board, occupiedInfo, playerInfo)
    }
}

/**
  * Factory to create Connect Four games for specific boards.
  */
object ConnectFourGame {

    def apply(board: Board) = {
        new SimpleConnectFourGame(board, OccupiedInfo.create(), PlayerInfo.create())
    }

    def withDebug(board: Board) = {
        class DebugConnectFourGame protected[connect4] (
            board: Board,
            occupiedInfo: OccupiedInfo,
            playerInfo: PlayerInfo,
            var initialSearchDepth: Int = Int.MaxValue)
                extends ConnectFourGameLike(board, occupiedInfo, playerInfo)
                with Debug {
            Game ⇒

            type This = DebugConnectFourGame

            final def newConnectFourGame(board: Board, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo): DebugConnectFourGame = {
                new DebugConnectFourGame(board, occupiedInfo, playerInfo, Game.initialSearchDepth)
            }
        }

        new DebugConnectFourGame(board, OccupiedInfo.create(), PlayerInfo.create(), Int.MaxValue)
    }
}

