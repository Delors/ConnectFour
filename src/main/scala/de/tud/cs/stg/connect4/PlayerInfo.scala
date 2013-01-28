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

private[connect4] class PlayerInfo(val playerInfo: Long) extends AnyVal {

    def isWhite(squareId: Int): Boolean = (playerInfo & (1l << squareId)) == 0l

    def isBlack(squareId: Int): Boolean = (playerInfo & (1l << squareId)) != 0l

    /**
      * Returns the player that occupies the given square. The result is only defined iff the
      * square is occupied.
      */
    def belongsTo(squareId: Int): Player = Player((playerInfo >>> squareId) & 1l)

    def areWhite(squareMask: Mask): Boolean = (playerInfo & squareMask) == 0l

    def areBlack(squareMask: Mask): Boolean = (playerInfo & squareMask) == squareMask

    /**
      * Returns the player that occupies the squares identified by the given mask.
      * If not all squares are occupied by the same player `None` is returned.
      * The result is only defined iff all identified squares are occupied.
      */
    def belongTo(squareMask: Mask): Option[Player] =
        playerInfo & squareMask match {
            case `squareMask` ⇒ Some(Player.Black)
            case 0l           ⇒ Some(Player.White)
            case _            ⇒ None
        }

    def belongToSamePlayer(squareMask: Mask): Boolean = {
        val filteredPlayerInfo = playerInfo & squareMask
        return filteredPlayerInfo == 0l || filteredPlayerInfo == squareMask
    }

    def isWhitesTurn(): Boolean = (playerInfo >>> 63) == 0l

    def isBlacksTurn(): Boolean = (playerInfo >>> 63) == 1l

    def turnOf(): Player = Player(playerInfo >>> 63)

    def update(squareMask: Mask): PlayerInfo =
        if ((playerInfo >>> 63) == 0l)
            new PlayerInfo(playerInfo | 1l << 63)
        else
            new PlayerInfo((playerInfo | squareMask) & Long.MaxValue)

}

/**
  * Factory object to create `PlayerInfo` instances.
  *
  * @author Michael Eichberg
  */
private[connect4] object PlayerInfo {

    /**
      * Creates a new player info object. The current player; i.e., the player that has to make the next
      * move is set to the white player.
      */
    def create(): PlayerInfo = new PlayerInfo(0)
}