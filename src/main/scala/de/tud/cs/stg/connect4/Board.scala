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
  * All basic information about a specific board.
  *
  * The implementation supports boards that have at least 4 rows and 4
  * columns and that have at most 8 rows and 8 columns, but which do not have more than 56 squares; i.e.,
  * a board has at most eight rows and seven columns or seven rows and eight columns.
  *
  * @param rows The board's number of rows (4 <= ROWS <= 8).
  * @param cols The board's number of columns (4 <= COLS <= 8).
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class Board( final val rows: Int, final val cols: Int) {

    require(rows >= 4 && rows <= 8)
    require(cols >= 4 && cols <= 8)
    require(cols * rows <= 56)

    /**
      * The number of squares.
      */
    final val squares: Int = cols * rows

    /**
      * The index of the right-most column.
      */
    final val maxColIndex: Int = cols - 1

    /**
      * The index of the top-level row.
      */
    final val maxRowIndex: Int = rows - 1

    /**
      * Index of the square in the upper right-hand corner.
      */
    final val maxSquareIndex: Int = squares - 1

    /**
      * The id of the upper left-hand square.
      */
    final val upperLeftSquareIndex: Int = (rows - 1) * cols

    /**
      * Masks the square in the upper left-hand corner.
      */
    final val upperLeftSquareMask: Mask = Mask.forSquare(upperLeftSquareIndex)

    /**
      * Masks the squares in the top-level row of the board.
      *
      * This mask can, e.g., be used to efficiently check whether all squares are occupied.
      */
    final val topRowMask: Mask = (Mask.Empty /: (upperLeftSquareIndex until squares))(_ combine Mask.forSquare(_))

    /**
      * Array of the masks that mask all squares in a column. I.e., the first element of the array
      * contains a mask that masks all squares in the first column (index 0).
      */
    final val columnMasks: Array[Mask] = {
        val mask = (1l /: (1 to rows))((v, r) ⇒ (v | (1l << (r * cols))))
        (for (col ← 0 to maxColIndex) yield {
            Mask(mask << col)
        }).toArray
    }

    final val masksForConnect4CheckInRows: Array[Array[Mask]] = {
        var mask = 15l // = 1111 (BINARY); i.e., mask for the four squares in the lower left-hand corner  
        (for (r ← 0 to maxRowIndex) yield {
            val rowMasks = (for (c ← 0 to maxColIndex - 3) yield {
                val currentMask = mask
                mask = mask << 1
                Mask(currentMask)
            }).toArray
            mask = mask << 3
            rowMasks
        }).toArray
    }

    final val masksForConnect4CheckInCols: Array[Array[Mask]] = {
        val initialMask = 1l | (1l << cols) | (1l << 2 * cols) | (1l << 3 * cols)
        (for (c ← 0 to maxColIndex) yield {
            var mask = initialMask << c
            (for (r ← 0 to maxRowIndex - 3) yield {
                val currentMask = mask
                mask = mask << cols
                Mask(currentMask)
            }).toArray
        }).toArray
    }

    final val masksForConnect4CheckInLLtoURDiagonals: Array[Array[Mask]] = {

        def idOfLastSquare(squareID: Int) = { squareID + 3 * (cols + 1) }

        val startIndexes = new Array[Int]((rows - 4) + (cols - 3))
        var i = 0;
        for (l ← (cols to ((rows - 4) * cols) by cols).reverse) {
            startIndexes(i) = l
            i += 1
        }
        for (b ← 0 to cols - 4) {
            startIndexes(i) = b
            i += 1
        }

        (for (startIndex ← startIndexes) yield {
            var currentMask = (1l << startIndex | 1l << (startIndex + (cols + 1)) | 1l << (startIndex + 2 * (cols + 1)) | 1l << (startIndex + 3 * (cols + 1)))
            var currentIndex = startIndex
            var masks = List[Mask]()
            while (idOfLastSquare(currentIndex) <= maxSquareIndex
                && (row(idOfLastSquare(currentIndex)) - row(currentIndex)) == 3) {
                masks = Mask(currentMask) :: masks
                currentIndex += (cols + 1)
                currentMask = currentMask << (cols + 1)
            }
            masks.reverse.toArray
        }).toArray
    }

    final val masksForConnect4CheckInLRtoULDiagonals: Array[Array[Mask]] = {

        def idOfLastSquare(squareID: Int) = { squareID + 3 * (cols - 1) }

        val startIndexes = new Array[Int]((rows - 4) + (cols - 3))
        var i = 0;
        for (b ← 3 to cols - 1) {
            startIndexes(i) = b
            i += 1
        }
        for (l ← (cols * 2 - 1 to ((rows - 3) * cols) - 1 by cols)) {
            startIndexes(i) = l
            i += 1
        }

        (for (startIndex ← startIndexes) yield {
            var currentMask = (1l << startIndex | 1l << (startIndex + (cols - 1)) | 1l << (startIndex + 2 * (cols - 1)) | 1l << (startIndex + 3 * (cols - 1)))
            var currentIndex = startIndex
            var masks = List[Mask]()
            while (idOfLastSquare(currentIndex) <= maxSquareIndex
                && (row(idOfLastSquare(currentIndex)) - row(currentIndex)) == 3) {
                masks = Mask(currentMask) :: masks
                currentIndex += (cols - 1)
                currentMask = currentMask << (cols - 1)
            }
            masks.reverse.toArray
        }).toArray
    }

    final val masksForConnect4CheckPerOrientation: Array[Array[Array[Mask]]] = Array(
        masksForConnect4CheckInRows,
        masksForConnect4CheckInCols,
        masksForConnect4CheckInLLtoURDiagonals,
        masksForConnect4CheckInLRtoULDiagonals
    )

    final val masksForConnect4Check: Array[Mask] = masksForConnect4CheckPerOrientation.flatten.flatten

    /**
      * Array[#SQUARES] of an array of masks that need to be considered in a connect four check if a men was
      * put in a specific square.
      */
    final val masksForConnect4CheckForSquare: Array[Array[Mask]] = {
        (for (squareId ← (0 to maxSquareIndex)) yield {
            val filterMask: Mask = Mask.forSquare(squareId)
            val r = masksForConnect4Check.filter(mask ⇒ mask.areSet(filterMask)).toArray
            r
        }).toArray
    }

    /**
      * A square is essential w.r.t. a specific column, row, or one of the diagonals when it is absolutely
      * necessary to poses the respective square to be able get a line of four connected men. For example,
      * in the first column the third and fourth squares are essential. A player who does not get
      * hold of these two squares will never be able to get a line of four connected men in the first column.
      */
    final val essentialSquareMasks: Array[Array[Mask]] =
        masksForConnect4CheckPerOrientation.map(perOrientationMasks ⇒ perOrientationMasks.map(perLineMasks ⇒ (
            (Mask.selectAll(squares) /: perLineMasks)(_ intersect _)
        )))

    final val essentialSquareWeights: Array[Int] = {
        val squareWeights = new Array[Int](squares)
        for (squareId ← 0 to squares - 1) {
            var count = 0
            for (
                masks ← essentialSquareMasks;
                mask ← masks if mask.isSet(squareId)
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
    final val squareWeights: Array[Int] = {
        val squareWeights = new Array[Int](squares)

        def evalMask(mask: Mask) {
            (0 until squares).foreach((index) ⇒ {
                if (mask.isSet(index)) squareWeights(index) += 1
            })
        }

        def evalMasks(masks: Array[Array[Mask]]) { masks.foreach(_.foreach(evalMask(_))) }

        evalMasks(masksForConnect4CheckInCols)
        evalMasks(masksForConnect4CheckInRows)
        evalMasks(masksForConnect4CheckInLLtoURDiagonals)
        evalMasks(masksForConnect4CheckInLRtoULDiagonals)
        squareWeights
    }

    final val maxSquareWeight: Int = squareWeights.max

    final val minSquareWeight: Int = squareWeights.min

    /**
      * Returns the id of the square that has the given row and column indexes.
      *
      * @param row The index [0..MAX_ROW_INDEX] of the row.
      * @param col The index [0..MAX_COL_INDEX] of the column.
      */
    final def squareId(row: Int, col: Int): Int = row * cols + col

    /**
      * Returns the id ([0..rows-1]) of the row of the given square.
      *
      * @param squareId A valid square id.
      */
    final def row(squareID: Int): Int = squareID / cols

    /**
      * Returns the id ([0..cols-1]) of the column of the given square.
      *
      * @param squareId A valid square id.
      */
    final def column(squareId: Int): Int = squareId % cols

    /**
      * Returns the id of the column of the square(s) identified by the mask.
      * If squares in different columns are masked, an IllegalArgumentException is thrown.
      */
    final def column(mask: Mask): Int = {
        for (col ← 0 to maxColIndex) {
            if (mask.isSubset(columnMasks(col))) return col
        }
        throw new IllegalArgumentException(
            "the mask:\n"+maskToString(mask)+"\nidentifies squares in several columns"
        )
    }

    final def squareId(singleSquareMask: Mask): Int = {
        var upperBound = maxSquareIndex
        var lowerBound = 0
        var id = upperBound / 2;
        var value = (singleSquareMask.value >>> id)
        while (value != 1l) {
            if (value > 0) {
                lowerBound = id + 1
            }
            else {
                upperBound = id - 1
            }
            id = (upperBound + lowerBound) / 2
            value = (singleSquareMask.value >>> id)
        }
        id
    }
    
    final def moveUpByOneRow(mask : Mask) : Mask = Mask(mask.value << cols)

    /**
      * Creates a human-readable representation of the given mask.
      *
      * Masks are generally used to analyze a board's state; i.e, which fields are occupied by which player.
      */
    def maskToString(mask: Mask): String = {
        (for (r ← (0 to maxRowIndex).reverse) yield {
            (0 to maxColIndex).map((c) ⇒ if (mask.isSet(squareId(r, c))) "1 " else "0 ").mkString
        }).mkString("\n")
    }

    /**
      * Creates a human-readable representation of the given array of masks.
      */
    def masksToString(masks: Array[Mask]): String = ("" /: masks)(_ + maskToString(_)+"\n\n")
}

/**
  * The standard connect four board with six rows and seven columns.
  *
  * @author Michael Eichberg
  */
object Board6x7 extends Board(6, 7)

