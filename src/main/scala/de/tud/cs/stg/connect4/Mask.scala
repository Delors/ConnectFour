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
  * A mask (basically a long value) that selects specific squares on a board.
  *
  * The respective long value is a bit mask to get information about which squares
  * are occupied (in combination with [[de.tud.cs.stg.connect4.OccupiedInfo]]) and by which
  * player (in combination with [[de.tud.cs.stg.connect4.PlayerInfo]]. The range of valid values depends on
  * the concrete size of the board.
  *
  * However, it is always a value between 1 (mask for the square in the lower left-hand corner)
  * and 2^56 in case of a board with seven(eight) rows and eight(seven) columns.
  *
  * @param mask A mask that identifies one or multiple squares on a specific board.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class Mask private (val value: Long) extends AnyVal {

    /**
      * Combination of this mask and the given mask. The returned mask selects all squares identified by
      * this mask and the other mask.
      */
    def combine(other: Mask): Mask = new Mask(this.value | other.value)

    /**
      * Intersection of this mask and the given mask. The returned mask selects those squares that are
      * identified by this mask and also by the other mask.
      */
    def intersect(other: Mask): Mask = new Mask(this.value & other.value)

    /**
      * Tests if the square is set in this mask.
      */
    def isSet(squareId: Int): Boolean = (value & (1l << squareId)) != 0l

    /**
      * True if this mask does not represent a legal square mask.
      */
    def isIllegal = this.value == Mask.Illegal.value

    /**
      * True if this mask represents a legal square mask.
      */
    def isLegal = this.value != Mask.Illegal.value

    /**
      * True if this mask selects no square.
      */
    def isEmpty = this.value == 0l

    /**
      * Tests is all squares identified by the other mask are set for this mask.
      */
    def areSet(other: Mask): Boolean = (this.value & other.value) == other.value

    /**
      * Tests if the squares identified by this mask are a strict subset of the
      * squares identified by the other mask.
      */
    def isSubset(other: Mask): Boolean = (this.value & other.value) == this.value
}

/**
  * Factory object to create specific masks.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
object Mask {

    /**
      * Creates a new mask.
      */
    private[connect4] def apply(mask: Long): Mask = new Mask(mask)

    /**
      * Returns one mask that selects all the squares with the given ids.
      *
      * ==Note==
      * This method is intended to be used by the tests only as it is not optimized.
      */
    def forSquares(squareIds: Int*): Mask = (Empty /: squareIds.map(Mask.forSquare(_)))(_ combine _)

    /**
      * Returns a mask that selects the square with the given id.
      */
    def forSquare(squareId: Int): Mask = new Mask(1l << squareId)

    /**
      * Returns a mask that selects the lowest square in the column.
      */
    def lowestSquareInColumn(column: Int): Mask = new Mask(1l << column)

    /**
      * A mask that selects no square.
      */
    val Empty = new Mask(0l)

    /**
      * A mask that is not legal and hence can be distinguished from all legal masks.
      */
    val Illegal = new Mask(-1l)

    /**
      * A mask that selects all squares on the current board.
      */
    def selectAll(squares: Int): Mask = new Mask((1l << squares) - 1)
}
