package com.rklaehn.radixtree


import cats._
import cats.instances.all._
import org.scalatest.funsuite.AnyFunSuite

import Instances._

class RadixTreeTest extends AnyFunSuite {

  implicit class StringOps(underlying: String) {
    def toBytes = underlying.getBytes("UTF-8")

    def toShorts = toBytes.map(_.toShort)
  }

  val kvs = (0 until 100).map(i => i.toString -> i)

  val kvs1k = (0 to 1000).map(i => i.toString -> i)

  val tree = RadixTree(kvs: _*)

  val tree1k = RadixTree(kvs1k: _*)

  val bkvs = (0 until 100).map(i => i.toString.toBytes -> i.toString.toBytes)

  val btree = RadixTree(bkvs: _*)

  val skvs = (0 until 100).map(i => i.toString.toShorts -> i.toString.toShorts)

  val stree = RadixTree(skvs: _*)

  val ckvs = (0 until 100).map(i => i.toString.toCharArray -> i.toString.toCharArray)

  val ctree = RadixTree(ckvs: _*)

  val bkvs1k = (0 to 1000).map(i => i.toString.toBytes -> i.toString.toBytes)

  val btree1k = RadixTree(bkvs1k: _*)

  val textkvs = (0 until 1000).map(x => NumberToWord(x) -> x)

  val texttree = RadixTree(textkvs: _*)

  def testCreate[K: RadixTree.Key, V: Eq](kvs: (K, V)*): Unit = {
    val tree = RadixTree(kvs: _*)
    assert(kvs.size === tree.count)
    for ((k, v) <- kvs) {
      assert(tree.contains(k))
      assert(tree.get(k).isDefined)
      assert(Eq.eqv(tree(k), v))
    }
  }

  def testEquals[K: Eq, V: Eq](kvs: (K, V)*)(implicit f: RadixTree.Key[K]): Unit = {
    assert(Eq.eqv(RadixTree(kvs: _*), RadixTree(kvs.reverse: _*)))
  }

  def testHashCode[K: Hash, V: Hash](kvs: (K, V)*)(implicit f: RadixTree.Key[K]): Unit = {
    assert(Hash.hash(RadixTree(kvs: _*)) === Hash.hash(RadixTree(kvs.reverse: _*)))
  }

  def testGeneric[K: RadixTree.Key: Hash, V: Hash](kvs: (K, V)*): Unit = {
    testCreate(kvs: _*)
    testHashCode(kvs: _*)
    testEquals(kvs: _*)
  }

  test("createFromPairs") {
    assert(RadixTree.apply[String, String]().isEmpty)
  }

  test("generic") {
    testGeneric(kvs: _*)
    testGeneric(ckvs: _*)
    testGeneric(bkvs: _*)
    testGeneric(skvs: _*)
    testGeneric(kvs1k: _*)
    testGeneric(bkvs1k: _*)
  }

  test("equals") {
    intercept[UnsupportedOperationException] {
      tree.equals("foo")
    }
    assert(Eq.eqv(tree, RadixTree(kvs: _*)))
    assert(Eq.eqv(tree, RadixTree(kvs.reverse: _*)))
    assert(Eq.eqv(btree, RadixTree(bkvs: _*)))
    assert(Eq.eqv(btree, RadixTree(bkvs.reverse: _*)))
  }

  test("hashCode") {
    intercept[UnsupportedOperationException] {
      tree.hashCode
    }
    assert(Hash.hash(tree) === Hash.hash(RadixTree(kvs: _*)))
    assert(Hash.hash(btree) === Hash.hash(RadixTree(bkvs: _*)))
  }

  test("mapValues") {
    assert(Eq.eqv(RadixTree("1" → 1, "11" → 11).mapValues(_.toString), RadixTree("1" → "1", "11" → "11")))
  }

  test("toString") {
    import cats.implicits._
    assert(!RadixTree("1" -> 1).toString.isEmpty)
    assert(!RadixTree("1" -> 1).printStructure.isEmpty)
    assert(!RadixTree("1" -> 1).show.isEmpty)
  }

  test("startsWith") {
    assert(RadixTree("1" -> 1).startsWith("1"))
    assert(RadixTree("11" -> 1).startsWith("1"))
    assert(!RadixTree("11" -> 1).startsWith("2"))
    assert(!RadixTree("1" -> 1).startsWith("11"))
  }

  test("emptyIsEmpty") {
    assert(RadixTree.empty[String, Int].isEmpty)
    assert(RadixTree.empty[Array[Byte], Array[Byte]].isEmpty)
  }

  test("contains") {
    assert(kvs.size === tree.count)
    for (i <- 0 until 100)
      assert(i === tree(i.toString))
    assert(!tree.contains("101"))
    assert(!tree.contains("-1"))
    assert(!RadixTree("a" -> 1).contains("b"))
  }

  test("mergeNoCollision") {
    val a = RadixTree("a" -> 1)
    val b = a.mergeWith(a, _ + _)
    assert(2 === b("a"))
  }

  test("pairs") {
    assert(kvs.toSet === tree.entries.toSet)
    assert(Eq[Array[(Array[Byte], Array[Byte])]].eqv(btree.entries.toArray, RadixTree(bkvs: _*).entries.toArray))
    assert(Eq[Array[(Array[Char], Array[Char])]].eqv(ctree.entries.toArray, RadixTree(ckvs: _*).entries.toArray))
  }

