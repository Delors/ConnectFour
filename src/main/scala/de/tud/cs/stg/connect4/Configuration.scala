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

/**
  * Configuration of a specific board. The implementation supports boards that have at least 4 rows and 4
  * columns and that have at most 7 rows and 7 columns.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
trait Configuration {

    /**
      * Recommendation w.r.t. the processing time that is required to evaluate the search tree up to a specific
      * depth.
      */
    val RECOMMENDED_AI_STRENGTH: Int

    /**
      * The board's number of columns (4 <= COLS <= 7).
      */
    val COLS: Int

    /**
      * The board's number of rows (4 <= ROWS <= 7).
      */
    val ROWS: Int

    /**
      * The number of squares.
      */
    final val SQUARES = COLS * ROWS

    protected[connect4] final val MAX_COL_INDEX = COLS - 1
    protected[connect4] final val MAX_ROW_INDEX = ROWS - 1
    protected[connect4] final val MAX_SQUARE_INDEX = SQUARES - 1

    /**
      * The id of the square in the upper right-hand corner.
      */
    protected[connect4] final val UPPER_LEFT_SQUARE_INDEX = (ROWS - 1) * COLS

    /**
      * Returns the id of the square that has the given row and column indexes.
      *
      * @param row The index [0..MAX_ROW_INDEX] of the row.
      * @param col The index [0..MAX_COL_INDEX] of the column.
      */
    final def squareId(row: Int, col: Int): Int = row * COLS + col

    /**
      * Returns the id ([0..ROWS-1]) of the row of the given square.
      *
      * @param squareId A valid square id.
      */
    final def row(squareID: Int): Int = squareID / COLS

    /**
      * Returns the id ([0..COLS-1]) of the column of the given square.
      *
      * @param squareId A valid square id.
      */
    final def column(squareId: Int): Int = squareId % COLS
    
    /**
      * Masks the squares in the top-level row of the board.
      *
      * This mask can, e.g., be used to efficiently check whether all squares are occupied.
      */
    val HIGHEST_ROW_BOARD_MASK: Mask

    /**
      * To get four men connected horizontally – on a board with 7 columns – it is strictly necessary that the
      * column exactly in the middle is occupied.
      */
    val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_ROWS: Array[Mask]

    val MASKS_FOR_CONNECT_4_CHECK_IN_ROWS: Array[Array[Mask]]

    /**
      * To get four men connected vertically, it is strictly necessary that – given a specific column –
      * the fourth row (id == 3) is occupied.
      *
      * The array contains the corresponding board masks for each column.
      */
    val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_COLS: Array[Mask]

    val MASKS_FOR_CONNECT_4_CHECK_IN_COLS: Array[Array[Mask]]

    val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS: Array[Mask]

    val MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS: Array[Array[Mask]]

    val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS: Array[Mask]

    val MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS: Array[Array[Mask]]

    final lazy val ALL_MASKS: Array[(Array[Mask], Array[Array[Mask]])] = Array(
        (QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_ROWS, MASKS_FOR_CONNECT_4_CHECK_IN_ROWS),
        (QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_COLS, MASKS_FOR_CONNECT_4_CHECK_IN_COLS),
        (QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS, MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS),
        (QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS, MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS)
    )

    /**
      * The weight of each square is equal to the number of times the square appears in a line of
      * four connected men.
      *
      * These weights can, e.g., be used by the ai.
      *
      * For example, in case of a board with 6 rows and 7 columns
      * the weights are:
      *  3.. 4.. 5.. 7.. 5.. 4.. 3
      *  4.. 6.. 8..10.. 8.. 6.. 4
      *  5.. 8..11..13..11.. 8.. 5
      *  5.. 8..11..13..11.. 8.. 5
      *  4.. 6.. 8..10.. 8.. 6.. 4
      *  3.. 4.. 5.. 7.. 5.. 4.. 3
      */
    val SQUARE_WEIGHTS: Array[Int]

    val MAX_SQUARE_WEIGHT: Int

    val MIN_SQUARE_WEIGHT: Int

    final val DIFFERENT_SQUARE_WEIGHTS: Int = MAX_SQUARE_WEIGHT - MIN_SQUARE_WEIGHT + 1

    /**
      * Creates a human-readable representation of the given mask.
      *
      * Masks are used to analyze a board's state; i.e, which fields are occupied by which player.
      */
    def maskToString(mask: Mask): String = {

        /**
          * Tests if the square with given id is occupied on the given board.
          */
        def isOccupied(board: Long, squareId: Int) = (board & (1l << squareId)) != 0l

        (for (r ← (0 to MAX_ROW_INDEX).reverse) yield {
            (0 to MAX_COL_INDEX).map((c) ⇒ if (isOccupied(mask, squareId(r, c))) "1 " else "0 ").mkString
        }).mkString("\n")
    }

    /**
      * Creates a human-readable representation of all given masks.
      */
    def masksToString(masks: Array[Mask]): String = ("" /: masks)(_ + maskToString(_)+"\n")
}

