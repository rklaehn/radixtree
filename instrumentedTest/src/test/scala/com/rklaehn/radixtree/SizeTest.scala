package com.rklaehn.radixtree
import org.github.jamm.MemoryMeter

import scala.collection.immutable.SortedSet
import scala.io.Source

object TestData {

  val de = Source.fromInputStream(getClass.getResourceAsStream("/germanwords.txt"), "iso-8859-1").getLines.toArray

  val en = Source.fromInputStream(getClass.getResourceAsStream("/englishWords.txt")).getLines.toArray
}

object SizeTest extends App {

  implicit object eqHash extends Hash[Unit] {

    override def hash(x: Unit): Int = 0

    override def eqv(x: Unit, y: Unit) = true
  }

  lazy val mm = new MemoryMeter()
  lazy val overhead = mm.measure(new java.lang.Object)
  lazy val pointerSize = (mm.measure(new Array[java.lang.Object](256)) - mm.measure(new Array[java.lang.Object](128))) / 128
  // lazy val englishWords = Source.fromURL("http://www-01.sil.org/linguistics/wordlists/english/wordlist/wordsEn.txt").getLines.toArray
  val englishWords = TestData.de
  println(englishWords.take(10).toIndexedSeq)

  def test(text: String, names: Seq[String]): Unit = {
    val pairs = names.map(name => name -> (())).toArray
    //    val pairsC = names.map(name => name.toCharArray -> (())).toArray
    //    val pairsB = names.map(name => name.getBytes("UTF-8") -> (())).toArray
    val tree1 = RadixTree(pairs: _*)
    val tree2 = tree1.packed
    val hashSet = pairs.toSet
    val sortedSet = pairs.to[SortedSet]
    //    val tree1c = RadixTree(pairsC: _*)
    //    val tree2c = tree1c.packed
    //    val tree1b = RadixTree(pairsB: _*)
    //    val tree2b = tree1b.packed
    println(text)
    println("\tElements:           " + mm.measureDeep(pairs))
    println("\tHashSet:            " + mm.measureDeep(hashSet))
    println("\tSortedSet:          " + mm.measureDeep(sortedSet))
    println("\tRadixTree:          " + mm.measureDeep(tree1))
    println("\tRadixTree (packed): " + mm.measureDeep(tree2))
    //    println("\tRadixTree:          " + mm.measureDeep(tree1c))
    //    println("\tRadixTree (packed): " + mm.measureDeep(tree2c))
    //
    //    println("\tRadixTree:          " + mm.measureDeep(tree1b))
    //    println("\tRadixTree (packed): " + mm.measureDeep(tree2b))
  }

  test("Numbers from 0 until 10000:", (0 until 10000).map(NumberToWord.apply))
  test(TestData.en.length + " English words:", TestData.en)
  test(TestData.de.length + " German words:", TestData.de)
}
