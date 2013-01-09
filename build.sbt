name := "Connect4"

version := "0.6"

scalaVersion := "2.10.0"

scalacOptions in (Compile,compile) += "–deprecation" 

scalacOptions in (Compile,compile) += "–target:jvm-1.7" 
 
scalacOptions in Compile += "-feature" 

libraryDependencies += "junit" % "junit" % "4.8.1" % "test"

libraryDependencies += "org.scalatest" % "scalatest_2.10.0-RC5" % "1.8-B1" % "test"