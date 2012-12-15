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

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

/**
  * An implementation of the game "Connect Four" that uses the minimax algorithm to implement the AI.
  * For a detailed discussion of "Connect Four" go to:
  * [[http://www.connectfour.net/Files/connect4.pdf Connect Four - Master Thesis]]
  *
  * ==Overview==
  * This implementation is primarily used for teaching purposes (w.r.t. the minimax algorithm), but the
  * ai is still strong enough to make it a reasonable opponent for a human player. However, the evaluation
  * function to assess the game state can be considered trivial at best unless the board is extremely small
  * (4x4) - in such a case the ai plays perfectly.
  *
  * In the following we use the following terminology:
  *  - The game is always played by exactly two ''PLAYER''s on a ''BOARD'' that typically has 6 ROWS x 7 COLUMNS =
  * 42 SQUARES.
  *  - The first player is called the ''WHITE'' player and has the integer identifier 0.
  *  - The second player is called the ''BLACK'' player and has the integer identifier 1.
  *  - Each player has 21 MEN. Dropping a man in a column is called a ''MOVE''.
  *
  * The squares are indexed as follows if we have 6 rows and 7 columns:
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
  * You first have to create an instance of this class and specify the configuration of the board.
  * {{{
  * val connectFour = new ConnectFour(Configuration6x7)
  * }}}
  * After that – to start a new game – you have to create a new Game object:
  * {{{
  * var game = new connectFour.Game
  * }}}
  * To update the game state you either call:
  * {{{
  * game = game.makeMove(<COLUMN_ID>).
  * }}}
  * after the user has chosen the column, or if it is the ai's turn:
  * {{{
  * game = game.makeMove(game.bestMove(aiStrength))
  * }}}
  * To evaluate the game state, i.e., to determine whether some user has won or the game is drawn call:
  * {{{
  * game.state()
  * }}}
  *
  * ==Implementation Details==
  * Internally, two long values are use to encode the game state. The game state encompasses the information
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
  *
  * @param configuration Configuration of a specific board. The  implementation supports boards
  *  	with at least 4 rows and 4 columns and which have at most 7 rows and 7 columns.
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class ConnectFour( final val configuration: Configuration = Configuration6x7) {

    import configuration._

    /**
      * Random number generator used by the ai to choose a random move between all moves that are evaluated
      * as equally good.
      */
    private final val RNG = new java.util.Random()

    var successfullCacheLookups = new java.util.concurrent.atomic.AtomicInteger()
    var uselessCalculations = new java.util.concurrent.atomic.AtomicLong()

    /**
      * Represents the current game state: which fields are occupied by which player and which player has
      * to make the next move.
      *
      * ==Note==
      * The game state object is immutable. I.e., every update creates a new instance.
      *
      * @param occupiedBitField Encodes which squares are occupied.
      * @param playerBitField In combination with the `occupiedBitField` encodes the information which player
      *     occupies a square and also encodes the information which player has to make the next move.
      */
    case class Game private (
            private val occupiedBitField: Long,
            private val playerBitField: Long) {

        import Player._

        /**
          * Creates a new empty board and sets the information
          * that it is the first player(ID = 0; White)'s turn.
          */
        def this() {
            this(0l /* all fields are empty */ , 0l)
        }

        /**
          * Returns the list of square ids where the next man can be placed.
          */
        def legalMoves: Buffer[Int] = {
            // In the following, a do-while loop is used to improve the performance. 
            // Scala's for-loops (2.9.x) are (still) slow(er) and this is one of 
            // the core methods used by the minimax algorithm.
            var squares = new ArrayBuffer[Int](COLS)
            var col = 0
            // in each column find the lowest square that is empty
            do {
                var row = 0
                var continue = true
                do {
                    val square: Int = squareId(row, col)
                    if (!isOccupied(square)) {
                        squares.+=(square)
                        continue = false
                    }
                    else {
                        row += 1
                    }
                } while (continue && row <= MAX_ROW_INDEX)
                col += 1
            } while (col <= MAX_COL_INDEX)

            // using square weights to sort the list of legal moves does not (yet) pay off: 
            // squares.sortWith((s1, s2) ⇒ SQUARE_WEIGHTS(s1) < SQUARE_WEIGHTS(s2))

            // sorting by first considering the columns in the middle does not (yet) pay off: 
            //squares.sortWith((s1,s2) => java.lang.Math.abs(column(s1)-3) <= java.lang.Math.abs(column(s2)-3))
            squares
        }

        /**
          * Returns the id (0 == WHITE or 1 == BLACK) of the player that has to make the next move.
          */
        def turnOfPlayer: Long = {
            playerBitField >>> 63
        }

        /**
          * Returns `Some(id)` of the lowest square in the respective column that is free or `None` otherwise.
          *
          * @param column A valid column identifier ([0..MAX_COL_INDEX]).
          * @return The id of the lowest square in the given column that is free.
          */
        def lowestFreeSquareInColumn(column: Int): Option[Int] = {
            (column to SQUARES by COLS) collectFirst ({ case squareId if !isOccupied(squareId) ⇒ squareId })
        }

        /**
          * Tests if the square with the given id is occupied.
          */
        def isOccupied(squareId: Int): Boolean = (occupiedBitField & (1l << squareId)) != 0l

        /**
          * Evaluates if all squares are occupied.
          */
        def allSquaresOccupied: Boolean = (occupiedBitField & TOP_ROW_BOARD_MASK) == TOP_ROW_BOARD_MASK

        /**
          * Returns the id of the player that occupies the given square.
          *
          * The result is only defined iff the square is occupied.
          */
        def playerId(squareId: Int): Int = if ((playerBitField & (1l << squareId)) == 0l) 0 else 1

        /**
          * Returns the player that occupies the squares identified by the given mask.
          * If not all of the squares are occupied or if not all squares are occupied by men of the
          * same player, None is returned.
          */
        def player(mask: Mask): Option[Player.Value] = {
            if ((occupiedBitField & mask) == mask) {
                playerBitField & mask match {
                    case `mask` ⇒ Some(Player.BLACK)
                    case 0l     ⇒ Some(Player.WHITE)
                    case _      ⇒ None
                }
            }
            else {
                None
            }
        }

        /**
          * Creating a new game state by putting a man in the given square and updating the
          * information which player has to make the next move.
          */
        def makeMove(squareId: Int): Game = {
            val squareMask = 1l << squareId
            new Game(
                occupiedBitField | squareMask /* put a man in the square */ ,
                if (turnOfPlayer == 0l)
                    playerBitField | (1l << 63) /* The BLACK player (ID = 1) is next. */
                else
                    (playerBitField | squareMask) &
                        // we have to mask the most significant bit (the 64th bit)
                        java.lang.Long.MAX_VALUE /* <=> 01111...1111*/
            )
        }

        /**
          * Determines the current state [[de.tud.cs.stg.connect4.State]] of the game.
          *
          * Either returns:
          *  - DRAWN (0l) if the game is finished (i.e., all squares are occupied), but no player has won.
          *  - NOT_FINISHED (-1l) if no player has won so far and some squares are empty
          *  - a mask (some positive value >= 15 (00000....00001111)) that identifies the squares with
          * 	the four connected men.
          *
          * ==Note==
          * Every call to this method (re)calculates the game state.
          */
        def state(): State = {
            // 1. check if we can find a line of four connected men
            val allMasks = ALL_MASKS
            val allQuickCheckMasks = ALL_QUICK_CHECK_MASKS
            var o = 0
            do {
                val quickCheckMasks = allQuickCheckMasks(o)
                val masks = allMasks(o)
                var x = 0
                val xMax = masks.length
                do { // for all colums, rows, diagonals...
                    val quickCheckMask = quickCheckMasks(x)
                    if ((occupiedBitField & quickCheckMask) == quickCheckMask) {
                        val perLineMasks = masks(x)
                        var y = 0
                        val yMax = perLineMasks.length
                        do { // for all potential lines of four connected men in a row, column, diagonal... 
                            val mask = perLineMasks(y)
                            if ((occupiedBitField & mask) == mask) {
                                (playerBitField & mask) match {
                                    case `mask` ⇒ return mask
                                    case 0l     ⇒ return mask
                                    case _      ⇒ /*continue*/
                                }
                            }
                            y += 1
                        } while (y < yMax)
                    }
                    x += 1
                } while (x < xMax)
                o += 1
            } while (o < 4 /* we can have a line of four connected men in one of the two diagonals or in a column or in a row*/ )

            // 2. check if the game is finished 
            if (allSquaresOccupied)
                return DRAWN

            // 3. the game is not yet decided
            NOT_FINISHED
        }

        def assess() {
            
        }
        
        //        private def sumSquareWeights(playerId: Int): Int = {
        //            //SHORT, BUT INEFFICIENT: 
        //            //(0 /: ((0 until 42).filter(isOccupied(_)).filter(playerId(_) == playerID)))(_ + SQUARE_WEIGHTS(_))
        //            var result = 0
        //            var squareId = 0
        //            var mask = 1l
        //            do {
        //                if ((occupiedBitField & mask) != 0l) {
        //                    val pID = (playerBitField & mask)
        //                    if ((pID == 0l && playerId == 0) || (pID != 0l && playerId == 1))
        //                        result += SQUARE_WEIGHTS(squareId)
        //                }
        //                mask = mask << 1
        //                squareId += 1
        //            } while (squareId < 42)
        //            result
        //        }
        //
        //        /**
        //          * Assessment of the current board based on the idea is that it is advantageous to occupy as many
        //          * squares as possible that are part of as many lines of four connected men as possible. The value
        //          * is positive if white has an advantage, negative otherwise.
        //          */
        //        private def assess(): Int = {
        //            val countsWhite = new Array[Int](DIFFERENT_SQUARE_WEIGHTS)
        //            val countsBlack = new Array[Int](DIFFERENT_SQUARE_WEIGHTS)
        //            var squareId = 0
        //            var mask = 1l
        //            do {
        //                if (isOccupied(squareId)) {
        //                    if (playerId(squareId) == 0 /*Player.WHITE.id*/ )
        //                        countsWhite(SQUARE_WEIGHTS(squareId) - MIN_SQUARE_WEIGHT) += 1
        //                    else
        //                        countsBlack(SQUARE_WEIGHTS(squareId) - MIN_SQUARE_WEIGHT) += 1
        //                }
        //                mask = mask << 1
        //                squareId += 1
        //            } while (squareId < 42)
        //            var i = 0;
        //            val MAX = DIFFERENT_SQUARE_WEIGHTS
        //            var result = 0
        //            do {
        //                result += (countsWhite(i) - countsBlack(i)) * (i + 1)
        //                i += 1
        //            } while (i < MAX)
        //            result
        //        }

        // We have implemented memoization of game states (caching) by means of a standard
        // Java ConcurrentHashMap as this type of hashmap is (as of Scala 2.9.1 and Java 7) the most
        // efficient data structure if we have multiple concurrent updates (as in this case!)
        import java.util.concurrent.ConcurrentHashMap
        
        // Thoughts on memoization of game states: 
        // At level 3 (i.e. after three moves) we have at most COLS*COLS*COLS nodes (e.g., 7^3 = 343 nodes); 
        // however, 147 nodes represent the same game state. E.g., the state after dropping a man in the 
        // columns: 1(W),2(B),3(W) or 3(W),2(B),1(W) is identical and not distinguishable and the minimax 
        // value will be the same. Hence, calculating it twice is a waste of ressources.
        // At level 4 we have at most COLS^4 nodes => 2.401
        // At level 5 we have at most COLS^5 nodes => 16.807
        // At level 6 we have at most COLS^6 nodes => 117.649
        // At level 7 we have at most COLS^7 nodes => 823.543
        // At level 8 we have at most COLS^8 nodes => 5.764.801
        // At level 9 we have at most COLS^9 nodes => 40.353.607
        // At level 10 we have at most COLS^10 nodes => 282.475.249
        // If assume that we need ~40Byte for storing one game state, we can store approximately 25.000.000
        // game states in one gigabyte.

        /**
          * Assesses the current board. The value will be between -Int.MaxValue and
          * Int.MaxValue. A positive value indicates that the white player (the beginning player) has an
          * advantage. A negative value indicates that the black player has an advantage. If the value
          * is (-)Int.MaxValue the white(black) player can win. If the value is 0 either the game is drawn
          * or the ai was not able to determine if any player has an advantage.
          *
          * ==Precondition==
          * Before the immediate last move the game was not already finished.
          *
          * @param depth The remaining number of levels that should be explored.
          * @param maxDepth The overall number of levels that should be explored.
          * @param cache The cache that is used memoize game states.
          */
        private def minimax(depth: Int, maxDepth: Int, cache: ConcurrentHashMap[Game, Int]): Int = {
            // The following implementation is basically the negamax variant of the minimax algorithm. 
            // I.e., we have a single minimax method instead of two separate `minValue` and `maxValue` methods.
            // We can use the negamax algorithm because with a zero-sum two player game where we strictly 
            // alternate between the players. (max(a,b) = -min(-a,-b)).

            // THE FOLLOWING CODE IS DEVELOPED WITH EFFICIENCY IN MIND!
            // That is, iterator objects and the like are not used for performance reasons. All these design
            // decisions were tested to have a significant (>>10%) overall performance impact.

            // 1. check if the game is finished
            val result = state()
            if (result == DRAWN) {
                return 0
            }
            if (result > 0 /* <=> some player has won*/ ) {
                if (turnOfPlayer == 1 /*Player.BLACK.id*/ )
                    // => now, it's black's turn, but white has already won
                    return Int.MaxValue // white gets positive values...
                else
                    return -Int.MaxValue // .. and black negative ones
            }

            // 2. check if we have to abort exploring the search tree
            if (depth <= 0) {
                return 0 // TODO implement a heuristic evaluation function
            }

            // 3. recursively call the minimax method to continue exploring the search tree
            val factor = if (turnOfPlayer == 1 /*Black*/ ) -1 else 1
            var valueOfBestMove = Int.MinValue
            // [ "HIGHLY INEFFICIENT": for (legalMove ← legalMoves) {...} ]
            val legalMoves = this.legalMoves
            val legalMovesSize = legalMoves.size
            var lm = 0
            do {
                val legalMove = legalMoves(lm)
                val newGameState: Game = makeMove(legalMove)
                var value: Int = {
                    val exploredDepth = maxDepth - depth
                    if (exploredDepth % 3 == 0) {
                        if (cache.containsKey(newGameState)) {
                            successfullCacheLookups.getAndIncrement()
                            cache.get(newGameState)
                        }
                        else {
                            val newValue = newGameState.minimax(depth - 1, maxDepth, cache) * factor
                            if (!(cache.put(newGameState, newValue) == null)) {
                                uselessCalculations.getAndAdd(Math.pow(7, (depth - 1)).toInt)
                                //uselessCalculations.getAndIncrement()
                            }
                            newValue
                        }
                    }
                    else {
                        newGameState.minimax(depth - 1, maxDepth, cache) * factor
                    }
                }
                if (value == Int.MaxValue) {
                    // there will be no better move (maybe there are other equally good moves, but we don't mind!)
                    return value * factor;
                }
                if (value >= valueOfBestMove) {
                    valueOfBestMove = value
                }
                lm += 1
            } while (lm < legalMovesSize)
            valueOfBestMove * factor
        }

        /**
          * Determines the ''best move'' given the current game configuration. The minimax algorithm is used
          * to determine the next move.
          *
          * @param aiStrength The strength of the ai. The strength determines the number of rounds the
          *     the ai looks ahead; a round consists of one move by each player. It should be at least 3 for
          *  	a game that is not too easy. A value of 4 is possible on recent hardware.
          */
        def bestMove(aiStrength: Int): Int = {

            successfullCacheLookups.set(0)
            uselessCalculations.set(0l)

            val startTime = System.currentTimeMillis()
            val depth = aiStrength * 2

            var candidateMoves: List[Int] = Nil
            var valueOfBestMove: Int = Int.MinValue // the result of the minimax is [-Int.MaxValue,Int.MaxValue]

            var factor = if (turnOfPlayer == 0) 1 else -1
            val cache = new ConcurrentHashMap[Game, Int](500000) // new scala.collection.mutable.HashMap[Game, Int]() // with SynchronizedMap[Game,Int]
            val evaluatedMoves = legalMoves.par.map((move) ⇒ {
                val value = makeMove(move).minimax(depth - 1, depth, cache) * factor
                println("Move: "+(column(move))+" => "+(value * factor)+" [Cached configurations: "+cache.size+"; successfull lookups: "+successfullCacheLookups.intValue+"; useless game state evaluations: "+uselessCalculations.longValue+"]")
                (move, value)
            })
            for ((move, value) ← evaluatedMoves.toList /* we don't want to do the following in parallel */ ) {
                if (value == valueOfBestMove) {
                    candidateMoves = move :: candidateMoves
                }
                else if (value > valueOfBestMove) {
                    valueOfBestMove = value
                    candidateMoves = List(move)
                }
            }

            // Given a set of equally good moves, search for those moves that put a men in a square that has 
            // the highest probability to eventually contribute to a line of four connected men 
            var bestMoves = List(candidateMoves.head)
            var bestMoveWeight = SQUARE_WEIGHTS(bestMoves.head)
            for (candidateMove ← candidateMoves.tail) {
                val candidateMoveWeight = SQUARE_WEIGHTS(candidateMove)
                if (candidateMoveWeight == bestMoveWeight) {
                    bestMoves = candidateMove :: bestMoves
                }
                else if (candidateMoveWeight > bestMoveWeight) {
                    bestMoveWeight = candidateMoveWeight
                    bestMoves = List(candidateMove)
                }
            }

            // Finally, if several candidates exist choose an arbitrary one.
            val bestMove = bestMoves(RNG.nextInt(bestMoves.size))

            println("Determined the best move in: "+(System.currentTimeMillis() - startTime) / 1000.0d)

            bestMove
        }

        /**
          * Returns a human-readable representation of the board.
          *
          * E.g.
          * <pre>
          * 5      ◼
          * 4      ○
          * 3      ◼
          * 2      0   ○
          * 1  ◼ ◼ ◼ ◼ ○
          * 0  ○ ◼ ○ ◼ ○   ○
          *
          *    0 1 2 3 4 5 6
          * </pre>
          */
        def boardToString(): String = {
            // we have to start with the upper left-hand corner when generating a human-readable representation
            var string = ""
            for (r ← (0 to MAX_ROW_INDEX).reverse) {
                string += r+"  " // add the row index
                for (c ← 0 to MAX_COL_INDEX) {
                    val sid = squareId(r, c)
                    if (isOccupied(sid)) {
                        if (playerId(sid) == Player.WHITE.id)
                            string += "○ " // White
                        else
                            string += "◼ " // Black   
                    }
                    else {
                        string += "  "
                    }
                }
                string += "\n"
            }
            string += "\n   "
            // add column indexes 
            for (c ← 0 until COLS) string += c+" "
            string
        }

        /**
          * Returns a human readable representation of the current game state.
          */
        override def toString(): String = {
            "Next Player: "+Player(turnOfPlayer.toInt)+"\nBoard:\n"+boardToString
        }
    }
}
