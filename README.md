Connect Four
============

Implementation of "Connect Four" that uses the minimax algorithm as the foundation for the implementation of the artificial intelligence.

To build the game you can use the Simple Build Tool (sbt). In this case it is sufficient to call sbt compile in the project's root directory.

To play a game call ''sbt run''.


Implemented Features
=================
 - negamax algorithm with alpha-beta-pruning
 - simple scoring function which scores a board based on the number of lines of three connected men
 - effective move ordering algorithm
 - caching of intermediate board configurations while exploring the search 
 - flexible board size (between 4x4 and 7x8/8x7)
 - output of the search tree as a Dot graph
 - the ai can play as the white or black player
 - configurable search depth 
 
 
Notes
=====
The following types of connect four games are decided:
Rows x Cols 
4 x 4 => drawn (if both players play perfectly, no player will be able to win)
4 x 5 => drawn
4 x 6 => the second player will win, if the second player plays perfectly
4 x 7 => drawn
5 x 5 => drawn
6 x 4 => drawn 
6 x 6 => the second player will win, if the second player plays perfectly

 
 
 