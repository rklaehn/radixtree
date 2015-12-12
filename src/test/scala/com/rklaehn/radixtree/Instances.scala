package com.rklaehn.radixtree

import algebra.Eq

import scala.util.hashing.MurmurHash3

object Instances {

  // todo: remove once algebra has array instances (or use spire for instances once that moves to algebra)?
  implicit def ArrayEq[A](implicit aEq: Eq[A]): Eq[Array[A]] = new Eq[Array[A]] {
    def eqv(x: Array[A], y: Array[A]): Boolean = {
      x.length == y.length && {
        var i = 0
        while(i < x.length) {
          if(!aEq.eqv(x(i), y(i)))
            return false
          i += 1
        }
        true
      }
    }
  }

  implicit def ArrayHash[@specialized A](implicit aHash: Hash[A]): Hash[Array[A]] = new Hash[Array[A]] {
    def eqv(x: Array[A], y: Array[A]): Boolean = {
      x.length == y.length && {
        var i = 0
        while(i < x.length) {
          if(!aHash.eqv(x(i), y(i)))
            return false
          i += 1
        }
        true
      }
    }

    override def hash(a: Array[A]): Int = {
      var result = MurmurHash3.arraySeed
      var i = 0
      while(i < a.length) {
        result = MurmurHash3.mix(result, aHash.hash(a(i)))
        i += 1
      }
      result
    }
  }

  // todo: remove once algebra has unit instances (or use spire for instances once that moves to algebra)?
  implicit object unitEq extends Eq[Unit] {

    def eqv(x: Unit, y: Unit) = true
  }

  // todo: remove once algebra has unit instances (or use spire for instances once that moves to algebra)?
  implicit def tuple2Eq[A, B](implicit aEq: Eq[A], bEq: Eq[B]): Eq[(A, B)] = new Eq[(A, B)] {
    def eqv(x: (A, B), y: (A, B)) = aEq.eqv(x._1, y._1) && bEq.eqv(x._2, y._2)
  }
}
