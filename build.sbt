name := "FreeMonad"

version := "1.0"

scalaVersion := "2.11.5"

val scalaTestVersion = "3.0.4"
val scalazVersion = "7.2.22"

libraryDependencies += "org.scalaz" %% "scalaz-core" % scalazVersion
libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % "test"