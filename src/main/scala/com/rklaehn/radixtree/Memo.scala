package com.rklaehn.radixtree

import spire.algebra.Eq

import scala.util.hashing.Hashing

private object Memo {

  def simple[A](implicit e: Eq[A], h: Hashing[A]): A ⇒ A = new (A ⇒ A) {

    val memo = new scala.collection.mutable.AnyRefMap[Element[A], Element[A]]

    def apply(a: A): A = {
      val k = Element(a)
      memo.getOrElseUpdate(k, k).value
    }
  }

  final case class Element[A](value: A)(implicit e: Eq[A], h: Hashing[A]) {

    override def equals(that: Any): Boolean = that match {
      case that: Element[A] => e.eqv(this.value, that.value)
      case _ => false
    }

    override val hashCode: Int = h.hash(value)
  }
}
