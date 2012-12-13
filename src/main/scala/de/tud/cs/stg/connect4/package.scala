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
package de.tud.cs.stg

/**
  * Implementation of "Connect Four" (cf. [[http://en.wikipedia.org/wiki/Connect_Four Wikipedia: Connect Four]]) using
  * the minimax algorithm as the foundation for the implementation of the artificial intelligence.
  *
  * ==Relevant Classes==
  * The game logic is implemented by the class [[de.tud.cs.stg.connect4.ConnectFour]] which is independent of
  * any specific user interface.
  *
  * A basic command-line interface is implemented by the class [[de.tud.cs.stg.connect4.ui.CLI]].
  *
  * ==Implementation==
  * In general, care was taken to make sure that the implementation is reasonable efficient w.r.t. the
  * memory usage as well as the overall runtime performance. To that end, the complete game state is encoded
  * using two long values. The encoding was done such that is efficient to check for lines of four connected
  * men as well as to update the game state.
  *
  * To make the code more comprehensible two aliases (State and Mask) for the primitive type long are defined to 
  * indicate in which way a particular long value is expected to be used.
  *
  * ==License==
  * BSD Style License
  *
  * Copyright (c) 2012, 2013
  *
  * Software Technology Group; Department of Computer Science; Technische Universität Darmstadt
  *
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
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
package object connect4 {

    implicit def connectFourToConfiguration(connectFour: ConnectFour): Configuration = connectFour.configuration

    /**
      * Type used to indicate the state of the game. Three states are distinguished:
      *  1. The game is not finished ([[de.tud.cs.stg.connect4.NOT_FINISHED]]).
      *  1. The game is drawn ([[de.tud.cs.stg.connect4.DRAWN]]).
      *  1. Some player has won the game.
      * The value associated with the first state is the long value -1; the long value associated with the
      * second state (DRAWN) is 0l. The third case uses values in the range: [15...2^(7x7)] to (a) identify
      * that some player has won and (b) to simultaneously specify the mask that can be used to identify the
      * line of four connect men on the board.
      */
    type State = Long

    /**
      * Indicates that the game is not finished. I.e., no player has won and some squares are still empty.
      */
    final val NOT_FINISHED: State = -1l

    /**
      * Indicates that the game is drawn. I.e., all squares are occupied and no player has won.
      */
    final val DRAWN: State = 0l

    /**
      * Type used to specify that the respective long value is a bit mask to get information about which squares
      * are occupied and – if so – by which player. The range of valid values depends on the concrete size of
      * the board. However, it is always a value between 1 (mask for the square in the lower left-hand corner)
      * and 2^49 in case of a board with seven rows and seven columns.
      */
    type Mask = Long

}