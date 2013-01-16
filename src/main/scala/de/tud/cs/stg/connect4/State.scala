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
  * Indicates the state of the game.
  *
  * Basically, three states are distinguished:
  *  1. The game is not finished ([[de.tud.cs.stg.connect4.State.NotFinished]]).
  *  1. The game is drawn ([[de.tud.cs.stg.connect4.State.Drawn]]).
  *  1. Some player has won the game.
  * The value associated with the first state (`NotFinished`) is the long value -1l; the long value associated
  * with the second state (`Drawn`) is 0l. The third case is a mask to (a)
  * identify that some player has won and (b) to simultaneously specify the line of four connect men on the
  * board.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
class State private (val state: Long) extends AnyVal {

    /**
      * `true` if some squares are still empty and no player has won sofar.
      */
    def isNotFinished: Boolean = state == -1l

    /**
      * `true` if the game is drawn; i.e., all squares are occupied but no player was able to get a
      * line of four connected men.
      */
    def isDrawn: Boolean = state == 0l

    /**
      * `true` if the game has ended because some player has won or the game is drawn.
      */
    def isFinished: Boolean = state >= 0l

    /**
      * `true` if the game has ended, because some player has won.
      */
    def hasWinner: Boolean = state > 0l

    /**
      * Returns the mask that identifies the line of four connected men of the winning player.
      *
      * The return value is only defined iff this game has a winner.
      */
    def getMask: Mask = state
}
/**
  * Utility methods related to creating state objects.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
object State {

    /**
      * Creates a state object that identifies a game that has ended, because a player has won the game.
      *
      * @param mask The mask that identifies the line of four connected men of the wining player. The value
      *     has to be – if SQ is the number of squares of the board – between 15 (1l|1l<<1|1l<<2|1l<<3) and
      *     1l<<(SQ-4)|1l<<(SQ-3)|1l<<(SQ-2)|1l<<(SQ-1). ''This requirement is not checked at runtime.''
      */
    def apply(mask: Mask): State = new State(mask)

    /**
      * The game is not finished. I.e., no player has won and some squares are still empty.
      */
    final val NotFinished: State = new State(-1l)

    /**
      * The game is drawn. I.e., all squares are occupied and no player has won.
      */
    final val Drawn: State = new State(0l)
}