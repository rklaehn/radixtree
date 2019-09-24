package com.rklaehn.radixtree

import scala.util.hashing.Hashing
import cats.kernel.{Eq, Hash}

private object Memo {

  def fromFunction[A: Hash](f: A ⇒ A): A ⇒ A = new (A ⇒ A) {

    val memo = new scala.collection.mutable.AnyRefMap[Element[A], Element[A]]

    def apply(a: A): A = {
      val k = Element(a)
      memo.getOrElseUpdate(k, Element(f(a))).value
    }
  }

  def simple[A: Hash]: A ⇒ A = new (A ⇒ A) {

    val memo = new scala.collection.mutable.AnyRefMap[Element[A], Element[A]]

    def apply(a: A): A = {
      val k = Element(a)
      memo.getOrElseUpdate(k, k).value
    }
  }

  // scalastyle:off equals.hash.code
  final case class Element[A](value: A)(implicit e: Eq[A], h: Hash[A]) {

    override def equals(that: Any): Boolean = that match {
      case that: Element[A] => e.eqv(this.value, that.value)
      case _ => false
    }

    override val hashCode: Int = h.hash(value)
  }
}
