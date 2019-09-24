package com.rklaehn.radixtree

import cats.instances.all._
import org.scalacheck.Arbitrary
import org.scalatest.funsuite.AnyFunSuite
import org.typelevel.discipline.scalatest.Discipline
import Instances._
import cats.kernel.laws.discipline.MonoidTests

class RadixTreeLawsCheck extends AnyFunSuite with Discipline {

  implicit def arbRadixTree[K: Arbitrary : RadixTree.Key, V: Arbitrary]: Arbitrary[RadixTree[K, V]] = Arbitrary {
    for {
      kvs ← Arbitrary.arbitrary[List[(K, V)]]
    } yield
    RadixTree(kvs: _*)
  }

  checkAll("MonoidTests[RadixTree[String, String]].monoid", MonoidTests[RadixTree[String, String]].monoid)
  checkAll("MonoidTests[RadixTree[Array[Byte], Array[Byte]]].monoid", MonoidTests[RadixTree[Array[Byte], Array[Byte]]].monoid)
}
