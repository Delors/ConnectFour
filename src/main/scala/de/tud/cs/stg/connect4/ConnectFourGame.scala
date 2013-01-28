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
import scala.collection.generic.BitOperations
import scala.collection.generic.BitOperations
import java.util.BitSet

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
  * val connectFourGame = new DefaultConnectFourGame(Board6x7)
  * }}}
  * To update the game state you either call:
  * {{{
  * connectFourGame = connectFourGame.makeMove(<SQUARE_MASK>).
  * }}}
  * after the user has chosen the square, or if it is the ai's turn:
  * {{{
  * connectFourGame = connectFourGame.makeMove(game.proposeMove(aiStrength))
  * }}}
  * To evaluate the game state, i.e., to determine whether some user has won or the game is drawn call:
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

    def score(): Int

    protected[connect4] def negamax(lastMove: Mask, depth: Int, alpha: Int, beta: Int): Int

    protected[connect4] def evaluateMove(nextMove: Mask, depth: Int, alpha: Int, beta: Int): Int

    def determineState(): State

    /**
      * Determines the current state given the last move. This method is generally more efficient than
      * the generalized `determineState` method.
      */
    def determineState(lastMove: Mask): State

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

    def makeMove(squareMask: Mask): This

    def proposeMove(aiStrength: Int): Mask

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

