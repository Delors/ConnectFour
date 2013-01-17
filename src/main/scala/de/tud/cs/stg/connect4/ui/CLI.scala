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
package ui

/**
  * Implementation of a lightweight command-line interface to play connect four against the computer.
  *
  * The command-line interface enables the human player to let the computer make a proposal, to let the
  * computer make the move on behalf of the player or to let the player make the move on her/his own.
  *
  * The interface requires that a font with a fixed-width is used (i.e., a monospaced font); otherwise the
  * board may be rendered in a way that hinders comprehension by a human player.
  *
  * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
  */
object CLI extends scala.App {

    import java.lang.System.in

    println("Welcome to connect four 2013!")

    print("Output the search tree (g) or debug info (d)(Default: <None>)?"); val o = in.read(); println
    print("Number of rows [4..8](Default: 6; rows*columns <= 56)?"); val r = in.read(); println
    print("Number of columns [4..8](Default: 7; rows*columns <= 56)?"); val c = in.read(); println
    val rows: Int = if (r >= '4' && r <= '8') r - '0' else 6
    val cols: Int = if (c >= '4' && c <= '8') c - '0' else 7
    val board = new Board(rows, cols)
    val setup = new Setup(
        o match {
            case 'g' ⇒ new ConnectFour(board, false, true)
            case 'd' ⇒ new ConnectFour(board, true, false)
            case _   ⇒ new ConnectFour(board, false, false)
        }
    )

    do { // main loop
        setup.playGame()
    } while ({ print("Do you want to play a game (Default: y)?"); val c = in.read(); println; c == 'y' })

    println("Good Bye!")
}