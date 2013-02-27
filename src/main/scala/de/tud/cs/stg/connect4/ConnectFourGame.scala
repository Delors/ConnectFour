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

import scala.collection.Iterator
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

/**
  * Represents the current game state: which fields are occupied by which player and which player has
  * to make the next move. The game state object is immutable. I.e., every update/move creates a new instance.
  *
  * For a detailed discussion of "Connect Four" go to:
  * [[http://www.connectfour.net/Files/connect4.pdf Connect Four - Master Thesis]]
  *
  * ==Overview==
  * This is an implementation of the game "Connect Four" that uses the minimax algorithm to implement the
  * artificial intelligence.
  *
  * This implementation is primarily used for teaching purposes (w.r.t. the minimax/negamax algorithm). To
  * this end, the default scoring function is very simple, but is still strong enough to make it a reasonable
  * opponent for a human player. However, if the board is extremely small (4x4) the ai plays perfectly.
  *
  * In the following we use the following terminology:
  *  - The game is always played by exactly two ''PLAYER''s on a ''BOARD'' that typically has 6 ''ROWS'' x
  *     7 ''COLUMNS'' = 42 ''SQUARES''.
  *  - The first player is called the ''WHITE'' player ([[de.tud.cs.stg.connect4.Player.White]]) and has the
  *     integer identifier 0.
  *  - The second player is called the ''BLACK'' player ([[de.tud.cs.stg.connect4.Player.Black]]) and has
  *     the integer identifier 1.
  *  - Given a board with 6 rows and 7 columns each player has 21 ''MEN''. Dropping a man in a column is
  *     called a ''MOVE''.
  *
  * When we have six rows and seven columns, the squares are indexed as follows :
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
  * Two long values are used to encode the game state. The game state encompasses the information
  * which squares are occupied (encoded using the first (ROWS x COLUMNS) bits of the first long value) and -
  * if so – by which player a square is occupied (using the first ROWS x COLUMNS bits of the second long
  * value). Hence, a specific bit of the second long value has a meaning if – and only if – the same
  * bit is set in the first long value.
  * Additionally, the information is stored which player has to make the next move (using the most
  * significant bit (the 64th) of the second long value). The other bits of the long values are not used.
  * To make the code easily comprehensible, two '''value classes''' are defined that wrap the long values and
  * implement the required functionality (see [[de.tud.cs.stg.connect4.OccupiedInfo]] and
  * [[de.tud.cs.stg.connect4.PlayerInfo]]).
  *
  * Overall, this design enables a reasonable performance and facilitates the exploration of a couple million
  * game states per second (Java 7, Intel Core i7 3GHZ and 8Gb Ram for the JVM).
  *
  * For example, to efficiently check whether the game is finished; i.e., some player was able to connect four
  * men, all winning conditions are encoded using special bit masks (see [[de.tud.cs.stg.connect4.Board]])
  * and these masks are just matched (by means of the standard binary-and ("&") operator) against both long
  * values to determine whether a certain player has won the game.
  *
  * @param board The board on which the game will be played. The board is shared across all instances
  * 	created using this `ConnectFourGame` object
  * @param score A function that scores a specific board. The value has to be positive if the white player has an
  *     advantage and negative if the black player has an advantage. The value has to be in the range
  *     (`-Int.MaxValue`..`Int.MaxValue`) unless the white or the black player will definitively win. In that
  *     case the value is either `Int.MaxValue` or `-Int.MaxValue`.
  *     '''The value Int.MinValue == -Int.MaxValue-1 is reserved for internal purposes'''.
  * @param occupiedSquares The number of occupied squares.
  * @param occupiedInfo Encodes which squares are occupied.
  * @param playerInfo In combination with `occupiedInfo` encodes the information which player occupies a
  *     square and also encodes the information which player has to make the next move.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class ConnectFourGame(
        final val occupiedSquares: Int = 0,
        final val occupiedInfo: OccupiedInfo = OccupiedInfo.create(),
        final val playerInfo: PlayerInfo = PlayerInfo.create())(
                implicit final val board: Board,
                implicit final val score: (Board, Int, OccupiedInfo, PlayerInfo) ⇒ Int) {

    private def this(
        board: Board,
        score: (Board, Int, OccupiedInfo, PlayerInfo) ⇒ Int,
        occupiedSquares: Int = 0,
        occupiedInfo: OccupiedInfo = OccupiedInfo.create(),
        playerInfo: PlayerInfo = PlayerInfo.create()) {
        this(occupiedSquares, occupiedInfo, playerInfo)(board, score)
    }

    /**
      * Iterates over the masks that select the squares where the current player is allowed to put its
      * next man.
      */
    def nextMoves(): Iterator[Mask] = nextMoves(this.occupiedInfo)

    /**
      * Returns the masks that selects the squares where the current player is allowed to put
      * its next man given the specific board configuration.
      *
      * The iterator will start with the column in the middle and will then return the next free column to
      * the left, the right, the left and so on. I.e., if we have seven columns, the order in which the
      * columns are tested for free squares is: 3,2,4,1,5,0,6.
      */
    protected[connect4] def nextMoves(occupiedInfo: OccupiedInfo): Iterator[Mask] = {
        new Iterator[Mask] {

            private var column = -1
            private var count = 0

            private def advance() {
                // if we have 7 columns, the sequence: 3+0,3-1,3+1,3-2,3+2,3-3,3+3 is generated
                val cols = board.cols
                do {
                    val currentCount = this.count
                    val newCount = currentCount + 1
                    this.count = newCount
                    column = (cols / 2) + (if ((newCount % 2) == 0) -(newCount / 2) else newCount / 2)
                } while (count <= cols &&
                    occupiedInfo.areOccupied(Mask(board.upperLeftSquareMask.value << column)))
            }

            advance()

            def hasNext(): Boolean = count <= board.cols

            def next(): Mask = {
                val columnMask = board.columnMasks(column)
                val filteredOccupiedInfo = occupiedInfo.filter(columnMask)
                val mask = {
                    if (filteredOccupiedInfo.allSquaresEmpty)
                        Mask.lowestSquareInColumn(column)
                    else
                        Mask((filteredOccupiedInfo.board ^ columnMask.value) &
                            (filteredOccupiedInfo.board << board.cols))
                }
                advance()
                mask
            }
        }
    }

    /**
      * If a move exists that leads to an immediate win of the ''current player'' that move is
      * returned. All other moves are pruned. If no winning move exists, but a forced move is found;
      * i.e., a move that prevents the current player from immediately loosing the game, the iterator will
      * only return that move.
      *
      * ==Implementation Note==
      * The complexity of identifying a killer move is roughly comparable to the effort that is necessary
      * when exploring and scoring the last level of the search tree. Hence, calling this function is not
      * meaningful when analyzing the last level.
      */
    protected[connect4] def nextMovesWithKillerMoveIdentification(
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): Iterator[Mask] = {

        val it = nextMoves(occupiedInfo)
        var preventImmediateLossMove: Mask = Mask.Illegal
        var otherMoves: List[Mask] = List()
        do {
            val move = it.next
            val updatedOccupiedInfo = occupiedInfo.update(move)
            if (determineState(move, updatedOccupiedInfo, playerInfo.update(move)).hasWinner) {
                return Iterator.single(move)
            }
            else if ( // The identification of a second move that prevents an immediate loss is not helpful...
            preventImmediateLossMove.isIllegal &&
                determineState(
                    move,
                    updatedOccupiedInfo,
                    playerInfo.update(move, playerInfo.turnOf.opponent)).hasWinner) {
                // we can not immediately return this result as there may also be a winning move 
                preventImmediateLossMove = move
            }
            else {
                otherMoves = move :: otherMoves
            }
        } while (it.hasNext)

        if (preventImmediateLossMove.isLegal)
            return Iterator.single(preventImmediateLossMove)

        otherMoves.reverseIterator
    }

    /**
      * Returns `Some(squareId)` of the lowest square in the respective column that is free or `None`
      * otherwise.
      *
      * ==Note==
      * This method is not optimized and is therefore not intended to be used by the ai – it can, however,
      * be used in an interactive game.
      *
      * @param column A valid column identifier ([0..[[de.tud.cs.stg.connect4.Board.maxColIndex]]]).
      * @return The id of the lowest square in the given column that is free.
      */
    def lowestFreeSquareInColumn(column: Int): Option[Int] =
        (column until board.squares by board.cols) collectFirst ({
            case squareId if !occupiedInfo.isOccupied(squareId) ⇒ squareId
        })

    /**
      * True if all squares of the current board are occupied.
      */
    def allSquaresOccupied(): Boolean = occupiedInfo.areOccupied(board.topRowMask)

    /**
      * Creates a new `ConnectFourGame` object given the new board configuration.
      *
      * This method can be used by subclasses to create more specific `ConnectFourGame` objects.
      */
    protected[connect4] def newConnectFourGame(
        occupiedSquares: Int,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): ConnectFourGame = {

        new ConnectFourGame(board, score, occupiedSquares, occupiedInfo, playerInfo)
    }

    /**
      * Creates a new `ConnectFourGame` object by putting a man in the given square and updating the
      * information which player has to make the next move.
      *
      * ==Prerequisites==
      * The squares below the square have to be occupied and the specified square has to be empty.
      * However, both is not checked for performance reasons.
      *
      * @param singleSquareMask The mask that masks the square where the next man is put.
      * @return The updated game object.
      */
    def makeMove(singleSquareMask: Mask): ConnectFourGame = {
        newConnectFourGame(
            occupiedSquares + 1,
            occupiedInfo.update(singleSquareMask),
            playerInfo.update(singleSquareMask))
    }

    /**
      * Determines the current state ([[de.tud.cs.stg.connect4.State]]) of the game.
      *
      * Either returns:
      *  - DRAWN (0l) if the game is finished (i.e., all squares are occupied), but no player has won.
      *  - NOT_FINISHED (-1l) if no player has won so far and some squares are empty.
      *  - State(Mask) where the mask (some positive value >= 15 (00000....00001111)) identifies the squares
      *     with the four connected men.
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
      * Every call to this method (re)analyses the board given the last move.
      */
    protected[connect4] def determineState(
        lastMove: Mask,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): State =
        board.determineState(
            board.masksForConnect4CheckForSquare(board.squareId(lastMove)),
            occupiedInfo,
            playerInfo)

    /**
      * Assesses the current board form the perspective of the current player. The value will be between
      * `-Int.MaxValue` and `Int.MaxValue`. A positive value indicates that the current player has an
      * advantage. A negative value indicates that the opponent has an advantage. If the value
      * is (-)Int.MaxValue the current(opponent) player can/will win. If the value is 0 the game is
      * drawn or the ai was not able to determine if any player has an advantage.
      *
      * Subclasses are explicitly allowed to implement ''fail-soft alpha-beta-pruning''. Hence, two moves
      * that are rated equally are not necessarily equally good when the second move was evaluated given
      * some specific alpha-beta bounds and some pruning was done.
      *
      * ==Precondition==
      * Before the immediate last move the game was not already finished. I.e., the return value is
      * not defined when the game was already won by a player before the last move.
      *
      * @param cacheManager The cache that is used to store the score of specific game states.
      * @param occupiedSquares The number of occupied squares.
      * @param occupiedInfo The information which squares are currently occupied.
      * @param playerInfo The information to which player an occupied square belongs.
      * @param lastMove The last move that was made and which led to the current game state.
      * @param depth The remaining number of levels of the search tree that may be explored.
      * @param alpha The best value that the current player can achieve (Initially `-Int.MaxValue`).
      * @param beta The best value that the opponent can achieve (Initially `Int.MaxValue`).
      */
    protected[connect4] def negamax(
        cacheManager: CacheManager,
        occupiedSquares: Int,
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

        // 1. check if the game is finished (terminal test)
        val state = determineState(lastMove, occupiedInfo, playerInfo)
        if (state.isDrawn) return 0
        if (state.hasWinner /* <=> the player who made the last move has won */ ) return ConnectFourGame.Lost

        // 2. check if we have to abort exploring the search tree and have to assess the game state
        if (depth <= 0) {
            if (playerInfo.isWhitesTurn())
                return score(board, occupiedSquares, occupiedInfo, playerInfo)
            else
                return -score(board, occupiedSquares, occupiedInfo, playerInfo)
        }

        // 3. continue exploring the search tree
        val nextMoves =
            if (depth > 1)
                this.nextMovesWithKillerMoveIdentification(occupiedInfo, playerInfo)
            else
                this.nextMoves(occupiedInfo)
        var valueOfBestMove = Int.MinValue // we always maximize (negamax)!
        var newAlpha = alpha
        do { // for each legal move perform a recursive call to continue exploring the search tree
            val nextMove: Mask = nextMoves.next
            val value = evaluateMove(
                cacheManager,
                nextMove, occupiedSquares, occupiedInfo, playerInfo,
                depth - 1, -beta, -newAlpha
            )
            // fail-soft alpha-beta pruning
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

    protected[connect4] trait CacheManager {

        /**
          * The current game configuration that is relevant w.r.t. caching consists of the information
          * which squares are occupied by which player and whoes turn it is.
          */
        type Configuration = (OccupiedInfo, PlayerInfo)

        /**
          * Reusing a calculated score of a specific configuration is only valid if the current alpha and
          * beta bounds are within in the original alpha and beta bounds.
          *
          * The first value is the alpha bound, the second is the beta bound and third value is the score.
          */
        type CurrentScore = (Int, Int, Int) /* alpha,beta,score*/

        def update(configuration: Configuration, score: CurrentScore): Unit

        def get(configuration: Configuration): Option[CurrentScore]

        def update(move: Mask): CacheManager

        /**
          * Returns true if caching (caching the configuration/trying to look up the score of a configuration
          * in the cache) potentially makes sense. I.e., it does not make sense to cache the node at the first
          * level of the search tree as there will never be a successful lookup of this configuration.
          */
        def doCaching: Boolean

    }

    protected[connect4] class RootCacheManager extends CacheManager {

        import scala.collection.mutable.Map;

        private final val cache: Map[Configuration, CurrentScore] = Map()

        final def update(configuration: Configuration, value: CurrentScore) = cache.update(configuration, value)

        final def get(configuration: Configuration): Option[CurrentScore] = cache.get(configuration)

        final def update(move: Mask): CacheManager = new PreCachePhaseCacheManager(this).update(move)

        final def doCaching = false

        /**
         * 
         */
        final val inCachePhaseCacheManager = new DelegatingCacheManager {

            final val rootCacheManager: RootCacheManager = RootCacheManager.this

            final def doCaching = true

            final def update(move: Mask): CacheManager = this
        }
    }

    /**
      * Implementation of the `CacheManager` trait that delegates all caching related calls to a
      * `RootCacheManager`.
      */
    protected[connect4] abstract class DelegatingCacheManager extends CacheManager {

        def rootCacheManager: RootCacheManager

        final def update(configuration: Configuration, score: CurrentScore): Unit =
            rootCacheManager.update(configuration, score)

        final def get(configuration: Configuration): Option[CurrentScore] =
            rootCacheManager.get(configuration)

    }

    /**
      * Cache manager that tracks the first moves to filter out board configurations that will only be
      * encountered once. E.g., caching the score of the first move by the current player does not make
      * sense as this configuration will never be encountered again during the exploration of the search
      * tree.   
      */
    protected[connect4] class PreCachePhaseCacheManager(
            final val rootCacheManager: RootCacheManager,
            final val currentPlayer: Player,
            final val menPerColumn: IndexedSeq[Int],
            final val menPerLogicalRowWhite: IndexedSeq[Int],
            final val menPerLogicalRowBlack: IndexedSeq[Int]) extends DelegatingCacheManager {

        private def this(
            rootCacheManager: RootCacheManager,
            menPerColumn: IndexedSeq[Int],
            menPerLogicalRow: IndexedSeq[Int]) {
            this(rootCacheManager, Player.White, menPerColumn, menPerLogicalRow, menPerLogicalRow)
        }

        def this(rootCacheManager: RootCacheManager) {
            this(rootCacheManager, IndexedSeq.fill(board.cols) { 0 }, IndexedSeq.fill(board.rows + 1) { 0 })
        }

        final def doCaching = { false }

        def update(move: Mask): CacheManager = {
            val column = board.column(move)
            val newMenPerColumn = menPerColumn(column) + 1
            val updatedMenPerColumn = menPerColumn.updated(column, newMenPerColumn)

            if (currentPlayer.isWhite) {
                val newMenPerLogicalRowWhite = menPerLogicalRowWhite(newMenPerColumn) + 1
                if (newMenPerLogicalRowWhite >= 2 && menPerLogicalRowBlack.exists(_ >= 2)) {
                    return rootCacheManager.inCachePhaseCacheManager
                }
                else {
                    new PreCachePhaseCacheManager(
                        rootCacheManager,
                        currentPlayer.opponent,
                        updatedMenPerColumn,
                        menPerLogicalRowWhite.updated(newMenPerColumn, newMenPerLogicalRowWhite),
                        menPerLogicalRowBlack)
                }
            }
            else {
                val newMenPerLogicalRowBlack = menPerLogicalRowBlack(newMenPerColumn) + 1
                if (newMenPerLogicalRowBlack >= 2 && menPerLogicalRowWhite.exists(_ >= 2)) {
                    return rootCacheManager.inCachePhaseCacheManager
                }
                else {
                    new PreCachePhaseCacheManager(
                        rootCacheManager,
                        currentPlayer.opponent,
                        updatedMenPerColumn,
                        menPerLogicalRowWhite,
                        menPerLogicalRowBlack.updated(newMenPerColumn, newMenPerLogicalRowBlack))
                }
            }
        }
    }

    /**
      * Evaluates the given move w.r.t. the given game state. This method is used by the `negamax` method when
      * exploring the search tree.
      *
      * ==Note==
      * This method was introduced as a hook to enable subclasses to intercept recursive calls to `negamax`.
      */
    protected[connect4] def evaluateMove(
        cacheManager: CacheManager,
        nextMove: Mask,
        occupiedSquares: Int,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo,
        depth: Int,
        alpha: Int,
        beta: Int): Int = {

        val updatedOccupiedInfo = occupiedInfo.update(nextMove)
        val updatedPlayerInfo = playerInfo.update(nextMove)
        val updatedCacheManager = cacheManager.update(nextMove)

        def callNegamax(cacheManager: CacheManager): Int =
            -negamax(
                cacheManager,
                occupiedSquares + 1, updatedOccupiedInfo, updatedPlayerInfo,
                nextMove, depth, alpha, beta)

        if (updatedCacheManager.doCaching && depth <= 16) {

            def putIntoCache(): Int = {
                val score = callNegamax(updatedCacheManager)
                // If the score is equal to Won or Lost then the score would not change if the calculation would
                // be repeated with relaxed alpha and beta bounds, hence, we can cache the score using relaxed 
                // bounds.
                if (score == ConnectFourGame.Lost || score == ConnectFourGame.Won)
                    updatedCacheManager.update(
                        (updatedOccupiedInfo, updatedPlayerInfo),
                        (ConnectFourGame.Lost, ConnectFourGame.Won, score))
                else
                    updatedCacheManager.update(
                        (updatedOccupiedInfo, updatedPlayerInfo),
                        (alpha, beta, score))
                score
            }

            if (depth > 1 /*caching "end configurations" is not effective*/ ) {
                updatedCacheManager.get(updatedOccupiedInfo, updatedPlayerInfo) match {
                    case Some((cachedAlpha, cachedBeta, cachedScore)) ⇒ {
                        if (alpha >= cachedAlpha && beta <= cachedBeta) {
                            cachedScore
                        }
                        else {
                            // The lookup was not directly successful, hence, we have to calculate the score.
                            // After that, however, we may still be able to use some of the cached bounds to
                            // calculate new relaxed bounds.

                            val score = callNegamax(updatedCacheManager)
                            if (score == ConnectFourGame.Lost || score == ConnectFourGame.Won) {
                                updatedCacheManager.update(
                                    (updatedOccupiedInfo, updatedPlayerInfo),
                                    (ConnectFourGame.Lost, ConnectFourGame.Won, score))
                            }
                            else if (score == cachedScore && (alpha < cachedBeta || cachedAlpha < beta)) {
                                // The bounds are overlapping and the score was identical, hence, we can
                                // use the outer most bounds; i.e., we can relax the bounds when compared
                                // to each individual result.
                                updatedCacheManager.update(
                                    (updatedOccupiedInfo, updatedPlayerInfo),
                                    (Math.min(alpha, cachedAlpha), Math.max(beta, cachedBeta), score))
                            }
                            else {
                                // We always update the cache, as tests have shown that this is more effective
                                // than leaving the old value in the cache or updating the value base on
                                // the result which has a broader validity, a lesser alpha bound, a higher
                                // beta bound,...
                                updatedCacheManager.update(
                                    (updatedOccupiedInfo, updatedPlayerInfo),
                                    (alpha, beta, score))
                            }
                            score
                        }
                    }
                    case _ ⇒ {
                        putIntoCache()
                    }
                }
            }
            else
                callNegamax(updatedCacheManager)
        }
        else {
            callNegamax(updatedCacheManager)
        }
    }

    /**
      * Evaluates the given move w.r.t. the current game state.
      *
      * ==Note==
      * This method serves as a hook that enables subclasses to intercept the initial call to `negamax`
      * done by the `proposeMove` method.
      */
    protected[connect4] def evaluateMove(
        cacheManager: CacheManager,
        nextMove: Mask,
        depth: Int,
        alpha: Int,
        beta: Int): Int = {

        -negamax(
            cacheManager.update(nextMove),
            occupiedSquares + 1,
            occupiedInfo.update(nextMove),
            playerInfo.update(nextMove),
            nextMove,
            depth,
            alpha,
            beta)
    }

    /**
      * Proposes a ''move'' given the current game state. The negamax algorithm is used to determine it.
      *
      * @param maxDepth The maximum number of levels of the search tree that will be explored.
      */
    def proposeMove(maxDepth: Int): Mask = {

        val cacheManager = new RootCacheManager

        val nextMoves = this.nextMovesWithKillerMoveIdentification(this.occupiedInfo, this.playerInfo)
        var bestMove = nextMoves.next()
        var alpha = evaluateMove(cacheManager, bestMove, maxDepth - 1, -Int.MaxValue, Int.MaxValue)
        while (nextMoves.hasNext) { // for each legal move...
            val nextMove: Mask = nextMoves.next()
            val value = evaluateMove(cacheManager, nextMove, maxDepth - 1, -Int.MaxValue, -alpha)
            // Beware: the negamax is implemented using fail-soft alpha-beta-pruning; hence, if we would
            // choose a move with a value that is equal to the value of a previously evaluated move, it
            // could lead to a move that is actually advantageous for the opponent because a the part of
            // the search tree that was cut may contain a move that is '''even more'' advantageous.
            // Therefore, it is not directly possible to get all equally good moves and we have to
            // use ">" here instead of ">=".
            if (value > alpha) {
                bestMove = nextMove
                alpha = value
            }
        }

        if (alpha == -Int.MaxValue && maxDepth > 2) {
            // When the AI determines that it will always loose in the long run (when the opponent plays 
            // perfectly) it may still be possible to prevent the opponent from winning immediately and,
            // hence, if the opponent does not play perfectly, to still win the game. However, to calculate
            // a meaningful move, we simply reduce the number of levels of the search tree that we want to 
            // explore.
            if (maxDepth > 6)
                proposeMove(6)
            else
                proposeMove(2)
        }
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
    override def toString() =
        "Next Player: "+playerInfo.turnOf()+
            "\nBoard ("+occupiedSquares+"/"+board.squares+"):\n"+
            boardToString

}

/**
  * Factory object to create specific types of ''Connect Four'' games.
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
        score: (Board, Int, OccupiedInfo, PlayerInfo) ⇒ Int = scoreBasedOnLinesOfThreeConnectedMen) =
        new ConnectFourGame(board, score)

    /**
      * Creates a ''Connect Four'' game that prints out some debugging information.
      *
      * @param board The board used for playing connect four.
      * @param score The scoring function that will be used to score the leaf-nodes of the search tree that
      * 	are not final states.
      */
    def withDebug(board: Board,
                  score: (Board, Int, OccupiedInfo, PlayerInfo) ⇒ Int = scoreBasedOnLinesOfThreeConnectedMen) = {

        class DebugConnectFourGame(occupiedSquares: Int, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo)
                extends ConnectFourGame(board, score, occupiedSquares, occupiedInfo, playerInfo) {

            override protected[connect4] def newConnectFourGame(
                occupiedSquares: Int,
                occupiedInfo: OccupiedInfo,
                playerInfo: PlayerInfo): ConnectFourGame = {

                new DebugConnectFourGame(occupiedSquares, occupiedInfo, playerInfo)
            }

            override protected[connect4] def evaluateMove(
                cacheManager: CacheManager,
                nextMove: Mask,
                depth: Int,
                alpha: Int,
                beta: Int): Int = {

                val score = super.evaluateMove(cacheManager, nextMove, depth, alpha, beta)

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

        new DebugConnectFourGame(0, OccupiedInfo.create(), PlayerInfo.create())
    }

    /**
      * Creates a `ConnectFourGame` object that always prints out the search tree using Graphviz's Dot
      * language.
      *
      * If you want to print out the search tree, you should limit the maximum depth of the search tree to
      * something like 4 or 5.
      *
      * @param board The board used for playing ''Connect Four''.
      * @param score The heuristic scoring function that will be used to score the leaf-nodes of the search
      *     tree that are not final states.
      */
    def withDotOutput(board: Board,
                      score: (Board, Int, OccupiedInfo, PlayerInfo) ⇒ Int = scoreBasedOnLinesOfThreeConnectedMen) = {

        class ConnectFourGameWithDotOutput(occupiedSquares: Int, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo)
                extends ConnectFourGame(board, score, occupiedSquares, occupiedInfo, playerInfo) {

            override protected[connect4] def newConnectFourGame(
                occupiedSquares: Int,
                occupiedInfo: OccupiedInfo,
                playerInfo: PlayerInfo) = {

                new ConnectFourGameWithDotOutput(occupiedSquares, occupiedInfo, playerInfo)
            }

            private var currentNodeLabel: String = _

            override protected[connect4] def evaluateMove(
                cacheManager: CacheManager,
                nextMove: Mask,
                occupiedSquares: Int,
                occupiedInfo: OccupiedInfo,
                playerInfo: PlayerInfo,
                depth: Int,
                alpha: Int,
                beta: Int): Int = {

                val oldLabel = currentNodeLabel
                currentNodeLabel += String.valueOf(board.column(nextMove))+"↓"
                println("\""+oldLabel+"\" -> "+"\""+currentNodeLabel+"\";")
                val score = super.evaluateMove(cacheManager, nextMove, occupiedSquares, occupiedInfo, playerInfo, depth, alpha, beta)
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
                cacheManager: CacheManager,
                nextMove: Mask,
                depth: Int,
                alpha: Int,
                beta: Int): Int = {

                currentNodeLabel = String.valueOf(board.column(nextMove))+"↓"
                val score = super.evaluateMove(cacheManager, nextMove, depth, alpha, beta)
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

        new ConnectFourGameWithDotOutput(0, OccupiedInfo.create(), PlayerInfo.create())
    }

    /**
      * A scoring function that assigns the same value to all boards.
      *
      * ==Use Case==
      * This scoring function is primarily useful for testing purposes.
      */
    def fixedScore(
        board: Board,
        occupiedSquares: Int,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): Int = 0

    /**
      * A scoring function that assigns a random value to the given ''Connect Four'' board.
      *
      * ==Use Case==
      * This scoring function is primarily useful for testing purposes. I.e., to test whether some "real"
      * scoring function is actually beneficial when compared to an ai that just drops a man randomly into
      * some column which has some empty squares.
      * Recall, that – if we do a deep exploration of the search tree – the minimax algorithm will still
      * prevent the ai from doing moves that will lead to a situation where the ai will certainly loose. I.e.,
      * a man is dropped into some random column only when no immediate threat or winning opportunity exists.
      * However, using this scoring function the ai does not actively pursue the goal of building a winning
      * position.
      */
    def randomScore(seed: Long = 1234567890l): (Board, Int, OccupiedInfo, PlayerInfo) ⇒ Int = {
        val rng = new java.util.Random(seed);
        (board: Board, occupiedSquares: Int, occupiedInfo: OccupiedInfo, playerInfo: PlayerInfo) ⇒ {
            rng.nextInt(21) - 10
        }
    }

    /**
      * Scores a ''Connect Four'' board by considering the weight of the squares occupied by each player.
      * This scoring function is simple and fast, but – in combination with a decent evaluation of the search
      * tree – still makes up for a reasonable opponent.
      */
    def scoreBasedOnSquareWeights(
        board: Board,
        occupiedSquares: Int,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): Int = {

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
      * This scoring function scores a board by looking for existing lines of three connected men that can be
      * extended to a line of four connected men.
      */
    def scoreBasedOnLinesOfThreeConnectedMen(
        board: Board,
        occupiedSquares: Int,
        occupiedInfo: OccupiedInfo,
        playerInfo: PlayerInfo): Int = {

        val maxRowIndex = board.maxRowIndex

        var sumOfSquareWeights = 0
        var weightedWinningPositionsWhite, weightedWinningPositionsBlack =
            board.sumOfSquareWeightsTimeEssentialSquareWeights

        var col = board.cols - 1
        do {
            var row = 0
            // list of relevant winning positions per column; per column we can have at most two relevant
            // winning positions w.r.t. the still empty squares
            var whiteWinningPositions, blackWinningPositions = List[Int]()
            do {
                val squareId = board.squareId(row, col)
                if (occupiedInfo.isOccupied(squareId)) {
                    // if no line of three connected men is found, the ai should at least try to put their
                    // men in squares that are of particular value..
                    val squareWeight = board.squareWeights(squareId)
                    sumOfSquareWeights +=
                        (if (playerInfo.isWhite(squareId))
                            squareWeight
                        else
                            -squareWeight
                        ) * board.essentialSquareWeights(squareId)
                }
                else {
                    val squareMask = Mask.forSquare(squareId)
                    val potentialOI = occupiedInfo.update(squareMask)

                    val potentialPIWhite = playerInfo.update(squareMask, Player.White)
                    if (board.determineState(board.masksForConnect4CheckForSquare(squareId), potentialOI, potentialPIWhite).hasWinner) {
                        if (blackWinningPositions.isEmpty) {
                            if (whiteWinningPositions.isEmpty) {
                                weightedWinningPositionsWhite *= 2
                                whiteWinningPositions = row :: whiteWinningPositions
                            }
                            else {
                                if (whiteWinningPositions.tail.isEmpty &&
                                    (row - whiteWinningPositions.head) % 2 == 1) {
                                    weightedWinningPositionsWhite *=
                                        // this is a "sure win" in this column!
                                        (board.rows - (row - whiteWinningPositions.head)) * 10
                                    whiteWinningPositions = row :: whiteWinningPositions
                                }
                            }
                        }
                        else if (blackWinningPositions.tail.isEmpty) {
                            if ((row - blackWinningPositions.head) % 2 == 0 && whiteWinningPositions.isEmpty) {
                                weightedWinningPositionsWhite *= 2
                                whiteWinningPositions = row :: whiteWinningPositions
                            } // else... a position immediately above a winning position of black is only of 
                            // value if we can force the other player to block/to give up its winning position 
                        }
                        // else... if black already has two winning positions in that column another line of 
                        // three connected men is not helpful
                    }

                    val potentialPIBlack = playerInfo.update(squareMask, Player.Black)
                    if (board.determineState(board.masksForConnect4CheckForSquare(squareId), potentialOI, potentialPIBlack).hasWinner) {
                        if (whiteWinningPositions.isEmpty) {
                            if (blackWinningPositions.isEmpty) {
                                weightedWinningPositionsBlack *= 2
                                blackWinningPositions = row :: blackWinningPositions
                            }
                            else {
                                if (blackWinningPositions.tail.isEmpty &&
                                    (row - blackWinningPositions.head) % 2 == 1) {
                                    weightedWinningPositionsBlack +=
                                        (board.rows - (row - blackWinningPositions.head)) * 10
                                    blackWinningPositions = row :: blackWinningPositions
                                }
                            }
                        }
                        else if (whiteWinningPositions.tail.isEmpty) {
                            if ((row - whiteWinningPositions.head) % 2 == 0 && blackWinningPositions.isEmpty) {
                                weightedWinningPositionsBlack *= 2
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


