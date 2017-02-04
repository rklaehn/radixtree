package com.rklaehn.radixtree

import algebra.instances.all._
import org.scalacheck.Arbitrary
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline
import Instances._
import cats.kernel.laws.GroupLaws
import algebra.laws.RingLaws

class RadixTreeLawsCheck extends FunSuite with Discipline {

  implicit def arbRadixTree[K: Arbitrary : RadixTree.Key, V: Arbitrary]: Arbitrary[RadixTree[K, V]] = Arbitrary {
    for {
      kvs ← Arbitrary.arbitrary[List[(K, V)]]
    } yield
    RadixTree(kvs: _*)
  }

  checkAll("GroupLaws[RadixTree[String, String]].monoid", GroupLaws[RadixTree[String, String]].monoid)
  checkAll("GroupLaws[RadixTree[Array[Byte], Array[Byte]]].monoid", GroupLaws[RadixTree[Array[Byte], Array[Byte]]].monoid)
  checkAll("RingLaws[RadixTree[String, Byte]].additiveMonoid", RingLaws[RadixTree[String, Short]].additiveMonoid)
  checkAll("RingLaws[RadixTree[Array[Byte], Int]].additiveMonoid", RingLaws[RadixTree[String, Int]].additiveMonoid)
}
