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

/**
  * Information about a specific board. The implementation supports boards that have at least 4 rows and 4
  * columns and that have at most 8 rows and 8 columns, but does not have more than 56 squares.
  *
  * @param ROWS The board's number of rows (4 <= ROWS <= 8).
  * @param COLS The board's number of columns (4 <= COLS <= 8).
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class Board(val ROWS: Int, val COLS: Int) {

    require(ROWS >= 4 && ROWS <= 8)
    require(COLS >= 4 && COLS <= 8)
    require(COLS * ROWS <= 56)

    /**
      * The number of squares.
      */
    final val SQUARES = COLS * ROWS

    final val MAX_COL_INDEX = COLS - 1

    final val MAX_ROW_INDEX = ROWS - 1

    final val MAX_SQUARE_INDEX = SQUARES - 1

    /**
      * The id of the square in the upper left-hand corner.
      */
    final val UPPER_LEFT_SQUARE_INDEX = (ROWS - 1) * COLS

    /**
      * Masks the squares in the top-level row of the board.
      *
      * This mask can, e.g., be used to efficiently check whether all squares are occupied.
      */
    final val TOP_ROW_BOARD_MASK: Long = (0l /: (UPPER_LEFT_SQUARE_INDEX until SQUARES))(_ | 1l << _)

    /**
      * Array of all masks that mask all squares in a column.
      */
    final val columnMasks: Array[Mask] = {
        val mask = (1l /: (1 to ROWS))((v, r) ⇒ (v | (1l << (r * COLS))))
        (for (col ← 0 to MAX_COL_INDEX) yield {
            mask << col
        }).toArray
    }

    final val MASKS_FOR_CONNECT_4_CHECK_IN_ROWS: Array[Array[Long]] = {
        var mask = 15l // = 1111 (BINARY); i.e., mask for the four squares in the lower left-hand corner  
        (for (r ← 0 to MAX_ROW_INDEX) yield {
            val rowMasks = (for (c ← 0 to MAX_COL_INDEX - 3) yield {
                val currentMask = mask
                mask = mask << 1
                currentMask
            }).toArray
            mask = mask << 3
            rowMasks
        }).toArray
    }

    final val MASKS_FOR_CONNECT_4_CHECK_IN_COLS: Array[Array[Long]] = {
        val initialMask = 1l | (1l << COLS) | (1l << 2 * COLS) | (1l << 3 * COLS)
        (for (c ← 0 to MAX_COL_INDEX) yield {
            var mask = initialMask << c
            (for (r ← 0 to MAX_ROW_INDEX - 3) yield {
                val currentMask = mask
                mask = mask << COLS
                currentMask
            }).toArray
        }).toArray
    }

    final val MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS: Array[Array[Long]] = {

        def idOfLastSquare(squareID: Int) = { squareID + 3 * (COLS + 1) }

        val startIndexes = new Array[Int]((ROWS - 4) + (COLS - 3))
        var i = 0;
        for (l ← (COLS to ((ROWS - 4) * COLS) by COLS).reverse) {
            startIndexes(i) = l
            i += 1
        }
        for (b ← 0 to COLS - 4) {
            startIndexes(i) = b
            i += 1
        }

        (for (startIndex ← startIndexes) yield {
            var mask = (1l << startIndex | 1l << (startIndex + (COLS + 1)) | 1l << (startIndex + 2 * (COLS + 1)) | 1l << (startIndex + 3 * (COLS + 1)))
            var currentIndex = startIndex
            var bitFields = List[Long]()
            while (idOfLastSquare(currentIndex) <= MAX_SQUARE_INDEX
                && (row(idOfLastSquare(currentIndex)) - row(currentIndex)) == 3) {
                bitFields = mask :: bitFields
                currentIndex += (COLS + 1)
                mask = mask << (COLS + 1)
            }
            bitFields.reverse.toArray
        }).toArray
    }

    final val MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS: Array[Array[Long]] = {

        def idOfLastSquare(squareID: Int) = { squareID + 3 * (COLS - 1) }

        val startIndexes = new Array[Int]((ROWS - 4) + (COLS - 3))
        var i = 0;
        for (b ← 3 to COLS - 1) {
            startIndexes(i) = b
            i += 1
        }
        for (l ← (COLS * 2 - 1 to ((ROWS - 3) * COLS) - 1 by COLS)) {
            startIndexes(i) = l
            i += 1
        }

        (for (startIndex ← startIndexes) yield {
            var mask = (1l << startIndex | 1l << (startIndex + (COLS - 1)) | 1l << (startIndex + 2 * (COLS - 1)) | 1l << (startIndex + 3 * (COLS - 1)))
            var currentIndex = startIndex
            var masks = List[Long]()
            while (idOfLastSquare(currentIndex) <= MAX_SQUARE_INDEX
                && (row(idOfLastSquare(currentIndex)) - row(currentIndex)) == 3) {
                masks = mask :: masks
                currentIndex += (COLS - 1)
                mask = mask << (COLS - 1)
            }
            masks.reverse.toArray
        }).toArray
    }

    final val ALL_MASKS_FOR_CONNECT4_CHECK: Array[Array[Array[Mask]]] = Array(
        MASKS_FOR_CONNECT_4_CHECK_IN_ROWS,
        MASKS_FOR_CONNECT_4_CHECK_IN_COLS,
        MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS,
        MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS
    )

    final val FLAT_ALL_MASKS_FOR_CONNECT4_CHECK: Array[Mask] = ALL_MASKS_FOR_CONNECT4_CHECK.flatten.flatten
    //    {
    //        val arrayBuffer = new ArrayBuffer[Long]()
    //        arrayBuffer ++= (MASKS_FOR_CONNECT_4_CHECK_IN_ROWS.flatten)
    //        arrayBuffer ++= (MASKS_FOR_CONNECT_4_CHECK_IN_COLS.flatten)
    //        arrayBuffer ++= (MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS.flatten)
    //        arrayBuffer ++= (MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS.flatten)
    //        arrayBuffer.toArray
    //    }

    /**
      * A square is essential w.r.t. a specific column, row, or one of the diagonals when it is absolutely
      * necessary to poses the respective square to be able get a line of four connected men. For example,
      * in the first column the third and fourth squares are essential. A player who does not get
      * hold of these two squares will never be able to get a line of four connected men in the first column.
      */
    final val ALL_ESSENTIAL_SQUARES_MASKS: Array[Array[Mask]] =
        ALL_MASKS_FOR_CONNECT4_CHECK.map(perOrientationMasks ⇒ perOrientationMasks.map(perLineMasks ⇒ (
            (Long.MaxValue /: perLineMasks)(_ & _)
        )))

    final val ESSENTIAL_SQUARE_WEIGHTS: Array[Int] = {
        val squareWeights = new Array[Int](SQUARES)
        for (squareId ← 0 to SQUARES - 1) {
            var count = 0
            for (
                masks ← ALL_ESSENTIAL_SQUARES_MASKS;
                mask ← masks if (mask & (1l << squareId)) != 0l
            ) {
                count += 1
            }
            squareWeights(squareId) = count
        }
        squareWeights
    }

    /**
      * The weight of each square is equal to the number of times the square appears in a line of
      * four connected men.
      *
      * These weights can, e.g., be used by the ai to score the current game state.
      *
      * For example, in case of a board with 6 rows and 7 columns the weights are:
      *  3.. 4.. 5.. 7.. 5.. 4.. 3
      *  4.. 6.. 8..10.. 8.. 6.. 4
      *  5.. 8..11..13..11.. 8.. 5
      *  5.. 8..11..13..11.. 8.. 5
      *  4.. 6.. 8..10.. 8.. 6.. 4
      *  3.. 4.. 5.. 7.. 5.. 4.. 3
      */
    final val SQUARE_WEIGHTS: Array[Int] = {
        val squareWeights = new Array[Int](SQUARES)

        def evalBoardMask(boardMask: Long) {
            (0 until SQUARES).foreach((index) ⇒ {
                if ((boardMask & (1l << index)) != 0l) squareWeights(index) += 1
            })
        }

        def evalBoardMasks(boardMasks: Array[Array[Long]]) {
            boardMasks.foreach(_.foreach(evalBoardMask(_)))
        }

        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_COLS)
        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_ROWS)
        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS)
        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS)
        squareWeights
    }

    final val maxSquareWeight: Int = SQUARE_WEIGHTS.max

    final val minSquareWeight: Int = SQUARE_WEIGHTS.min

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
      * Returns the id of the column of the square(s) identified by the mask.
      */
    final def column(mask: Mask): Int = {
        for (col ← 0 to MAX_COL_INDEX) {
            if ((mask & columnMasks(col)) == mask) return col
        }
        throw new IllegalArgumentException("the mask:\n"+maskToString(mask)+"\ndoes not identify squares in a unique column")
    }

    /**
      * Creates a human-readable representation of the given mask.
      *
      * Masks are used to analyze a board's state; i.e, which fields are occupied by which player.
      */
    def maskToString(mask: Mask): String = {
        // Tests if the square with given id is set in the given mask.
        def isSet(mask: Long, squareId: Int) = (mask & (1l << squareId)) != 0l

        (for (r ← (0 to MAX_ROW_INDEX).reverse) yield {
            (0 to MAX_COL_INDEX).map((c) ⇒ if (isSet(mask, squareId(r, c))) "1 " else "0 ").mkString
        }).mkString("\n")
    }

    /**
      * Creates a human-readable representation of all given masks.
      */
    def masksToString(masks: Array[Mask]): String = ("" /: masks)(_ + maskToString(_)+"\n\n")
}

/**
  * The standard connect four board with six rows and seven columns.
  *
  * @author Michael Eichberg
  */
object Board6x7 extends Board(6, 7)

