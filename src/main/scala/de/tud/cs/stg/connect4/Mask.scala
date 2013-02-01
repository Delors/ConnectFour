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
  * A mask (a long value) that selects specific squares on a board.
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

    def combine(other: Mask): Mask = new Mask(this.value | other.value)
    
    def intersect(other : Mask) : Mask = new Mask(this.value & other.value)

    def isSet(squareId: Int): Boolean = (value & (1l << squareId)) != 0l

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
  * Factory to create specific masks.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
object Mask {

    def apply(mask: Long): Mask = new Mask(mask)
    
    def forSquares(squareIds : Int*) : Mask = (Empty /: squareIds.map(Mask.forSquare(_)))(_ combine _)

    def forSquare(squareId: Int): Mask = new Mask(1l << squareId)

    val Empty = new Mask(0l)

    val Illegal = new Mask(-1l)
    
    def selectAll(squares : Int) : Mask = new Mask((1 << squares)-1)
}
