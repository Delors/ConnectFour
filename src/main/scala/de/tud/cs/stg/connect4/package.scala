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

import scala.language.implicitConversions

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
  * To make the code more comprehensible value classes are used to indicate in which way a particular long
  * value is used.
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

    implicit def connectFourToBoard(connectFourGame: ConnectFourGame): Board = connectFourGame.board

    /**
      * Specifies that the respective long value is a bit mask to get information about which squares
      * are occupied and – if so – by which player. The range of valid values depends on the concrete size of
      * the board. However, it is always a value between 1 (mask for the square in the lower left-hand corner)
      * and 2^56 in case of a board with seven(eight) rows and eight(seven) columns.
      */
    type Mask = Long
}
