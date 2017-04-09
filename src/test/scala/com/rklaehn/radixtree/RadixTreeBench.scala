package com.rklaehn.radixtree

import scala.collection.immutable.{HashMap, SortedMap}
import scala.io.Source
import scala.util.hashing.Hashing
import Instances._
import com.rklaehn.radixtree.RadixTreeBenchGerman.getClass
import ichi.bench.Thyme

object TestData {

  val de = Source.fromInputStream(getClass.getResourceAsStream("/germanwords.txt"), "iso-8859-1").getLines.toArray

  val en = Source.fromInputStream(getClass.getResourceAsStream("/englishWords.txt")).getLines.toArray
}

object RadixTreeBench {

  lazy val th = Thyme.warmed(verbose = println, warmth = Thyme.HowWarm.BenchOff)

  def doBenchmark(names: Array[String]): Unit = {

    implicit object EqHashing extends Hashing[Unit] {

      override def hash(x: Unit): Int = 0
    }

    val kvs = names.map(s => s -> (()))

    val radixTree = RadixTree(kvs: _*)

    val radixTree1 = RadixTree(kvs: _*)

    val radixTreeP = RadixTree(kvs: _*).packed

    val sortedMap = SortedMap(kvs: _*)

    val sortedMap1 = SortedMap(kvs: _*)

    val hashMap = HashMap(kvs: _*)

    def createS[K: Ordering, V](kvs: Array[(K, V)]): Int = {
      SortedMap(kvs: _*).size
    }

    def createR[K, V](kvs: Array[(K, V)])(implicit f: RadixTree.Key[K]): Int = {
      RadixTree[K, V](kvs: _*).count
    }

    def lookupR(): Boolean = {
      kvs.forall {
        case (k, v) => radixTree.contains(new String(k))
      }
    }

    def lookupH(): Boolean = {
      kvs.forall {
        case (k, v) => hashMap.contains(new String(k))
      }
    }

    def lookupS(): Boolean = {
      kvs.forall {
        case (k, v) => sortedMap.contains(new String(k))
      }
    }

    def filterPrefixS(): AnyRef = {
      sortedMap.filter { case (k, v) => k.startsWith("ab") }
    }

    def filterPrefixH(): AnyRef = {
      hashMap.filter { case (k, v) => k.startsWith("ab") }
    }

    def filterPrefixR(): AnyRef = {
      radixTree.filterPrefix("ab")
    }

    def filterContainsS(): AnyRef = {
      sortedMap.filter { case (k, v) => k.contains("one") }
    }

    def filterContainsH(): AnyRef = {
      hashMap.filter { case (k, v) => k.contains("one") }
    }

    def filterContainsR(): AnyRef = {
      radixTree.filterKeysContaining("one")
    }

    def equalsS(): Boolean = {
      (sortedMap == sortedMap1)
    }

    def equalsR(): Boolean = {
      import cats.syntax.all._
      (radixTree === radixTree1)
    }

    th.pbenchOffWarm(s"Create ${names.length} SortedMap vs. RadixTree")(th.Warm(createS(kvs)))(th.Warm(createR(kvs)))
    th.pbenchOffWarm(s"Lookup ${names.length} HashMap vs. RadixTree")(th.Warm(lookupR()))(th.Warm(lookupH()))
    th.pbenchOffWarm(s"Lookup ${names.length} SortedMap vs. RadixTree")(th.Warm(lookupR()))(th.Warm(lookupS()))

//    th.pbenchOffWarm("FilterPrefix HashMap vs. RadixTree")(th.Warm(filterPrefixH()))(th.Warm(filterPrefixR()))
    th.pbenchOffWarm("FilterPrefix SortedMap vs. RadixTree")(th.Warm(filterPrefixS()))(th.Warm(filterPrefixR()))
//
//    th.pbenchOffWarm("FilterContains HashMap vs. RadixTree")(th.Warm(filterContainsH()))(th.Warm(filterContainsR()))
//    th.pbenchOffWarm("FilterContains SortedMap vs. RadixTree")(th.Warm(filterContainsS()))(th.Warm(filterContainsR()))

    th.pbenchOffWarm(s"Equals ${names.length} SortedMap vs. RadixTree")(th.Warm(equalsS()))(th.Warm(equalsR()))
  }
}

object RadixTreeBenchGerman extends App {

  import TestData._

  println(de.length)
  println(de.take(10).mkString("\n"))
  RadixTreeBench.doBenchmark(de.take(10000))

  println(en.length)
  println(en.take(10).mkString("\n"))
  RadixTreeBench.doBenchmark(en.take(10000))

}
