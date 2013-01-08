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
  * Configuration for a board with 6 rows and 7 columns (the standard board size).
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmtadt.de)
  */
object Board6x7 extends Board {

    final val RECOMMENDED_AI_STRENGTH = 5

    final val ROWS = 6

    final val COLS = 7

    final val TOP_ROW_BOARD_MASK: Long =
        (0l /: (UPPER_LEFT_SQUARE_INDEX until SQUARES))(_ | 1l << _)

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_COLS: Array[Long] =
        ((21 to 27) map (1l << _)).toArray

    final val MASKS_FOR_CONNECT_4_CHECK_IN_COLS: Array[Array[Long]] = {
        val initialMask = 1l | (1l << 7) | (1l << 14) | (1l << 21)
        (for (c ← 0 to MAX_COL_INDEX) yield {
            var mask = initialMask << c
            (for (r ← 0 to MAX_ROW_INDEX - 3) yield {
                val currentMask = mask
                mask = mask << 7
                currentMask
            }).toArray
        }).toArray
    }

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_ROWS: Array[Long] =
        ((3 to 38 by 7) map { 1l << _ }).toArray

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

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS =
        Array(1l << 38, 1l << 31, 1l << 24, 1l << 25, 1l << 26, 1l << 27)

    private final val START_INDEXES_FOR_CONNECT_4_CHECKS_IN_LL_TO_UR_DIAGONALS = Array[Int](14, 7, 0, 1, 2, 3)
    final val MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS: Array[Array[Long]] = {

        def idOfLastSquare(squareID: Int) = { squareID + 3 * 8 }

        (for (startIndex ← START_INDEXES_FOR_CONNECT_4_CHECKS_IN_LL_TO_UR_DIAGONALS) yield {
            var bitField = (1l << startIndex | 1l << (startIndex + 8) | 1l << (startIndex + 2 * 8) | 1l << (startIndex + 3 * 8))
            var currentIndex = startIndex
            var bitFields = List[Long]()
            while (idOfLastSquare(currentIndex) <= MAX_SQUARE_INDEX
                && (row(idOfLastSquare(currentIndex)) - row(currentIndex)) == 3) {
                bitFields = bitField :: bitFields
                currentIndex += 8
                bitField = bitField << 8
            }
            bitFields.reverse.toArray
        }).toArray
    }

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS =
        Array(1l << 21, 1l << 22, 1l << 23, 1l << 24, 1l << 31, 1l << 38)

    private final val START_INDEXES_FOR_CONNECT_4_CHECKS_IN_LR_TO_UL_DIAGONALS = Array[Int](3, 4, 5, 6, 13, 20)
    final val MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS: Array[Array[Long]] = {

        def idOfLastSquare(squareID: Int) = { squareID + 3 * 6 }

        (for (startIndex ← START_INDEXES_FOR_CONNECT_4_CHECKS_IN_LR_TO_UL_DIAGONALS) yield {
            var bitField = (1l << startIndex | 1l << (startIndex + 6) | 1l << (startIndex + 2 * 6) | 1l << (startIndex + 3 * 6))
            var currentIndex = startIndex
            var bitFields = List[Long]()
            while (idOfLastSquare(currentIndex) <= MAX_SQUARE_INDEX
                && (row(idOfLastSquare(currentIndex)) - row(currentIndex)) == 3) {
                bitFields = bitField :: bitFields
                currentIndex += 6
                bitField = bitField << 6
            }
            bitFields.reverse.toArray
        }).toArray
    }

    final val SQUARE_WEIGHTS: Array[Int] = {
        val squareWeights = new Array[Int](42)

        def evalMask(mask: Mask) {
            (0 until 42).foreach((index) ⇒ {
                if ((mask & (1l << index)) != 0l) squareWeights(index) += 1
            })
        }

        def evalBoardMasks(masks: Array[Array[Long]]) {
            masks.foreach(_.foreach(evalMask(_)))
        }

        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_COLS)
        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_ROWS)
        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS)
        evalBoardMasks(MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS)
        squareWeights
    }

    final val MAX_SQUARE_WEIGHT = 13

    final val MIN_SQUARE_WEIGHT = 3

}
