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
  * Configuration for a board with 4 rows and 4 columns. For a board of this size, the ai can easily play
  * perfectly. There are (upper bound) at most 4^13*3*2 leaf nodes; but only a fraction will actually be
  * explored by the implementation.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmtadt.de)
  */
object Configuration4x4 extends Configuration {

    // => we will construct the complete search tree; hence, we play perfectly
    final val RECOMMENDED_AI_STRENGTH = 8

    final val ROWS = 4

    final val COLS = 4

    final val TOP_ROW_BOARD_MASK: Mask =
        (0l /: (UPPER_LEFT_SQUARE_INDEX until SQUARES))(_ | 1l << _)

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_COLS: Array[Long] =
        ((4 to 7) map (1l << _)).toArray

    final val MASKS_FOR_CONNECT_4_CHECK_IN_COLS: Array[Array[Long]] = {
        val initialMask = 1l | (1l << 4) | (1l << 8) | (1l << 12)
        (for (c ← 0 to MAX_COL_INDEX) yield {
            var mask = initialMask << c
            (for (r ← 0 to MAX_ROW_INDEX - 3) yield {
                val currentMask = mask
                mask = mask << 4
                currentMask
            }).toArray
        }).toArray
    }

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_ROWS: Array[Long] =
        ((3 to 15 by 4) map { 1l << _ }).toArray

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

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS = Array(1l)

    final val MASKS_FOR_CONNECT_4_CHECK_IN_LL_TO_UR_DIAGONALS: Array[Array[Mask]] =
        Array(Array(1l | 1l << 5 | 1l << 10 | 1l << 15))

    final val QUICK_CHECK_MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS =
        Array(1l << 12)

    final val MASKS_FOR_CONNECT_4_CHECK_IN_LR_TO_UL_DIAGONALS: Array[Array[Mask]] =
        Array(Array(1l << 3 | 1l << 6 | 1l << 9 | 1l << 12))

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

    final val MAX_SQUARE_WEIGHT = SQUARE_WEIGHTS.max

    final val MIN_SQUARE_WEIGHT = SQUARE_WEIGHTS.min

}

