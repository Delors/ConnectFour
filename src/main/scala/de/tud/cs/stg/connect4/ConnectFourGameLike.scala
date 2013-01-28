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
  * Implementation of the `ConnectFourGame`trait.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
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

    def allSquaresOccupied(): Boolean = occupiedInfo.areOccupied(TOP_ROW_BOARD_MASK)

    def makeMove(squareMask: Mask): This = {
        newConnectFourGame(board, occupiedInfo.update(squareMask), playerInfo.update(squareMask))
    }

    def determineState(): State =
        determineState(FLAT_ALL_MASKS_FOR_CONNECT4_CHECK)

    def determineState(lastMove: Mask): State =
        determineState(masksForConnect4CheckForSquare(squareId(lastMove)))

    private[this] def determineState(allMasks: Array[Mask]): State = {
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
      * Scores a board by considering the weight of the squares occupied by each player. This scoring
      * function is extremely fast.
      *
      * @inheritdoc
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

    protected[connect4] def evaluateMove(nextMove: Mask, depth: Int, alpha: Int, beta: Int): Int = 
        -(makeMove(nextMove).negamax(nextMove, depth, alpha, beta))
    
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
      * Returns a human readable representation of the current game state suitable for debugging purposes.
      */
    override def toString() = "Next Player: "+playerInfo.turnOf()+"\nBoard:\n"+boardToString

}


