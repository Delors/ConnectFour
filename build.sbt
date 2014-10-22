name := "Connect4"

version := "0.9.9"

scalaVersion := "2.10.2"

scalacOptions in (Compile,compile) += "–deprecation" 

scalacOptions in (Compile,compile) += "–target:jvm-1.7" 
 
scalacOptions in Compile += "-feature" 

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
