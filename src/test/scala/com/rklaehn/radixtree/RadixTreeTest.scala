package com.rklaehn.radixtree

import org.scalatest.FunSuite
import algebra.Eq
import algebra.std.all._
import Instances._

class RadixTreeTest extends FunSuite {

  implicit class StringOps(underlying: String) {
    def toBytes = underlying.getBytes("UTF-8")
  }

  val kvs = (0 until 100).map(i => i.toString -> i)

  val kvs1k = (0 to 1000).map(i => i.toString -> i)

  val tree = RadixTree(kvs: _*)

  val tree1k = RadixTree(kvs1k: _*)

  val bkvs = (0 until 100).map(i => i.toString.toBytes -> i.toString.toBytes)

  val btree = RadixTree(bkvs: _*)

  val ckvs = (0 until 100).map(i => i.toString.toCharArray -> i.toString.toCharArray)

  val ctree = RadixTree(ckvs: _*)

  val bkvs1k = (0 to 1000).map(i => i.toString.toBytes -> i.toString.toBytes)

  val btree1k = RadixTree(bkvs1k: _*)

  val textkvs = (0 until 1000).map(x => NumberToWord(x) -> x)

  val texttree = RadixTree(textkvs: _*)

  def testCreate[K, V](kvs: (K, V)*)(implicit f: RadixTree.Family[K, V]): Unit = {
    val tree = RadixTree(kvs: _*)
    assert(kvs.size === tree.count)
    for ((k, v) <- kvs) {
      assert(tree.contains(k))
      assert(f.valueEq.eqv(v, tree(k)))
      assert(f.valueEq.eqv(v, tree.get(k).get))
    }
  }

  def testEquals[K, V](kvs: (K, V)*)(implicit f: RadixTree.Family[K, V]): Unit = {
    assert(RadixTree(kvs: _*) === RadixTree(kvs.reverse: _*))
  }

  def testHashCode[K, V](kvs: (K, V)*)(implicit f: RadixTree.Family[K, V]): Unit = {
    assert(RadixTree(kvs: _*).hashCode === RadixTree(kvs.reverse: _*).hashCode)
    assert(RadixTree(bkvs: _*).hashCode === RadixTree(bkvs.reverse: _*).hashCode)
    assert(RadixTree(ckvs: _*).hashCode === RadixTree(ckvs.reverse: _*).hashCode)
  }

  def testGeneric[K, V](kvs: (K, V)*)(implicit f: RadixTree.Family[K, V]): Unit = {
    testCreate(kvs: _*)
    testEquals(kvs: _*)
    testHashCode(kvs: _*)
  }

  test("createFromPairs") {
    assert(RadixTree.apply[String, String]().isEmpty)
  }

  test("generic") {
    testGeneric(kvs: _*)
    testGeneric(kvs1k: _*)
    testGeneric(bkvs1k: _*)
  }

  test("equals") {
    assert(tree === RadixTree(kvs: _*))
    assert(tree === RadixTree(kvs.reverse: _*))
    assert(btree === RadixTree(bkvs: _*))
    assert(btree === RadixTree(bkvs.reverse: _*))
    assert(!(tree === "fnord"))
  }

  test("hashCode") {
    assert(tree.## === RadixTree(kvs: _*).##)
    assert(btree.## === RadixTree(bkvs: _*).##)
  }

  test("toString") {
    assert("RadixTree(1 -> 1)" === RadixTree("1" -> 1).toString)
    assert(!RadixTree("1" -> 1).printStructure.isEmpty)
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
    val b = a.merge(a, _ + _)
    assert(2 === b("a"))
  }

  test("pairs") {
    assert(kvs.toSet === tree.pairs.toSet)
    assert(Eq[Array[(Array[Byte], Array[Byte])]].eqv(btree.pairs.toArray, RadixTree(bkvs: _*).pairs.toArray))
    assert(Eq[Array[(Array[Char], Array[Char])]].eqv(ctree.pairs.toArray, RadixTree(ckvs: _*).pairs.toArray))
  }

  test("packed") {
    assert(tree === tree.packed)
    assert(tree1k === tree1k.packed)
  }

  test("keys") {
    assert(kvs.map(_._1).toSet === tree.keys.toSet)
  }

  test("values") {
    assert(kvs.map(_._2).toSet === tree.values.toSet)
  }

  test("filterPrefix") {
    assert(kvs.filter { case (k, v) => k.startsWith("1") }.toSeq === tree.filterPrefix("1").pairs.toSeq)
    assert(RadixTree("1" -> 1).filterPrefix("foo").isEmpty)
    assert(RadixTree("1" -> 1, "12" -> 12).filterPrefix("123").isEmpty)
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
    assert(kvs.filter { case (k, v) => k.startsWith("1") }.toSeq === tree.filter { case (k, v) => k.startsWith("1") }.pairs.toSeq)
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
  }

  test("eq") {
    Eq.eqv(tree, tree.packed)
  }
}