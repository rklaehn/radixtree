import ReleaseTransformations._

lazy val commonSettings = Seq(
  organization := "com.rklaehn",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "com.rklaehn" %%% "sonicreducer" % "0.5.0",
    "org.typelevel" %%% "cats" % "0.9.0",
    "org.typelevel" %%% "algebra" % "0.7.0",
    "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
    "org.typelevel" %%% "algebra-laws" % "0.7.0" % "test",

    // thyme
    "ichi.bench" % "thyme" % "0.1.1" % "test" from "https://github.com/Ichoran/thyme/raw/9ff531411e10c698855ade2e5bde77791dd0869a/Thyme.jar"
  ),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature"
  ),
  licenses += ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("http://github.com/rklaehn/radixtree")),

  // release stuff
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  publishTo <<= version { v =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra :=
    <scm>
      <url>git@github.com:rklaehn/radixtree.git</url>
      <connection>scm:git:git@github.com:rklaehn/radixtree.git</connection>
    </scm>
      <developers>
        <developer>
          <id>r_k</id>
          <name>R&#xFC;diger Klaehn</name>
          <url>http://github.com/rklaehn/</url>
        </developer>
      </developers>
  ,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    ReleaseStep(action = Command.process("package", _)),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges))

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false)

lazy val root = project.in(file("."))
  .aggregate(coreJVM, coreJS, instrumentedTest)
  .settings(name := "root")
  .settings(commonSettings: _*)
  .settings(noPublish: _*)

lazy val core = crossProject.crossType(CrossType.Pure).in(file("."))
  .settings(name := "radixtree")
  .settings(commonSettings: _*)

lazy val instrumentedTest = project.in(file("instrumentedTest"))
  .settings(name := "instrumentedTest")
  .settings(commonSettings: _*)
  .settings(instrumentedTestSettings: _*)
  .settings(noPublish: _*)
  .dependsOn(coreJVM)

lazy val instrumentedTestSettings = {
  def makeAgentOptions(classpath:Classpath) : String = {
    val jammJar = classpath.map(_.data).filter(_.toString.contains("jamm")).head
    s"-javaagent:$jammJar"
  }
  Seq(
    javaOptions in Test <+= (dependencyClasspath in Test).map(makeAgentOptions),
      libraryDependencies += "com.github.jbellis" % "jamm" % "0.3.0" % "test",
      fork := true
    )
}

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