abstract class ConnectFourGameLike protected[connect4] (
        final val board: Board,
        final val occupiedInfo: OccupiedInfo,
        final val playerInfo: PlayerInfo) extends ConnectFourGame {

    import board._

    protected[connect4]type This <: ConnectFourGameLike

    def nextMoves(): scala.collection.Iterator[Mask] = {
        new Iterator[Mask] {

            private var col = (cols / 2) - 1
            private var count = cols

            private def advance() {
                do {
                    count -= 1
                    col = (col + 1) % cols
                } while (occupiedInfo.areOccupied(upperLeftSquareMask << col) && count >= 0)
            }

            advance()

            def hasNext(): Boolean = count >= 0

            def next(): Mask = {
                val columnMask = columnMasks(col)
                var filteredOccupiedInfo = occupiedInfo.filter(columnMask)
                var mask = {
                    if (filteredOccupiedInfo.allSquaresEmpty)
                        1l << col
                    else
                        (filteredOccupiedInfo.board ^ columnMask) & (filteredOccupiedInfo.board << cols)
                }
                advance()
                mask
            }
        }
    }

    def lowestFreeSquareInColumn(column: Int): Option[Int] =
        (column to squares by cols) collectFirst ({
            case squareId if !occupiedInfo.isOccupied(squareId) ⇒ squareId
        })

    /**
      * True if all squares are occupied.
      */
    def allSquaresOccupied(): Boolean = occupiedInfo.areOccupied(TOP_ROW_BOARD_MASK)

    /**
      * Creates a new game state object by putting a man in the given square and updating the
      * information which player has to make the next move.
      *
      * ==Prerequisites==
      * The squares below the square have to be occupied and the specified square has to be empty.
      * However, both is not checked.
      *
      * @param square The mask that masks the respective square.
      * @return The updated game state.
      */
    def makeMove(squareMask: Mask): This = {
        newConnectFourGame(board, occupiedInfo.update(squareMask), playerInfo.update(squareMask))
    }

    /**
      * Determines the current state ([[de.tud.cs.stg.connect4.State]]) of the game.
      *
      * Either returns:
      *  - DRAWN (0l) if the game is finished (i.e., all squares are occupied), but no player has won.
      *  - NOT_FINISHED (-1l) if no player has won so far and some squares are empty.
      *  - a mask (some positive value >= 15 (00000....00001111)) that identifies the squares with
      * 	the four connected men.
      *
      * ==Note==
      * Every call to this method (re)analyses the board.
      */
    def determineState(): State = determineState(FLAT_ALL_MASKS_FOR_CONNECT4_CHECK)

    /**
      * Determines the state ([[de.tud.cs.stg.connect4.State]]) of the game given the last made move.
      * The result is only well defined iff the game was not already finished before the last move.
      *
      * ==Note==
      * Every call to this method (re)analyses the board.
      */
    def determineState(lastMove: Mask): State =
        determineState(masksForConnect4CheckForSquare(squareId(lastMove)))

    private def determineState(allMasks: Array[Mask]): State = {
        // 1. check if we can find a line of four connected men related to the last move
        val allMasksCount = allMasks.size
        var m = 0
        do {
            val mask = allMasks(m)
            if (occupiedInfo.areOccupied(mask) && playerInfo.belongToSamePlayer(mask)) return State(mask)
            m += 1
        } while (m < allMasksCount)

        // 2. check if the game is finished or not yet decided 
        if (occupiedInfo.areOccupied(TOP_ROW_BOARD_MASK))
            State.Drawn
        else
            State.NotFinished
    }

    /**
      * Scores a board by simply calculating the product of the square weights for each player and then
      * subtracting these values. This scoring function is extremely fast.
      */
    def score(): Int = {
        var whiteSquaresCount = 0
        var blackSquaresCount = 0
        var productOfSquareWeightsWhite: Long = 1l
        var productOfSquareWeightsBlack: Long = 1l

        // The following value is used to approximate the next move which is important if the number of men is 
        // not equal and we want to avoid that the scoring is skewed (too much)
        var bestSquareWeightOfNextMove: Int = 1

        var col = 0
        do {
            var mask = 1l << col
            var row = 0
            do {
                if (occupiedInfo.areEmpty(mask)) {
                    val squareWeight = SQUARE_WEIGHTS(squareId(row, col))
                    if (squareWeight > bestSquareWeightOfNextMove)
                        bestSquareWeightOfNextMove = squareWeight
                    row = rows // => break                        
                }
                else {
                    val sid = squareId(row, col)
                    if (playerInfo.areWhite(mask)) {
                        productOfSquareWeightsWhite += SQUARE_WEIGHTS(sid) * ESSENTIAL_SQUARE_WEIGHTS(sid)
                        whiteSquaresCount += 1
                    }
                    else {
                        productOfSquareWeightsBlack += SQUARE_WEIGHTS(sid) * ESSENTIAL_SQUARE_WEIGHTS(sid)
                        blackSquaresCount += 1
                    }
                }
                row += 1
                mask = mask << cols
            } while (row < rows)
            col += 1
        } while (col < cols)

        (whiteSquaresCount - blackSquaresCount) match {
            case 1  ⇒ (productOfSquareWeightsWhite - productOfSquareWeightsBlack + bestSquareWeightOfNextMove).toInt
            case -1 ⇒ (productOfSquareWeightsWhite + bestSquareWeightOfNextMove - productOfSquareWeightsBlack).toInt
            case _  ⇒ (productOfSquareWeightsWhite - productOfSquareWeightsBlack).toInt
        }
    }

    /**
      * Assesses the current board form the perspective of the current player. The value will be between
      * -Int.MaxValue and Int.MaxValue. A positive value indicates that the current player has an
      * advantage. A negative value indicates that the opponent has an advantage. If the value
      * is (-)Int.MaxValue the current(opponent) player can/will win. If the value is 0 either the game is
      * drawn or the ai was not able to determine if any player has an advantage.
      *
      * ==Precondition==
      * Before the immediate last move the game was not already finished. I.e., the return value is
      * not defined when the game was already won by a player before the last move.
      *
      * @param lastMove The last move that was made and which led to the current board.
      * @param depth The remaining number of levels that should be explored.
      * @param alpha The the best value that the current player can achieve (Initially -Int.MaxValue).
      * @param beta The best value that the opponent can achieve (Initially Int.MaxValue).
      */
    protected[connect4] def negamax(
        lastMove: Mask,
        depth: Int,
        alpha: Int,
        beta: Int): Int = {
        // The negamax algorithm is basically just a variant of the minimax algorithm. 
        // I.e., we have a single negamax method instead of two separate `minValue` and `maxValue` methods.
        // We can use the negamax algorithm because we have a zero-sum two player game where we strictly 
        // alternate between the players and the evaluation function is symmetric. (max(a,b) = -min(-a,-b)).

        // 1. check if the game is finished
        val state = determineState(lastMove)
        if (state.isDrawn) return 0
        if (state.hasWinner /* <=> the player who made the last move has won */ ) return -Int.MaxValue

        // 2. check if we have to abort exploring the search tree and have to assess the game state
        if (depth <= 0) {
            if (playerInfo.isWhitesTurn())
                return score()
            else
                return -score()
        }

        // 3. performs a recursive call to this method to continue exploring the search tree
        var valueOfBestMove = Int.MinValue // we always maximize (negamax)!
        val nextMoves = this.nextMoves()
        var newAlpha = alpha
        do { // for each legal move...
            val nextMove: Mask = nextMoves.next
            val value = evaluateMove(nextMove, depth - 1, -beta, -newAlpha)
            if (value >= beta) {
                // there will be no better move (we don't mind if there are other equally good moves)
                return value;
            }
            if (value > valueOfBestMove) {
                valueOfBestMove = value
                if (value > newAlpha)
                    newAlpha = value
            }
        } while (nextMoves.hasNext)
        valueOfBestMove
    }

    protected[connect4] def evaluateMove(nextMove: Mask, depth: Int, alpha: Int, beta: Int): Int = {
        -(makeMove(nextMove).negamax(nextMove, depth, alpha, beta))
    }

    /**
      * Proposes a ''move'' given the current game state. The negamax algorithm is used to determine it.
      *
      * @param aiStrength The strength of the ai. The strength determines the number of rounds the
      *     the ai looks ahead; a round consists of one move by each player. It should be at least 3 for
      *     a game that is not too easy.
      */
    def proposeMove(aiStrength: Int): Mask = {

        val maxDepth = aiStrength * 2

        val nextMoves = this.nextMoves()
        var bestMove: Mask = -1l
        var alpha = -Int.MaxValue
        do { // for each legal move...
            val nextMove: Mask = nextMoves.next()
            val value = evaluateMove(nextMove, maxDepth - 1, -Int.MaxValue, -alpha)

            // Beware: the negamax is implemented using fail-soft alpha-beta pruning; hence, if we would
            // choose a move with a value that is equal to the value of a previously evaluated move, it
            // could lead to a move that is actually advantageous for the opponent because a relevant part of
            // the search true was cut. 
            // Therefore, it is not directly possible to evaluate all equally good moves and we have to
            // use ">" here instead of ">=".
            if (value > alpha || bestMove == -1l) {
                bestMove = nextMove
                alpha = value
            }
        } while (nextMoves.hasNext)

        if (alpha == -Int.MaxValue && aiStrength > 2)
            // When the AI determines that it will always loose in the long run (when the opponent plays 
            // perfectly) it may still be possible to prevent the opponent from winning immediately and
            // hence, if the opponent does not play perfectly, to still win the game. However, to calculate
            // a meaningfull move, we simply reduce the number of levels we want to explore.
            proposeMove(math.max(1, aiStrength - 2))
        else
            bestMove
    }

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
    def boardToString(): String = {
        // we have to start with the upper left-hand corner when generating a human-readable representation
        var string = ""
        for (r ← (0 to maxRowIndex).reverse) {
            string += r+"  " // add the row index
            for (c ← 0 to maxColIndex) {
                val sid = squareId(r, c)
                if (occupiedInfo.isOccupied(sid))
                    string += playerInfo.belongsTo(sid).symbol+" "
                else
                    string += "  "
            }
            string += "\n"
        }
        string += "\n   "
        // add column indexes 
        for (c ← 0 until cols) string += c+" "
        string
    }

    /**
      * Returns a human readable representation of the current game state.
      */
    override def toString() = "Next Player: "+playerInfo.turnOf()+"\nBoard:\n"+boardToString

}

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

