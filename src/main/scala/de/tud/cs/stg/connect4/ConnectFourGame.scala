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
  * This implementation is primarily used for teaching purposes (w.r.t. the minimax/negamax algorithm). To
  * this end the default scoring function is very simple, but is still strong enough to make it a reasonable
  * opponent for a human player. However, the evaluation function to assess the game state can be considered
  * trivial at best unless the board is extremely small (4x4) - in such a case the ai plays perfectly.
  *
  * In the following we use the following terminology:
  *  - The game is always played by exactly two ''PLAYER''s on a ''BOARD'' that typically has 6 ''ROWS'' x
  *     7 ''COLUMNS'' = 42 SQUARES.
  *  - The first player is called the ''WHITE'' player and has the integer identifier 0.
  *  - The second player is called the ''BLACK'' player and has the integer identifier 1.
  *  - Given a board with 6 rows and 7 columns each player has 21 MEN. Dropping a man in a column is called a
  * 	''MOVE''.
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
  * @param board The board on which the game will be played.
  * @param score A function that scores a specific board. The value is positive if the white player has an
  *     advantage and negative if the black player has an advantage. The value will be in the range
  *     (-Int.MaxValue..Int.MaxValue) unless the white or the black player will definitively win. In that
  *     case the value is either Int.MaxValue or -Int.MaxValue. The value Int.MinValue == -Int.MaxValue-1 is
  *     reserved for internal purposes.
  * @param playerInfo In combination with `occupiedInfo` encodes the information which player occupies a
  *     square and also encodes the information which player has to make the next move.
  * @param occupiedInfo Encodes which squares are occupied.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class ConnectFourGame(
        final val board: Board,
        final val score: (Board, OccupiedInfo, PlayerInfo) ⇒ Int,
        final val occupiedInfo: OccupiedInfo = OccupiedInfo.create(),
        final val playerInfo: PlayerInfo = PlayerInfo.create()) {

    /**
      * Iterates over the masks that select the squares where the current player is allowed to put its
      * next man. We always start with the mask that selects the lowest free square in the "middle column"
      * as this is often the most relevant one (move ordering).
      */
    def nextMoves(): scala.collection.Iterator[Mask] = nextMoves(this.occupiedInfo)

    /**
      * Iterator that returns the masks that selects the squares where the current player is allowed to put
      * its next man.
      */
    protected[connect4] def nextMoves(occupiedInfo: OccupiedInfo): scala.collection.Iterator[Mask] = {
        new Iterator[Mask] {

            private var col = (board.cols / 2) - 1
            private var count = board.cols

            private def advance() {
                do {
                    count -= 1
                    col = (col + 1) % board.cols
                } while (occupiedInfo.areOccupied(Mask(board.upperLeftSquareMask.value << col)) && count >= 0)
            }

            advance()

            def hasNext(): Boolean = count >= 0

            def next(): Mask = {
                val columnMask = board.columnMasks(col)
                var filteredOccupiedInfo = occupiedInfo.filter(columnMask)
                var mask = {
                    if (filteredOccupiedInfo.allSquaresEmpty)
                        Mask(1l << col)
                    else
                        Mask(
                            (filteredOccupiedInfo.board ^ columnMask.value) &
                                (filteredOccupiedInfo.board << board.cols))
                }
                advance()
                mask
            }
        }
    }

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
    def lowestFreeSquareInColumn(column: Int): Option[Int] =
        (column to board.squares by board.cols) collectFirst ({
            case squareId if !occupiedInfo.isOccupied(squareId) ⇒ squareId
        })

    /**
      * True if all squares of the board are occupied.
      */
    def allSquaresOccupied(): Boolean = occupiedInfo.areOccupied(board.topRowMask)

    /**
      * Creates a new ConnectFourGame object.
      */
    protected[connect4] def newConnectFourGame(
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): ConnectFourGame = {

        new ConnectFourGame(board, score, occupiedInfo, playerInfo)
    }

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
    def makeMove(squareMask: Mask): ConnectFourGame = {
        newConnectFourGame(occupiedInfo.update(squareMask), playerInfo.update(squareMask))
    }

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
    def determineState(): State = determineState(board.masksForConnect4Check, occupiedInfo, playerInfo)

    /**
      * Determines the current state ([[de.tud.cs.stg.connect4.State]]) given the last move and a specific
      * game state.
      *
      * The result is only well defined iff the game was not already finished before the last move.
      *
      * ==Note==
      * Every call to this method (re)analyses the board given the last move
      */
    protected[connect4] def determineState(
        lastMove: Mask,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): State =
        determineState(board.masksForConnect4CheckForSquare(board.squareId(lastMove)), occupiedInfo, playerInfo)

    /**
      * Determines the state of the game given the game state encoded by `occpuiedInfo` and  `playerInfo`.
      *
      * The result is only well defined iff the game was not already finished before the last move.
      *
      * ==Note==
      * Every call to this method (re)analyses the board given the last move
      */
    protected[connect4] def determineState(
        allMasks: Array[Mask],
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): State = {

        // 1. check if we can find a line of four connected men related to the last move
        val allMasksCount = allMasks.size
        var m = 0
        do {
            val mask = allMasks(m)
            if (occupiedInfo.areOccupied(mask) && playerInfo.belongToSamePlayer(mask)) return State(mask)
            m += 1
        } while (m < allMasksCount)

        // 2. check if the game is finished or not yet decided 
        if (occupiedInfo.areOccupied(board.topRowMask))
            State.Drawn
        else
            State.NotFinished
    }

    /**
      * Assesses the current board form the perspective of the current player. The value will be between
      * `-Int.MaxValue` and `Int.MaxValue`. A positive value indicates that the current player has an
      * advantage. A negative value indicates that the opponent has an advantage. If the value
      * is (-)Int.MaxValue the current(opponent) player can/will win. If the value is 0 the game is
      * drawn or the ai was not able to determine if any player has an advantage.
      *
      * Subclasses are explicitly allowed to implement ''fail-soft alpha-beta-pruning''. Hence, two moves
      * that are rated equally are not necessarily equally good when the second move was evaluated given
      * some specific alpha-beta bounds.
      *
      * ==Precondition==
      * Before the immediate last move the game was not already finished. I.e., the return value is
      * not defined when the game was already won by a player before the last move.
      *
      * @param occupiedInfo The information which squares are currently occupied.
      * @param playerInfo The information which player occupies a specific square if the square is occupied at
      * 	all.
      * @param lastMove The last move that was made and which led to the current game state.
      * @param depth The remaining number of levels of the search tree that should be explored.
      * @param alpha The best value that the current player can achieve (Initially `-Int.MaxValue`).
      * @param beta The best value that the opponent can achieve (Initially `Int.MaxValue`).
      */
    protected[connect4] def negamax(
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo,
        lastMove: Mask,
        depth: Int,
        alpha: Int,
        beta: Int): Int = {
        // The negamax algorithm is basically just a variant of the minimax algorithm. 
        // I.e., we have a single negamax method instead of two separate `minValue` and `maxValue` methods.
        // We can use the negamax algorithm because we have a zero-sum two player game where we strictly 
        // alternate between the players and the evaluation function is symmetric. (max(a,b) = -min(-a,-b)).

        // 1. check if the game is finished
        val state = determineState(lastMove, occupiedInfo, playerInfo)
        if (state.isDrawn) return 0
        if (state.hasWinner /* <=> the player who made the last move has won */ ) return ConnectFourGame.Lost

        // 2. check if we have to abort exploring the search tree and have to assess the game state
        if (depth <= 0) {
            if (playerInfo.isWhitesTurn())
                return score(board, occupiedInfo, playerInfo)
            else
                return -score(board, occupiedInfo, playerInfo)
        }

        // 3. performs a recursive call to this method to continue exploring the search tree
        var valueOfBestMove = Int.MinValue // we always maximize (negamax)!
        val nextMoves = this.nextMoves(occupiedInfo)
        var newAlpha = alpha
        do { // for each legal move...
            val nextMove: Mask = nextMoves.next
            val value = evaluateMove(nextMove, occupiedInfo, playerInfo, depth - 1, -beta, -newAlpha)
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

    /**
      * Evaluates the given move w.r.t. the given game state. This method is used by the `negamax` when
      * exploring the search tree.
      *
      * ==Note==
      * This method was introduced as a hook to enable subclasses to intercept recursive calls to `negamax`.
      */
    protected[connect4] def evaluateMove(
        nextMove: Mask,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo,
        depth: Int,
        alpha: Int,
        beta: Int): Int = {

        -negamax(occupiedInfo.update(nextMove), playerInfo.update(nextMove), nextMove, depth, alpha, beta)
    }

    /**
      * Evaluates the given move w.r.t. the current game state.
      *
      * ==Note==
      * This method was introduced as a hook to enable subclasses to intercept the initial call to `negamax`.
      */
    protected[connect4] def evaluateMove(nextMove: Mask, depth: Int, alpha: Int, beta: Int): Int = {
        -negamax(occupiedInfo.update(nextMove), playerInfo.update(nextMove), nextMove, depth, alpha, beta)
    }

    /**
      * Proposes a ''move'' given the current game state. The negamax algorithm is used to determine it.
      *
      * @param aiStrength The strength of the ai. The strength determines the number of rounds the
      *     the ai looks ahead; a round consists of one move by each player. The `aiStrength` should be at
      *     least 3 for a game that is not too easy.
      */
    def proposeMove(aiStrength: Int): Mask = {

        val maxDepth = aiStrength * 2

        val nextMoves = this.nextMoves()
        var bestMove: Mask = Mask.Illegal
        var alpha = -Int.MaxValue
        do { // for each legal move...
            val nextMove: Mask = nextMoves.next()
            val value = evaluateMove(nextMove, maxDepth - 1, -Int.MaxValue, -alpha)

            // Beware: the negamax is implemented using fail-soft alpha-beta-pruning; hence, if we would
            // choose a move with a value that is equal to the value of a previously evaluated move, it
            // could lead to a move that is actually advantageous for the opponent because a relevant part of
            // the search true was cut. 
            // Therefore, it is not directly possible to evaluate all equally good moves and we have to
            // use ">" here instead of ">=".
            if (value > alpha || bestMove == Mask.Illegal) {
                bestMove = nextMove
                alpha = value
            }
        } while (nextMoves.hasNext)

        if (alpha == -Int.MaxValue && aiStrength > 2)
            // When the AI determines that it will always loose in the long run (when the opponent plays 
            // perfectly) it may still be possible to prevent the opponent from winning immediately and
            // hence, if the opponent does not play perfectly, to still win the game. However, to calculate
            // a meaningful move, we simply reduce the number of levels we want to explore.
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
        for (r ← (0 to board.maxRowIndex).reverse) {
            string += r+"  " // add the row index
            for (c ← 0 to board.maxColIndex) {
                val sid = board.squareId(r, c)
                if (occupiedInfo.isOccupied(sid))
                    string += playerInfo.belongsTo(sid).symbol+" "
                else
                    string += "  "
            }
            string += "\n"
        }
        string += "\n   "
        // add column indexes 
        for (c ← 0 until board.cols) string += c+" "
        string
    }

    /**
      * Returns a human readable representation of the current game state suitable for debugging purposes.
      */
    override def toString() = "Next Player: "+playerInfo.turnOf()+"\nBoard:\n"+boardToString

}

/**
  * Factory object to create specific types of Connect Four games.
  *
  * @author Michael Eichberg
  */
object ConnectFourGame {

    /**
      * Value returned by the `negamax`/`evaluateMove` method if the current player has lost the game.
      */
    protected[connect4] final val Lost = -Int.MaxValue

    /**
      * Value returned by the `negamax`/`evaluateMove` method if the current player has won the game.
      */
    protected[connect4] final val Won = Int.MaxValue

    /**
      * Creates a `Connect Four` game to play games on the given board with the given scoring function.
      *
      * @param board The board used for playing connect four.
      * @param score The scoring function that will be used to score the leaf-nodes of the search tree that
      * 	are not final states.
      */
    def apply(
        board: Board,
        score: (Board, OccupiedInfo, PlayerInfo) ⇒ Int = scoreBasedOnLinesOfThreeConnectedMen) =
        new ConnectFourGame(board, score)

    /**
      * Creates a `Connect Four` game that prints out some debugging information.
      *
      * @param board The board used for playing connect four.
      * @param score The scoring function that will be used to score the leaf-nodes of the search tree that
      * 	are not final states.
      */
    def withDebug(board: Board,
                  score: (Board, OccupiedInfo, PlayerInfo) ⇒ Int = scoreBasedOnLinesOfThreeConnectedMen) = {

        class DebugConnectFourGame(occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo)
                extends ConnectFourGame(board, score, occupiedInfo, playerInfo) {

            override protected[connect4] def newConnectFourGame(
                occupiedInfo: OccupiedInfo,
                playerInfo: PlayerInfo): ConnectFourGame = {

                new DebugConnectFourGame(occupiedInfo, playerInfo)
            }

            override protected[connect4] def evaluateMove(
                nextMove: Mask,
                depth: Int,
                alpha: Int,
                beta: Int): Int = {

                val score = super.evaluateMove(nextMove, depth, alpha, beta)

                println("Move: "+board.column(nextMove)+" => "+
                    {
                        score match {
                            case `Won`  ⇒ "will win"
                            case `Lost` ⇒ "will loose"
                            case v      ⇒ String.valueOf(v)+" (better if larger than a previous move)"
                        }
                    }
                )

                score
            }
        }

        new DebugConnectFourGame(OccupiedInfo.create(), PlayerInfo.create())
    }

    /**
      * Creates a `Connect Four` game that always prints out the search tree using Graphviz's Dot language.
      *
      * @param board The board used for playing connect four.
      * @param score The scoring function that will be used to score the leaf-nodes of the search tree that
      * 	are not final states.
      */
    def withDotOutput(board: Board,
                      score: (Board, OccupiedInfo, PlayerInfo) ⇒ Int = scoreBasedOnLinesOfThreeConnectedMen) = {

        class ConnectFourGameWithDotOutput(occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo)
                extends ConnectFourGame(board, score, occupiedInfo, playerInfo) {

            override protected[connect4] def newConnectFourGame(
                occupiedInfo: OccupiedInfo,
                playerInfo: PlayerInfo) = {

                new ConnectFourGameWithDotOutput(occupiedInfo, playerInfo)
            }

            private var currentNodeLabel: String = _

            override protected[connect4] def evaluateMove(
                nextMove: Mask,
                occupiedInfo: OccupiedInfo,
                playerInfo: PlayerInfo,
                depth: Int,
                alpha: Int,
                beta: Int): Int = {

                val oldLabel = currentNodeLabel
                currentNodeLabel += String.valueOf(board.column(nextMove))+"↓"
                println("\""+oldLabel+"\" -> "+"\""+currentNodeLabel+"\";")
                val score = super.evaluateMove(nextMove, occupiedInfo, playerInfo, depth, alpha, beta)
                println(
                    "\""+
                        currentNodeLabel+
                        "\" [label=\"{alpha="+
                        (alpha)+
                        "|beta="+
                        (beta)+
                        "| v("+
                        currentNodeLabel+
                        ")="+(-score)+
                        "}\"];")
                currentNodeLabel = oldLabel

                score
            }

            override protected[connect4] def evaluateMove(
                nextMove: Mask,
                depth: Int,
                alpha: Int,
                beta: Int): Int = {

                currentNodeLabel = String.valueOf(board.column(nextMove))+"↓"
                val score = super.evaluateMove(nextMove, depth, alpha, beta)
                println("\"root\" -> "+"\""+currentNodeLabel+"\";")
                println(
                    "\""+
                        currentNodeLabel+
                        "\" [label=\"{alpha="+
                        (alpha)+
                        "|beta="+
                        (beta)+
                        "| v("+
                        currentNodeLabel+
                        ")="+
                        (-score)+
                        "}\"];")
                        
                 
                score

            }

            override def proposeMove(aiStrength: Int): Mask = {
                println("digraph connect4{ ordering=out;node [shape=record,style=filled];")
                val move = super.proposeMove(aiStrength)
                println("\"root\" [label=\"move = "+board.column(move)+"\"];\n}")
                move
            }
        }

        new ConnectFourGameWithDotOutput(OccupiedInfo.create(), PlayerInfo.create())
    }

    /**
      * A scoring function that assigns the same value to all boards.
      *
      * This scoring function is primarily useful for testing purposes.
      */
    def fixedScore(board: Board, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo): Int = 0

    /**
      * A scoring function that assigns a random value to the current board.
      *
      * This scoring function is primarily useful for testing purposes.
      */
    def randomScore(): (Board, OccupiedInfo, PlayerInfo) ⇒ Int = {
        val rng = new java.util.Random();
        (board: Board, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo) ⇒ {
            rng.nextInt(21) - 10
        }
    }

    /**
      * Scores a board by considering the weight of the squares occupied by each player. This scoring
      * function is simple and fast but – in combination with a decent evaluation of the search tree – makes
      * a reasonable opponent.
      */
    def scoreBasedOnSquareWeights(board: Board, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo): Int = {

        import board._

        var whiteSquaresCount = 0
        var blackSquaresCount = 0
        var squareWeightsWhite: Long = 1l
        var squareWeightsBlack: Long = 1l

        // The following value is used to approximate the next move which is important if the number of men is 
        // not equal and we want to avoid that the scoring is skewed (too much)
        var bestSquareWeightOfNextMove: Int = 1

        var col = 0
        do {
            var mask = Mask.forSquare(col)
            var row = 0
            do {
                if (occupiedInfo.areEmpty(mask)) {
                    val squareWeight = squareWeights(board.squareId(row, col))
                    if (squareWeight > bestSquareWeightOfNextMove)
                        bestSquareWeightOfNextMove = squareWeight
                    row = board.rows // => break                        
                }
                else {
                    val sid = board.squareId(row, col)
                    if (playerInfo.areWhite(mask)) {
                        squareWeightsWhite += squareWeights(sid) * essentialSquareWeights(sid)
                        whiteSquaresCount += 1
                    }
                    else {
                        squareWeightsBlack += squareWeights(sid) * essentialSquareWeights(sid)
                        blackSquaresCount += 1
                    }
                }
                row += 1
                mask = board.moveUpByOneRow(mask)
            } while (row < board.rows)
            col += 1
        } while (col < board.cols)

        (whiteSquaresCount - blackSquaresCount) match {
            case 1  ⇒ (squareWeightsWhite - squareWeightsBlack + bestSquareWeightOfNextMove).toInt
            case -1 ⇒ (squareWeightsWhite + bestSquareWeightOfNextMove - squareWeightsBlack).toInt
            case _  ⇒ (squareWeightsWhite - squareWeightsBlack).toInt
        }
    }

    /**
      * This scoring function scores a board by looking for lines of three connected men that can be extended
      * to a line of four connected men.
      */
    def scoreBasedOnLinesOfThreeConnectedMen(
        board: Board,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): Int = {

        def determineState(
            lastMove: Int /*Square Id*/ ,
            occupiedInfo: OccupiedInfo,
            playerInfo: PlayerInfo): State = {
            val allMasks: Array[Mask] = board.masksForConnect4CheckForSquare(lastMove)

            // 1. check if we can find a line of four connected men related to the last move
            val allMasksCount = allMasks.size
            var m = 0
            do {
                val mask = allMasks(m)
                if (occupiedInfo.areOccupied(mask) && playerInfo.belongToSamePlayer(mask)) return State(mask)
                m += 1
            } while (m < allMasksCount)

            // 2. check if the game is finished or not yet decided 
            if (occupiedInfo.areOccupied(board.topRowMask))
                State.Drawn
            else
                State.NotFinished
        }

        val maxRowIndex = board.maxRowIndex

        var sumOfSquareWeights = 0
        var weightedWinningPositionsWhite = 0
        var weightedWinningPositionsBlack = 0

        var col = board.cols - 1
        do {
            var row = 0
            var whiteWinningPositions, blackWinningPositions = List[Int]() // list of relevant winning positions
            do {
                val squareId = board.squareId(row, col)
                if (occupiedInfo.isOccupied(squareId)) {
                    val squareWeight = board.squareWeights(squareId)
                    sumOfSquareWeights += (if (playerInfo.isWhite(squareId)) squareWeight else -squareWeight)
                }
                else {
                    val squareMask = Mask.forSquare(squareId)
                    val potentialOI = occupiedInfo.update(squareMask)

                    val potentialPIWhite = playerInfo.update(squareMask, Player.White)
                    if (determineState(squareId, potentialOI, potentialPIWhite).hasWinner) {
                        if (blackWinningPositions.isEmpty) {
                            if (whiteWinningPositions.isEmpty) {
                                weightedWinningPositionsWhite += 100
                                whiteWinningPositions = row :: whiteWinningPositions
                            }
                            else {
                                if (whiteWinningPositions.tail.isEmpty &&
                                    (row - whiteWinningPositions.head) % 2 == 1) {
                                    weightedWinningPositionsWhite +=
                                        // this is a "sure win" in this column!
                                        (board.rows - (row - whiteWinningPositions.head)) * 1000
                                    whiteWinningPositions = row :: whiteWinningPositions
                                }
                            }
                        }
                        else if (blackWinningPositions.tail.isEmpty) {
                            if ((row - blackWinningPositions.head) % 2 == 0 && whiteWinningPositions.isEmpty) {
                                weightedWinningPositionsWhite += 100
                                whiteWinningPositions = row :: whiteWinningPositions
                            } // else... a position immediately above a winning position of black has no value for white
                        } // else... if black already has two winning positions another line of three connected men is not helpful
                    }

                    val potentialPIBlack = playerInfo.update(squareMask, Player.Black)
                    if (determineState(squareId, potentialOI, potentialPIBlack).hasWinner) {
                        if (whiteWinningPositions.isEmpty) {
                            if (blackWinningPositions.isEmpty) {
                                weightedWinningPositionsBlack += 100
                                blackWinningPositions = row :: blackWinningPositions
                            }
                            else {
                                if (blackWinningPositions.tail.isEmpty &&
                                    (row - blackWinningPositions.head) % 2 == 1) {
                                    weightedWinningPositionsBlack +=
                                        (board.rows - (row - blackWinningPositions.head)) * 1000
                                    blackWinningPositions = row :: blackWinningPositions
                                }
                            }
                        }
                        else if (whiteWinningPositions.tail.isEmpty) {
                            if ((row - whiteWinningPositions.head) % 2 == 0 && blackWinningPositions.isEmpty) {
                                weightedWinningPositionsBlack += 100
                                blackWinningPositions = row :: blackWinningPositions
                            }
                        }
                    }
                }
                row = row + 1
            } while (row <= maxRowIndex)

            col = col - 1
        } while (col >= 0)

        // println(sumOfSquareWeights+" :: "+weightedWinningPositionsWhite+"<=>"+weightedWinningPositionsBlack)
        sumOfSquareWeights + weightedWinningPositionsWhite - weightedWinningPositionsBlack
    }

}