  test("packed") {
    assert(Eq.eqv(tree, tree.packed))
    assert(Eq.eqv(tree1k, tree1k.packed))
    val strings = (0 until 100).map(x => "%03d".format(x) -> (()))
    val t = RadixTree(strings: _*).packed
    assert(t.children(0).children(0) eq t.children(1).children(0))
  }

  test("keys") {
    assert(kvs.map(_._1).toSet === tree.keys.toSet)
  }

  test("values") {
    assert(kvs.map(_._2).toSet === tree.values.toSet)
  }

  test("filterPrefix") {
    assert(kvs.filter { case (k, v) => k.startsWith("1") }.toSeq === tree.filterPrefix("1").entries.toSeq)
    assert(RadixTree("1" -> 1).filterPrefix("foo").isEmpty)
    assert(RadixTree("1" -> 1, "12" -> 12).filterPrefix("123").isEmpty)
  }

  test("filterPrefixesOf") {
    assert(RadixTree("1" -> 1).entries.toSeq === tree.filterPrefixesOf("1x").entries.toSeq)
    assert(RadixTree("1" -> 1).filterPrefixesOf("foo").isEmpty)
    assert(RadixTree("1" -> 1, "12" -> 12).filterPrefixesOf("2").isEmpty)
  }

  test("modifyOrRemove") {
    val tree1 = tree.modifyOrRemove { case (k, v, _) => Some(v * 2) }
    val tree2 = tree.modifyOrRemove { case (k, v, _) => None }
    for ((k, v) <- kvs)
      assert(v * 2 === tree1(k))
    assert(tree2.isEmpty)
  }

  test("subtreeWithPrefix") {
    assert(tree.subtreeWithPrefix("x").isEmpty)
    assert(11 === tree.subtreeWithPrefix("1").count)
  }

  test("filter") {
    assert(kvs.filter { case (k, v) => k.startsWith("1") }.toSeq === tree.filter { case (k, v) => k.startsWith("1") }.entries.toSeq)
  }

  test("filterKeysContaining1") {
    val tt = RadixTree("aa" -> 1, "ab" -> 2, "a" -> 3)
    assert(1 === tt.filterKeysContaining("aa").count)
    assert(0 === tt.filterKeysContaining("aaa").count)
    val t = RadixTree("a" -> 1)
    assert(t.filterKeysContaining("b").isEmpty)
    assert(!t.filterKeysContaining("a").isEmpty)
    assert(
      kvs1k.count(_._1.contains("10")) ===
        tree1k.filterKeysContaining("10").count
    )
    assert(
      kvs.count(_._1.contains("1")) ===
        tree.filterKeysContaining("1").count
    )
    assert(
      textkvs.count(_._1.contains("eight")) ===
        texttree.filterKeysContaining("eight").count
    )
  }

  test("filterKeysContaining2") {
    assert(
      kvs1k.count(_._1.contains("10")) ===
        btree1k.filterKeysContaining("10".toBytes).count
    )
    assert(1 === RadixTree("abcd".toBytes -> 1).filterKeysContaining("bcd".toBytes).count)
  }

  test("memo") {
    val x = Memo.Element[Int](3)
    assert(!(x === "foo"))
  }

  test("opt") {
    intercept[NoSuchElementException] {
      Opt.empty[Int].get
    }
    assert(Opt.empty[Int].toOption.isEmpty)
    assert(Opt.empty[Int].toString === "Opt.empty")
  }

  test("getOrNull") {
    val t = RadixTree.singleton("ab", "x")
    assert(t.getOrNull("ab") == "x")
    assert(t.getOrNull("ba") eq null)
  }

  test("getOrDefault") {
    val t = RadixTree.singleton("ab", 3)
    assert(t.getOrDefault("ab", 7) == 3)
    assert(t.getOrDefault("ba", 7) == 7)
  }

  test("eq") {
    Eq.eqv(tree, tree.packed)
  }

  test("implicitly") {
    assert(implicitly[RadixTree.Key[String]].getClass.getSimpleName === "StringKey$")
    assert(implicitly[RadixTree.Key[Array[Byte]]].getClass.getSimpleName === "ByteArrayKey$")
    assert(implicitly[RadixTree.Key[Array[Char]]].getClass.getSimpleName === "CharArrayKey$")
    assert(implicitly[RadixTree.Key[Array[Short]]].getClass.getSimpleName === "ArrayKey")
  }

  test("arrayIsKey") {
    assert(RadixTree.arrayIsKey[Byte].concat("a".toBytes, "b".toBytes).length == 2)
    assert(RadixTree.arrayIsKey[Byte].neqv("a".toBytes, "b".toBytes))
  }
  test("monoid") {
    val entries = (0 until 100).map(x => x.toString -> (()))
    val singles = entries.map { case (k, v) => RadixTree.singleton(k, v) }
    assert(Eq.eqv(RadixTree(entries: _*), Monoid[RadixTree[String, Unit]].combineAll(singles)))
  }
  test("wordCount") {
    import scala.io.Source
    val text = Source.fromURL("http://classics.mit.edu/Homer/odyssey.mb.txt").getLines
    val words = text.flatMap(_.split(' ')).filterNot(_.isEmpty)
    val m = Monoid[RadixTree[String, Int]]
    val count = words.map(x ⇒ RadixTree(x → 1)).reduce(m.combine)
    println(count.entries.take(10))
  }
  test("arrayEq") {
    assert(!arrayEqv(Array(1,2), Array(1,3)))
  }
}
