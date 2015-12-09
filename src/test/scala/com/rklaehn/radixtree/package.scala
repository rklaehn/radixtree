package com.rklaehn.radixtree

import algebra.Eq

object Instances {

  // todo: remove once algebra has array instances (or use spire for instances once that moves to algebra)?
  implicit def arrayEq[A](implicit aEq: Eq[A]): Eq[Array[A]] = new Eq[Array[A]] {
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

  // todo: remove once algebra has unit instances (or use spire for instances once that moves to algebra)?
  implicit object unitEq extends Eq[Unit] {

    def eqv(x: Unit, y: Unit) = true
  }

  // todo: remove once algebra has unit instances (or use spire for instances once that moves to algebra)?
  implicit def tuple2Eq[A, B](implicit aEq: Eq[A], bEq: Eq[B]): Eq[(A, B)] = new Eq[(A, B)] {
    def eqv(x: (A, B), y: (A, B)) = aEq.eqv(x._1, y._1) && bEq.eqv(x._2, y._2)
  }
}
