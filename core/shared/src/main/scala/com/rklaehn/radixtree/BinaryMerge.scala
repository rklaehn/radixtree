package com.rklaehn.radixtree

import scala.annotation.tailrec

/**
  * Abstract class that can be used to implement custom binary merges with e.g. special collision behavior or an ordering
  * that is not defined via an Order[T] typeclass
  */
private abstract class BinaryMerge {

  private[this] final def binarySearchB(ai: Int, b0: Int, b1: Int): Int = {

    @tailrec
    def binarySearch0(low: Int, high: Int): Int =
      if (low <= high) {
        val mid = (low + high) >>> 1
        val c = compare(ai, mid)
        if (c > 0)
          binarySearch0(mid + 1, high)
        else if (c < 0)
          binarySearch0(low, mid - 1)
        else
          mid
      } else -(low + 1)
    binarySearch0(b0, b1 - 1)
  }

  /**
    * Compare element ai of the first sequence with element bi of the second sequence
    * @param ai an index into the first sequence
    * @param bi an index into the second sequence
    * @return -1 if a(ai) &lt; b(bi), 0 if a(ai) == b(bi), 1 if a(ai) &gt; b(bi)
    */
  def compare(ai: Int, bi: Int): Int

  /**
    * Called when elements a(ai) and b(bi) are equal according to compare
    * @param ai
    * @param bi
    */
  def collision(ai: Int, bi: Int): Unit

  /**
    * Called for a subsequence of elements of a that are not overlapping any element of b
    */
  def fromA(a0: Int, a1: Int, bi: Int): Unit

  /**
    * Called for a subsequence of elements of b that are not overlapping any element of a
    */
  def fromB(ai: Int, b0: Int, b1: Int): Unit

  def merge0(a0: Int, a1: Int, b0: Int, b1: Int): Unit = {
    if (a0 == a1) {
      if (b0 != b1)
        fromB(a0, b0, b1)
    } else if (b0 == b1) {
      fromA(a0, a1, b0)
    } else {
      val am = (a0 + a1) / 2
      val res = binarySearchB(am, b0, b1)
      if (res >= 0) {
        // same elements
        val bm = res
        // merge everything below a(am) with everything below the found element
        merge0(a0, am, b0, bm)
        // add the elements a(am) and b(bm)
        collision(am, bm)
        // merge everything above a(am) with everything above the found element
        merge0(am + 1, a1, bm + 1, b1)
      } else {
        val bm = -res - 1
        // merge everything below a(am) with everything below the found insertion point
        merge0(a0, am, b0, bm)
        // add a(am)
        fromA(am, am + 1, bm)
        // everything above a(am) with everything above the found insertion point
        merge0(am + 1, a1, bm, b1)
      }
    }
  }
}
