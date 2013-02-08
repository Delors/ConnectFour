ConnectFour
===========

Implementation of "Connect Four" that uses the minimax algorithm as the foundation for the implementation of the artificial intelligence.

To build the game you can use the Simple Build Tool (sbt). In this case it is sufficient to call sbt compile in the project's root directory.

To play a game call ''sbt run''.


Implemented Features
=================
 - negamax algorithm with alpha-beta-pruning
 - scoring function which scores a board based on the number of lines of three connected men
 - flexible board size (between 4x4 and 7x8/8x7)
 - output of the search tree as a Dot graph
 - the ai can play as the white or black player
 - configurable search depth 
 