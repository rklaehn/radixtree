import ReleaseTransformations._

import sbtcrossproject.CrossPlugin.autoImport.crossProject

lazy val commonSettings = Seq(
  organization := "com.rklaehn",
  scalaVersion := "2.12.10",
  crossScalaVersions := Seq("2.12.10", "2.13.1"),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.typelevel" %%% "cats-core" % "2.0.0",
    "org.typelevel" %%% "cats-laws" % "2.0.0" % Test,
    "org.typelevel" %%% "discipline-scalatest" % "1.0.0-M1" % Test,
    "org.scalatest" %%% "scalatest" % "3.0.8" % Test
  ),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature"
  ),
  licenses += ("Apache License, Version 2.0", url(
    "http://www.apache.org/licenses/LICENSE-2.0.txt"
  )),
  homepage := Some(url("http://github.com/rklaehn/radixtree"))
)

lazy val noPublish =
  Seq(publish := {}, publishLocal := {}, publishArtifact := false)

lazy val root = project
  .in(file("."))
  .aggregate(coreJVM, coreJS, instrumentedTest)
  .settings(name := "root")
  .settings(commonSettings: _*)
  .settings(noPublish: _*)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(name := "radixtree")
  .settings(commonSettings: _*)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val instrumentedTest = project
  .in(file("instrumentedTest"))
  .settings(name := "instrumentedTest")
  .settings(commonSettings: _*)
  .dependsOn(coreJVM)
  .settings(instrumentedTestSettings: _*)
  .settings(noPublish: _*)

lazy val instrumentedTestSettings = {
  def makeAgentOptions(classpath: Classpath): String = {
    val jammJar = classpath.map(_.data).filter(_.toString.contains("jamm")).head
    s"-javaagent:$jammJar"
  }
  Seq(
    javaOptions in Test += makeAgentOptions(
      (dependencyClasspath in Test).value
    ),
    libraryDependencies += "com.github.jbellis" % "jamm" % "0.3.0" % "test",
    fork := true
  )
}
