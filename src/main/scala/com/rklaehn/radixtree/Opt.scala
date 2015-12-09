package com.rklaehn.radixtree

import spire.algebra.Eq

private object Opt {
  def apply[A](a: A): Opt[A] = new Opt(a)
  def empty[A]: Opt[A] = new Opt[A](null.asInstanceOf[A])
  def fromOption[A](a: Option[A]) = a match {
    case Some(x) => Opt(x)
    case None => Opt.empty[A]
  }

  implicit def Eq[A](implicit aEq: Eq[A]): Eq[Opt[A]] = new Eq[Opt[A]] {
    def eqv(x: Opt[A], y: Opt[A]): Boolean =
      if (x.isEmpty) y.isEmpty else aEq.eqv(x.ref, y.ref)
  }
}

private class Opt[+A](val ref: A) extends AnyVal {
  def isDefined: Boolean = ref != null
  def isEmpty: Boolean = ref == null

  def get: A = if (ref == null) throw new NoSuchElementException("Opt.empty.get") else ref

  def map[B](f: A => B): Opt[B] =
    if (ref == null) Opt.empty else Opt(f(ref))

  def toOption: Option[A] = if (ref == null) None else Some(ref)
}
