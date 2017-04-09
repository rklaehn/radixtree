package com.rklaehn.radixtree

import language.implicitConversions
import reftree.render._
import reftree.diagram._
import java.nio.file.Paths

import scala.collection.immutable.TreeSet
import scala.concurrent.duration._

object RadixTreeVisualization extends App {

  def build(elements: Seq[String]): RadixTree[String, Unit] = RadixTree(elements.map(_ -> (())): _*)
  def buildSequentially(elements: Seq[String]): Seq[RadixTree[String, Unit]] = {
    elements.scanLeft(RadixTree.empty[String, Unit]) { case (tree, value) =>
      tree.merge(RadixTree(value -> (())))
    }.drop(1)
  }

  val renderer = Renderer(
    renderingOptions = RenderingOptions(density = 200),
    animationOptions = AnimationOptions(keyFrameDuration = 2000.milliseconds, interpolationDuration = 1000.milliseconds, framesPerSecond = 2, loop = true),
    format = "gif",
    directory = Paths.get("target")
  )
  import renderer._

  val skalaWords = """implementationsdefiniert	muster	klasse	fang
                       verfahrensweise	mach	andernfalls	erbt
                       unrichtig	letzte	endlich	für
                       gegeben	sofern	stillschweigend	einführen
                       auffaltungsanleitung	faul	kompilationsroutine	vergleiche
                       erzeuge	nichtig	entität	überschreiben
                       paket	vertraulich	zugangsbeschränkt	retoure
                       versiegelt	vorgesetzte	gegebenenfalls	ich
                       wirf	charakterzug	wahrhaftig	versuche
                       sorte	unveränderliche	opportunistisch	außerdem
                       währen	vorfahrtGewähren""".split("\\s")

  val pwords = skalaWords.filter(_.startsWith("v")) //Seq("vertraulich", "vergleiche", "versuche", "vorgesetzte")

  def buildSimple() = {
    import SimplifiedRefTreeImplicits._
    Animation.startWithSequence(buildSequentially(pwords)).build(Diagram(_).withAnchor("a").withoutCaptions).render("skala")

    val numbers = Seq("twentyone", "twentytwo", "thirtyone", "thirtytwo", "fourtyone", "fourtytwo", "fourtythree")
    val nt1: RadixTree[String, Unit] = RadixTree(numbers.map(_ -> (())): _*)
    val nt2 = nt1.packed
    Diagram(nt1).render("unpacked")
    Diagram(nt2).render("packed")
    Animation.startWithSequence(Seq(nt1, nt2)).build(Diagram(_).withAnchor("a").withoutCaptions).render("packing-animation")
    (Diagram(nt1).withCaption("unpacked").toNamespace("one") + Diagram(nt2).withCaption("packed").toNamespace("two")).render("packing-sidebyside")

    val en = build(Seq("val","var","def"))
    val de = build(Seq("unveränderliche", "opportunistisch", "verfahrensweise"))
    val global = en.prepend("en.") merge de.prepend("de.")
    Diagram(identifierDe).render("id_de")
    Diagram(identifierEn).render("id_en")
    Diagram(identifier).render("id")

  }
  buildSimple()

  def buildTreeSet(): Unit = {
    val words = pwords.take(4)
    TreeSet("a", "b", "c", "d")
    TreeSet("d", "c", "b", "a")
    val s1 = Seq("a", "b", "c", "d").scanLeft(TreeSet.empty[String])(_ + _).tail
    val s2 = Seq("d", "c", "b", "a").scanLeft(TreeSet.empty[String])(_ + _).tail
    Animation.startWithSequence(s1).build(Diagram(_).withAnchor("a").withoutCaptions).render("treeset1")
    Animation.startWithSequence(s2).build(Diagram(_).withAnchor("a").withoutCaptions).render("treeset2")
//    val d1 = Diagram(TreeSet(pwords: _*))
//    val d2 = Diagram(TreeSet(pwords.reverse: _*))
//    import renderer._
//    (d1.toNamespace("one") + d2.toNamespace("two")).render("sidebyside")
  }
  buildTreeSet()

//  val words2 = """implementationsdefiniert muster klasse fang
//                 |verfahrensweise mach andernfalls erbt
//                 |unrichtig letzte endlich für
//                 |gegeben sofern stillschweigend einführen
//                 |auffaltungsanleitung faul kompilationsroutine vergleiche
//                 |erzeuge nichtig entität überschreiben
//                 |paket vertraulich zugangsbeschränkt retoure
//                 |versiegelt vorgesetzte gegebenenfalls ich
//                 |wirf charakterzug wahrhaftig versuche
//                 |sorte unveränderliche opportunistisch außerdem
//                 |währen vorfahrtGewähren""".stripMargin.split("\\s").map(_.trim).filterNot(_.isEmpty).filter(_.startsWith("v"))
//
//
//  val words = (40 until 80).map(NumberToWord.apply)
//
//  val trees = buildSequentially(words2)
//
//  val lastPacked = trees.last.packed
//  val trees1 = trees // ++ Seq.fill(20)(lastPacked)
//
//
//
//  val queueAnimation: Animation = Animation.startWithSequence(trees1)
//    .build(Diagram(_).withoutCaptions)
//
//  val d1 = Diagram(TreeSet("public", "private", "protected", "package"))
//  val d2 = Diagram(TreeSet("protected", "package", "private", "private", "public"))
//  import renderer._
//  (d1.toNamespace("one") + d2.toNamespace("two")).render("sidebyside")

//  renderer.render("radixtree-animation-svg", queueAnimation)
}
