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
  * Indicates the state of the game. Basically, three states are distinguished:
  *  1. The game is not finished ([[de.tud.cs.stg.connect4.State.notFinished]]).
  *  1. The game is drawn ([[de.tud.cs.stg.connect4.State.drawn]]).
  *  1. Some player has won the game.
  * The value associated with the first state (`notFinished`) is the long value -1l; the long value associated
  * with the second state (`drawn`) is 0l. The third case uses values in the range: [15...2^(7x7)] to (a)
  * identify that some player has won and (b) to simultaneously specify the mask that can be used to identify
  * the line of four connect men on the board.
  */
class State(val state: Long) extends AnyVal {

    def isNotFinished: Boolean = state == -1l

    def isDrawn: Boolean = state == 0l

    def isFinished: Boolean = state >= 0l

    def hasWinner: Boolean = state > 0l

    def getMask: Long = state
}

object State {

    /**
      * @param mask The mask that identifies the line of four connected men of the wining player.
      */
    def apply(mask: Mask): State = new State(mask)

    /**
      * Indicates that the game is not finished. I.e., no player has won and some squares are still empty.
      */
    final val notFinished: State = new State(-1l)

    /**
      * Indicates that the game is drawn. I.e., all squares are occupied and no player has won.
      */
    final val drawn: State = new State(0l)
}