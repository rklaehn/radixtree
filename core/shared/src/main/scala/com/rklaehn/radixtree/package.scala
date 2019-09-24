package com.rklaehn

import cats.kernel.{Eq, Hash}

import scala.reflect.ClassTag
import scala.util.hashing.MurmurHash3

// scalastyle:off return
package object radixtree {

  private[radixtree] def arrayEqv[A: Eq](x: Array[A], y: Array[A]): Boolean = x.length == y.length && {
    var i = 0
    while (i < x.length) {
      if (!Eq.eqv(x(i), y(i)))
        return false
      i += 1
    }
    true
  }

  private[radixtree] def arrayHash[A: Hash](a: Array[A]): Int = {
    var result = MurmurHash3.arraySeed
    var i = 0
    while(i < a.length) {
      result = MurmurHash3.mix(result, Hash.hash(a(i)))
      i += 1
    }
    result
  }

  private[radixtree] implicit class ArrayOps[T](private val underlying: Array[T]) extends AnyVal {

    def updated(index: Int, value: T): Array[T] = {
      val result = underlying.clone
      result(index) = value
      result
    }

    def patched(index: Int, value: T)(implicit c: ClassTag[T]): Array[T] = {
      val result = new Array[T](underlying.length + 1)
      System.arraycopy(underlying, 0, result, 0, index)
      result(index) = value
      if (index < underlying.length)
        System.arraycopy(underlying, index, result, index + 1, underlying.length - index)
      result
    }

    def resizeInPlace(n: Int)(implicit c: ClassTag[T]): Array[T] =
      if (underlying.length == n)
        underlying
      else {
        val r = c.newArray(n)
        System.arraycopy(underlying, 0, r, 0, n min underlying.length)
        r
      }
  }
}
