package com.rklaehn.radixtree

import algebra.Eq

import scala.{specialized => sp}

import scala.util.hashing.Hashing

// $COVERAGE-OFF$
/**
  * This is typeclass extends Eq to provide a method hash with the law:
  * if eqv(a, b) then hash(a) == hash(b).
  *
  * This is similar to scala.util.hashing.Hashing
  */
trait Hash[@sp A] extends Any with Eq[A] with Serializable { self =>
  def hash(a: A): Int
  /**
    * Constructs a new `Eq` instance for type `B` where 2 elements are
    * equivalent iff `eqv(f(x), f(y))`.
    */
  override def on[@sp B](f: B => A): Hash[B] =
    new Hash[B] {
      def eqv(x: B, y: B): Boolean = self.eqv(f(x), f(y))
      def hash(x: B): Int = self.hash(f(x))
    }
}

trait HashFunctions {
  def hash[@sp A](a: A)(implicit ev: Hash[A]): Int = ev.hash(a)
}

object Hash extends HashFunctions {

  implicit val byteHash: Hash[Byte] = new Hash[Byte] {
    override def hash(a: Byte): Int = a.hashCode()
    override def eqv(x: Byte, y: Byte): Boolean = x == y
  }
  implicit val shortHash: Hash[Short] = new Hash[Short] {
    override def hash(a: Short): Int = a.hashCode()
    override def eqv(x: Short, y: Short): Boolean = x == y
  }
  implicit val intHash: Hash[Int] = new Hash[Int] {
    override def hash(a: Int): Int = a.hashCode()
    override def eqv(x: Int, y: Int): Boolean = x == y
  }
  implicit val longHash: Hash[Long] = new Hash[Long] {
    override def hash(a: Long): Int = a.hashCode()
    override def eqv(x: Long, y: Long): Boolean = x == y
  }
  implicit val floatHash: Hash[Float] = new Hash[Float] {
    override def hash(a: Float): Int = a.hashCode()
    override def eqv(x: Float, y: Float): Boolean = x == y
  }
  implicit val doubleHash: Hash[Double] = new Hash[Double] {
    override def hash(a: Double): Int = a.hashCode()
    override def eqv(x: Double, y: Double): Boolean = x == y
  }
  implicit val charHash: Hash[Char] = new Hash[Char] {
    override def hash(a: Char): Int = a.hashCode()
    override def eqv(x: Char, y: Char): Boolean = x == y
  }
  implicit val booleanHash: Hash[Boolean] = new Hash[Boolean] {
    override def hash(a: Boolean): Int = a.hashCode()
    override def eqv(x: Boolean, y: Boolean): Boolean = x == y
  }
  implicit val stringHash: Hash[String] = new Hash[String] {
    override def hash(a: String): Int = a.hashCode()
    override def eqv(x: String, y: String): Boolean = x == y
  }
  implicit val unitHash: Hash[Unit] = new Hash[Unit] {
    override def hash(a: Unit): Int = 0
    override def eqv(x: Unit, y: Unit): Boolean = true
  }

  /**
    * Access an implicit `Hash[A]`.
    */
  @inline final def apply[A](implicit ev: Hash[A]) = ev

  /**
    * Convert an implicit `Hash[B]` to an `Hash[A]`
    * using the given function `f`.
    */
  def by[@sp A, @sp B](f: A => B)(implicit ev: Hash[B]): Hash[A] =
    ev.on(f)

  /*
  /**
   * This gives compatibility with scala's Hashing trait
   */
  implicit def hash[A](implicit h: Hashing[A], e: Eq[A]): Hash[A] =
    new Hash[A] {

      override def eqv(x: A, y: A): Boolean = e.eqv(x, y)

      def hash(a: A) = h.hash(a)
    }
    */
}
// $COVERAGE-ON$
