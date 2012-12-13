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
package utils

/**
  * Identifies all final, valid board configurations. That is, all those board configurations where either one player
  * has won or which are a draw and which could be the result of a real game are identified.
  *
  * This class is primarily used to evaluate the performance of the implementation; i.e., it evaluates how
  * many boards configurations can be evaluated in a given time frame and how caching affects the exploration
  * of the search tree. This number is important to identify the maximum depth of
  * the search tree that can be explored when using the minimax algorithm to implement the ai.
  *
  * @author Michael Eichberg
  */
object AnalyzeConnect4 extends scala.App {

    val connectFour = new ConnectFour(Configuration6x7)
    import connectFour._

    /**
      * Returns the set of all final board configurations.
      */
    def allFinalConfigurations: scala.collection.Set[Game] = {
        val startTime = System.currentTimeMillis()
        var lastTime = startTime

        // the list of all final board configurations
        var finalConfigurations: scala.collection.mutable.Set[Game] = scala.collection.mutable.Set()

        // _some_ game states are memoized to speed up the search for all board configurations
        var memoizedGameStates: scala.collection.mutable.Set[Game] = scala.collection.mutable.Set()

        // some variables to collect some statistics
        var gameStatesCreated: Long = 1l // 1 for the initial one
        var filteredGameStates: Long = 0l // a game state is filtered if it was encountered before

        var gameStates = List(new Game) // start with the empty board

        def printStatistics = {
            if (System.currentTimeMillis() - lastTime > 15000) {
                println("Number of created game states (in million): "+(gameStatesCreated / 1000000)+" in "+(System.currentTimeMillis() - startTime) / 1000+" secs.")
                println("Number of final configurations: "+finalConfigurations.size+" final positions (boards in queue: "+gameStates.size+")")
                println("Number of filtered game states: "+filteredGameStates)
                println("Number of memoized game states: "+memoizedGameStates.size)
                println
                lastTime = System.currentTimeMillis()
            }
        }

        while (!gameStates.isEmpty) { // while we have not fully exploited all paths...
            val gameState = gameStates.head
            gameStates = gameStates.tail

            for (legalMove ← gameState.legalMoves) {
                val newGameState = gameState.makeMove(legalMove)

                gameStatesCreated += 1l
                if ((gameStatesCreated % 1000000l) == 0l) {
                    printStatistics
                }

                if (newGameState.state == NOT_FINISHED) {
                    if (!memoizedGameStates.contains(newGameState)) {
                        gameStates = newGameState :: gameStates
                        if (memoizedGameStates.size < 65000000 &&
                            (gameStates.size < 95 || (gameStates.size < 125 && (gameStatesCreated % 49) == 0))) {
                            memoizedGameStates += newGameState
                        }
                    }
                    else {
                        filteredGameStates += 1l
                    }
                }
                else {
                    finalConfigurations += newGameState
                }
            }
        }
        printStatistics
        finalConfigurations
    }

    println("Determining all final configurations.")
    allFinalConfigurations
    println("All final configurations identified.")
    for (position ← allFinalConfigurations) {
        println(position)
    }
}