package com.rklaehn.radixtree

import cats.kernel.{Eq, Hash}

import scala.util.hashing.MurmurHash3

object Instances {

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
}
