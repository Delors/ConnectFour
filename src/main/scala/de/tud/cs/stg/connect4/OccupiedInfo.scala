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
  * Stores the information which squares of a connect four game are occupied.
  *
  * The information by which
  * player an occupied field is actually occupied is stored in a [[de.tud.cs.stg.connect4.PlayerInfo]]
  * object.
  *
  * @param board Encodes the information which squares are occupied.
  * @author Michael Eichberg
  */
class OccupiedInfo private (val board: Long) extends AnyVal {

    /**
      * True if the board is completely empty.
      */
    def allSquaresEmpty(): Boolean = board == 0l

    /**
      * True, if the square with the given id is empty.
      */
    def isEmpty(squareId: Int): Boolean = (board & (1l << squareId)) == 0l

    /**
      * True, if the square with the given id is occupied by a player.
      */
    def isOccupied(squareId: Int): Boolean = (board & (1l << squareId)) != 0l

    /**
      * Tests if all squares that are identified by the given mask are empty.
      */
    def areEmpty(squareMask: Mask): Boolean = (board & squareMask.value) == 0l

    /**
      * Tests if all squares that are identified by the given mask are occupied.
      */
    def areOccupied(squareMask: Mask): Boolean = (board & squareMask.value) == squareMask.value

    /**
      * Updates this mask and returns a new mask where the given squares are also set.
      */
    def update(squareMask: Mask): OccupiedInfo = new OccupiedInfo(board | squareMask.value)

    /**
      * Returns the occupied squares of this mask that are also selected by the given mask.
      */
    def filter(squareMask: Mask): OccupiedInfo = new OccupiedInfo(board & squareMask.value)

    /**
      * Returns a mask that identifies the squares where a man was placed when compared with this
      * maks (the base mask).
      *
      * ==Prerequisite==
      * The result is only defined if the given mask is a successor mask of this mask.
      */
    def lastMoves(occupiedInfo: OccupiedInfo): Mask = Mask(occupiedInfo.board ^ this.board)
}

/**
  * Factory object to create `OccupiedInfo` objects.
  *
  * @author Michael Eichberg
  */
object OccupiedInfo {

    /**
      * Creates a new `OccupiedInfo` object where all squares are empty.
      */
    def create(): OccupiedInfo = new OccupiedInfo(0l)
}
