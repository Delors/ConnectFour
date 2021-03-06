Connect Four
============

Implementation of "Connect Four" that uses the minimax algorithm as the foundation for the implementation of the artificial intelligence.

To build the game you can use the Simple Build Tool (sbt). In this case it is sufficient to call `sbt compile` in the project's root directory.

To play a game call `sbt run`.


Implemented Features
=================
 - negamax algorithm with alpha-beta-pruning
 - effective move ordering algorithm
 - caching of intermediate game states to speed up exploring the search tree
 - simple scoring function which scores a board based on the number of lines of three connected men that could be completed to a line of four men
 - flexible board size (between 4x4 and 7x8/8x7)
 - output of the search tree as a Dot graph
 - the ai can play as the white or black player
 - configurable search depth 
 
 
Notes
=====
The following types of connect four games are decided:

|Rows | Cols | Result, when both players play perfectly |
|----|------|----|
|4 | 4 | drawn
|4 | 5 | drawn
|4 | 6 | the second player will win
|4 | 7 | drawn
|5 | 4 | drawn
|5 | 5 | drawn
|5 | 6 | drawn
|6 | 4 | drawn 
|6 | 5 | drawn 
|6 | 6 | the second player will win

 
 
 
