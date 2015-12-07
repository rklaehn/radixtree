import ReleaseTransformations._

lazy val radixtreeSettings = Seq(
  organization := "com.rklaehn",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7"),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scalatest" %%% "scalatest" % "3.0.0-M7" % "test",
    "org.spire-math" %%% "spire" % "0.11.0",
    "com.rklaehn" %%% "sonicreducer" % "0.2.0",

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
  .aggregate(radixtreeJVM, radixtreeJS)
  .settings(name := "radixtree-root")
  .settings(radixtreeSettings: _*)
  .settings(noPublish: _*)

lazy val radixtree = crossProject.crossType(CrossType.Pure).in(file("."))
  .settings(name := "radixtree")
  .settings(radixtreeSettings: _*)

lazy val radixtreeInstrumentedTest = project.in(file("instrumented-test"))
  .settings(name := "radixtreeInstrumentedTest")
  .settings(radixtreeSettings: _*)
  .settings(instrumentedTestSettings: _*)
  .dependsOn(radixtreeJVM)

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

lazy val radixtreeJVM = radixtree.jvm
lazy val radixtreeJS = radixtree.js
