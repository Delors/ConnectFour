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
  * Represents the players of a connect four game.
  *
  * @param id The id of the player. `0` for the white player and `1` for the black player.
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class Player(val id: Int) extends AnyVal {

    /**
      * `True` if this player instance represents the white player/the starting player, `false` otherwise.
      */
    def isWhite: Boolean = id == 0

    /**
      * `True` if this player instance represents the black player, `false` otherwise.
      */
    def isBlack: Boolean = id == 1

    /**
      * A single-character symbol that represents the white player ("○") or the black player ("◼").
      *
      * Intended to be used by a command-line interface or for debugging purposes. Requires the use of a fixed-
      * width font.
      */
    def symbol: String = if (id == 0) "○" else "◼"

    /**
      * Returns the name of the player ("white" or "black").
      */
    override def toString(): String = if (id == 0) "White" else "Black"
}

/**
  * Utility methods to create and get player objects.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
object Player {

    /**
      * Creates a new player instance.
      *
      * @param palyerId The id of the player. The value has to be either `0l` (white player) or `1l` (black player).
      */
    def apply(playerId: Long): Player = if (playerId == 0l) white else black

    /**
      * The white player; the player that always makes the first move.
      */
    final val white: Player = new Player(0)

    /**
      * The black player.
      */
    final val black: Player = new Player(1)
}
