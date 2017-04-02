package com.rklaehn.radixtree

import language.implicitConversions
import reftree.render._
import reftree.diagram._
import java.nio.file.Paths
import scala.concurrent.duration._
object RadixTreeVisualization extends App with RefTreeImplicits {

  val words2 = """implementationsdefiniert muster klasse fang
                 |verfahrensweise mach andernfalls erbt
                 |unrichtig letzte endlich für
                 |gegeben sofern stillschweigend einführen
                 |auffaltungsanleitung faul kompilationsroutine vergleiche
                 |erzeuge nichtig entität überschreiben
                 |paket vertraulich zugangsbeschränkt retoure
                 |versiegelt vorgesetzte gegebenenfalls ich
                 |wirf charakterzug wahrhaftig versuche
                 |sorte unveränderliche opportunistisch außerdem
                 |währen vorfahrtGewähren""".stripMargin.split("\\s").map(_.trim).filterNot(_.isEmpty).filter(_.startsWith("v"))

  val words = (40 until 80).map(NumberToWord.apply)

  val trees: Seq[RadixTree[String, Unit]] = words2.scanLeft(RadixTree.empty[String, Unit]) { case (tree, value) =>
      tree.merge(RadixTree(value -> (())))
  }.drop(1)

  val lastPacked = trees.last.packed
  val trees1 = trees ++ Seq.fill(20)(lastPacked)

  val renderer = Renderer(
    renderingOptions = RenderingOptions(density = 75),
    animationOptions = AnimationOptions(keyFrameDuration = 500.milliseconds, interpolationDuration = 500.milliseconds),
    format = "gif",
    directory = Paths.get("target")
  )

  val queueAnimation: Animation = Animation.startWithSequence(trees1)
    .build(Diagram(_).withAnchor("queue").withCaption("keywords"))

  renderer.render("radixtree-animation", queueAnimation)
}
