package com.rklaehn.radixtree

import cats.{Monoid, Semigroup}
import cats.kernel._
import cats.Show
import cats.kernel.Hash
import com.rklaehn.radixtree.RadixTree.Key
import com.rklaehn.sonicreducer.Reducer

import scala.annotation.tailrec
import scala.collection.AbstractTraversable
import scala.reflect.ClassTag
import scala.util.hashing.MurmurHash3

final class RadixTree[K, V](val prefix: K, private[radixtree] val children: Array[RadixTree[K, V]], private[radixtree] val valueOpt: Opt[V]) extends NoEquals {

  def packed(implicit K: Key[K], V: Hash[V]): RadixTree[K, V] = {
    val keyMemo = Memo.simple[K]
    val valueMemo = Memo.simple[V]
    val nodeMemo = Memo.simple[RadixTree[K, V]]
    lazy val pack0: RadixTree[K, V] => RadixTree[K, V] = {
      tree: RadixTree[K, V] =>
        val tree1 = tree.copy(prefix = keyMemo(tree.prefix), valueOpt = tree.valueOpt.map(valueMemo), children = tree.children.map(pack0))
        nodeMemo(tree1)
    }
    pack0(this)
  }

  def count: Int = {
    var n = if (valueOpt.isDefined) 1 else 0
    var i = 0
    while (i < children.length) {
      n += children(i).count
      i += 1
    }
    n
  }

  def printStructure: String = children
    .mkString(s"RadixTree($prefix, $valueOpt, [", ",", "])")

  def isEmpty(implicit K: Key[K]) = K.size(prefix) == 0

  def prepend(prefix: K)(implicit K: Key[K]): RadixTree[K, V] =
    new RadixTree[K, V](K.concat(prefix, this.prefix), children, valueOpt)

  def startsWith(prefix: K)(implicit K: Key[K]) =
    filterPrefix(prefix) eq this

  def filterPrefix(prefix: K)(implicit K: Key[K]): RadixTree[K, V] =
    filterPrefix0(prefix, 0)

  def filterPrefixesOf(query: K)(implicit K: Key[K]): RadixTree[K, V] = {
    val ft = filterPrefixesOf0(this, query, 0)
    if (ft.prefix == K.empty)
      if (! ft.children.isEmpty) ft.children.head
      else RadixTree.empty
    else ft
  }

  def subtreeWithPrefix(prefix: K)(implicit K: Key[K]): RadixTree[K, V] = {
    val tree1 = filterPrefix(prefix)
    if (K.startsWith(tree1.prefix, prefix, 0))
      tree1.copy(prefix = K.slice(tree1.prefix, K.size(prefix), K.size(tree1.prefix)))
    else
      RadixTree.empty
  }

  //    override protected[this] def newBuilder: mutable.Builder[(K, V), Traversable[(K, V)]] = new ArrayBuffer[(K, V)]()

  def entries(implicit K: Key[K]): Traversable[(K, V)] = new AbstractTraversable[(K, V)] {
    def foreach[U](f: ((K, V)) => U) = foreachEntry(K.empty, f)
  }

  def values(implicit K: Key[K]): Traversable[V] = new AbstractTraversable[V] {
    def foreach[U](f: V => U) = foreachValue(f)
  }

  def keys(implicit K: Key[K]): Traversable[K] = new AbstractTraversable[K] {
    def foreach[U](f: K => U) = foreachKey(K.empty, f)
  }

  private def foreachChild[U](f: RadixTree[K, V] => U) {
    var i = 0
    while (i < children.length) {
      f(children(i))
      i += 1
    }
  }

  private def foreachEntry[U](prefix: K, f: ((K, V)) => U)(implicit K: Key[K]) {
    val newPrefix = K.concat(prefix, this.prefix)
    if (valueOpt.isDefined)
      f((newPrefix, valueOpt.get))
    foreachChild(_.foreachEntry(newPrefix, f))
  }

  private def foreachValue[U](f: V => U)(implicit K: Key[K]) {
    if (valueOpt.isDefined)
      f(valueOpt.get)
    foreachChild(_.foreachValue(f))
  }

  private def foreachKey[U](prefix: K, f: K => U)(implicit K: Key[K]) {
    val newPrefix = K.concat(prefix, this.prefix)
    if (valueOpt.isDefined)
      f(newPrefix)
    foreachChild(_.foreachKey(newPrefix, f))
  }

  private def filterPrefix0(pre: K, offset: Int)(implicit K: Key[K]): RadixTree[K, V] = {
    val ps = K.size(prefix)
    val pres = K.size(pre)
    val maxFd = ps min (pres - offset)
    val fd = K.indexOfFirstDifference(prefix, 0, pre, offset, maxFd)
    if (fd == maxFd) {
      if (maxFd < ps || pres - offset == ps)
        this
      else {
        val index = K.binarySearch(children, pre, offset + ps)
        if (index >= 0) {
          val child1 = children(index).filterPrefix0(pre, offset + ps)
          val children1 =
            if (child1.isEmpty) RadixTree.emptyChildren[K, V]
            else Array(child1)
          copy(valueOpt = Opt.empty[V], children = children1)
        } else
          RadixTree.empty
      }
    } else
      RadixTree.empty
  }

  private def filterPrefixesOf0(acc: RadixTree[K, V], query: K, offset: Int)(implicit K: Key[K]): RadixTree[K, V] = {
    val ps = K.size(prefix)
    val qs = K.size(query) - offset
    val maxFd = ps min qs
    val fd = K.indexOfFirstDifference(prefix, 0, query, offset, maxFd)
    if (fd == maxFd) {
      if (maxFd < ps) RadixTree.empty
      else if (qs == ps) copy(children = RadixTree.emptyChildren)
      else {
        val index = K.binarySearch(children, query, offset + ps)
        if (index >= 0) {
          val child1 = children(index).filterPrefixesOf0(this, query, offset + ps)
          val children1 = if (child1.isEmpty) RadixTree.emptyChildren[K, V] else Array(child1)
          copy(children = children1)
        } else {
          copy(children = RadixTree.emptyChildren)
        }
      }
    } else RadixTree.empty
  }

  def modifyOrRemove(f: (K, V, Int) => Option[V])(implicit K: Key[K]): RadixTree[K, V] =
    modifyOrRemove0(f, K.empty)

  private def modifyOrRemove0(f: (K, V, Int) => Option[V], prefix: K)(implicit K: Key[K]): RadixTree[K, V] = {
    val newPrefix = K.concat(prefix, this.prefix)
    val builder = Array.newBuilder[RadixTree[K, V]]
    builder.sizeHint(children.length)
    for (child <- children) {
      val child1 = child.modifyOrRemove0(f, newPrefix)
      if (!child1.isEmpty)
        builder += child1
    }
    val temp = builder.result()
    val children1 =
      if (children.length == temp.length && children.corresponds(temp)(_ eq _)) children
      else if(temp.isEmpty) RadixTree.emptyChildren[K, V]
      else temp
    val valueOpt1 = if (valueOpt.isDefined) Opt.fromOption(f(newPrefix, valueOpt.get, children1.length)) else Opt.empty
    copy(children = children1, valueOpt = valueOpt1)
  }

  def mapValues[V2](f: V ⇒ V2): RadixTree[K, V2] =
    new RadixTree[K, V2](prefix, children.map(_.mapValues(f)), valueOpt.map(f))

  def filter(f: (K, V) => Boolean)(implicit K: Key[K]): RadixTree[K, V] =
    filter0(f, K.empty)

  private def filter0(f: (K, V) => Boolean, prefix: K)(implicit K: Key[K]): RadixTree[K, V] = {
    val prefix1 = K.concat(prefix, this.prefix)
    val builder = Array.newBuilder[RadixTree[K, V]]
    builder.sizeHint(children.length)
    for (child <- children) {
      val child1 = child.filter0(f, prefix1)
      if (!child1.isEmpty)
        builder += child1
    }
    val temp = builder.result()
    val children1 =
      if (children.length == temp.length && children.corresponds(temp)(_ eq _)) children
      else if(temp.isEmpty) RadixTree.emptyChildren[K, V]
      else temp
    val newValueOpt = if (valueOpt.isDefined && f(prefix1, valueOpt.get)) valueOpt else Opt.empty
    copy(children = children1, valueOpt = newValueOpt)
  }

  private def copy(
      prefix: K = this.prefix,
      valueOpt: Opt[V] = this.valueOpt,
      children: Array[RadixTree[K, V]] = this.children
    )(implicit K: Key[K]): RadixTree[K, V] = {
    def same(a: Opt[V], b: Opt[V]): Boolean =
      if (a.isDefined && b.isDefined)
        a.get.asInstanceOf[AnyRef] eq b.get.asInstanceOf[AnyRef]
      else a.isDefined == b.isDefined
    if (K.eqv(prefix, this.prefix) && same(valueOpt, this.valueOpt) && ((children eq this.children) || (children.length == 0 && this.children.length == 0)))
      this
    else if (valueOpt.isEmpty)
      children.length match {
        case 0 => RadixTree.empty
        case 1 => children(0).prepend(prefix)
        case _ => new RadixTree[K, V](K.intern(prefix), children, valueOpt)
      }
    else
      new RadixTree[K, V](K.intern(prefix), children, valueOpt)
  }

  def merge(other: RadixTree[K, V])(implicit K: Key[K]): RadixTree[K, V] =
    merge0(other, 0, null)

  def mergeWith(other: RadixTree[K, V], collision: (V, V) => V)(implicit K: Key[K]): RadixTree[K, V] =
    merge0(other, 0, collision)

  def apply(key: K)(implicit K: Key[K]) = get0(key, 0).get

  def contains(key: K)(implicit K: Key[K]) = get0(key, 0).isDefined

  def get(key: K)(implicit K: Key[K]): Option[V] = get0(key, 0).toOption

  def getOrNull(key: K)(implicit K: Key[K]): V = get0(key, 0).ref

  def getOrDefault[VV >: V](key: K, default: VV)(implicit K: Key[K]): VV = {
    val o = get0(key, 0)
    if (o.isDefined) o.get else default
  }

  @tailrec
  private def get0(key: K, offset: Int)(implicit K: Key[K]): Opt[V] =
    if (K.startsWith(key, prefix, offset)) {
      val newOffset = offset + K.size(prefix)
      if (K.size(key) == newOffset) valueOpt
      else {
        val index = K.binarySearch(children, key, newOffset)
        if (index >= 0) children(index).get0(key, newOffset)
        else Opt.empty
      }
    } else
      Opt.empty

  private def merge0(that: RadixTree[K, V], offset: Int, collision: (V, V) => V)(implicit K: Key[K]): RadixTree[K, V] = {
    val ps = K.size(prefix)
    val tps = K.size(that.prefix)
    val tps1 = tps - offset
    val maxFd = ps min tps1
    val fd = K.indexOfFirstDifference(prefix, 0, that.prefix, offset, maxFd)
    if (fd == maxFd) {
      // prefixes match
      if (maxFd < ps) {
        // this.prefix is longer than (that.prefix.size - offset)
        val prefix0 = K.slice(prefix, 0, fd)
        val prefix1 = K.slice(prefix, fd, ps)
        val this1 = copy(prefix = prefix1)
        val children1 = K.mergeChildren(Array(this1), that.children, collision)
        copy(prefix = prefix0, valueOpt = that.valueOpt, children = children1)
      } else if (tps1 == ps) {
        // this.prefix is the same as other.prefix when adjusted by offset
        // merge the values and children using the collision function if necessary
        val mergedValueOpt =
          if (this.valueOpt.isDefined) {
            if ((collision ne null) && that.valueOpt.isDefined)
              Opt(collision(this.valueOpt.get, that.valueOpt.get))
            else
              this.valueOpt
          } else
            that.valueOpt
        new RadixTree[K, V](this.prefix, K.mergeChildren(this.children, that.children, collision), mergedValueOpt)
      } else {
        val childOffset = offset + K.size(prefix)
        val index = K.binarySearch(children, that.prefix, childOffset)
        val children1 = if (index >= 0) {
          val child1 = children(index).merge0(that, childOffset, collision)
          ArrayOps(children).updated(index, child1)
        } else {
          val tp1 = K.slice(that.prefix, childOffset, tps)
          val child1 = that.copy(prefix = tp1)
          ArrayOps(children).patched(-index - 1, child1)
        }
        copy(children = children1)
      }
    } else {
      // both trees have a common prefix (might be "")
      val commonPrefix = K.slice(prefix, 0, fd)
      val p1 = K.slice(this.prefix, fd, ps)
      val tp1 = K.slice(that.prefix, offset + fd, tps)
      val childA = this.copy(prefix = p1)
      val childB = that.copy(prefix = tp1)
      val children1 =
        if (K.compareAt(childA.prefix, 0, childB.prefix, 0) < 0)
          Array(childA, childB)
        else
          Array(childB, childA)
      new RadixTree[K, V](commonPrefix, children1, Opt.empty)
    }
  }

  def filterKeysContaining(fragment: K)(implicit K: Key[K], v: Hash[V]) = {

    lazy val filter = Memo.fromFunction[RadixTree[K,V]](tree ⇒ filter0(K.empty, tree))

    def filter0(prefix: K, tree: RadixTree[K, V]): RadixTree[K, V] = {
      val prefix1 = K.concat(prefix, tree.prefix)
      if (K.indexOf(prefix1, fragment) >= 0) tree
      else {
        val p1s = K.size(prefix1)
        val fs = K.size(fragment)
        val children1 = tree.children.flatMap { child =>
          val prefixEnd = K.slice(prefix1, (p1s - fs + 1) max 0, p1s)
          val pes = K.size(prefixEnd)
          var result = filter(child)
          for (i <- 1 until (fs min (pes + 1))) {
            if (K.regionMatches(fragment, 0, prefixEnd, pes - i, i))
              result = result merge child.filterPrefix(K.slice(fragment, i, fs))
          }
          if (result.isEmpty) None else Some(result)
        }
        val children2 =
          if(children1.isEmpty) RadixTree.emptyChildren[K, V]
          else children1
        tree.copy(valueOpt = Opt.empty[V], children = children2)
      }
    }

    filter0(K.empty, this)
  }
}

private class RadixTreeEqv[K: Eq, V: Eq] extends Eq[RadixTree[K, V]] {
  def eqv(x: RadixTree[K, V], y: RadixTree[K, V]) = {
    def same(a: Opt[V], b: Opt[V]): Boolean =
      if (a.isDefined && b.isDefined)
        Eq.eqv(a.get, b.get)
      else a.isDefined == b.isDefined
    Eq.eqv(x.prefix, y.prefix) &&
    same(x.valueOpt, y.valueOpt) &&
    arrayEqv(x.children, y.children)(this)
  }
}

private class RadixTreeHash[K: Hash, V: Hash] extends RadixTreeEqv[K, V] with Hash[RadixTree[K, V]] {
  override def hash(a: RadixTree[K, V]): Int = {
    val valueHash = if (a.valueOpt.isDefined) Hash.hash(a.valueOpt.get) else 0
    val prefixHash = Hash.hash(a.prefix)
    val childrenHash = arrayHash(a.children)(this)
    MurmurHash3.mixLast(MurmurHash3.mix(prefixHash, valueHash), childrenHash)
  }
}

object RadixTree {

  implicit def eqv[K: Eq, V: Eq]: Eq[RadixTree[K, V]] = new RadixTreeEqv[K, V]

  implicit def hash[K: Hash, V: Hash]: Hash[RadixTree[K, V]] = new RadixTreeHash[K, V]

  implicit def show[K: Key: Show, V: Show]: Show[RadixTree[K, V]] = Show.show {
    _.entries
      .map { case (k, v) ⇒ s"$k->$v" }
      .mkString("RadixTree(", ",", ")")
  }

  implicit def monoid[K: Key, V]: Monoid[RadixTree[K, V]] = new Monoid[RadixTree[K, V]] {

    def empty = RadixTree.empty[K, V]

    def combine(x: RadixTree[K, V], y: RadixTree[K, V]) = x merge y
  }

  def empty[K: Key, V](implicit K: Key[K]): RadixTree[K, V] =
    new RadixTree[K, V](K.empty, emptyChildren[K, V], Opt.empty)

  def singleton[K: Key, V](key: K, value: V): RadixTree[K, V] =
    new RadixTree[K, V](key, RadixTree.emptyChildren[K, V], Opt(value))

  def apply[K: Key, V](kvs: (K, V)*): RadixTree[K, V] = {
    val reducer = Reducer[RadixTree[K, V]](_ merge _)
    for ((k, v) <- kvs)
      reducer.apply(singleton(k, v))
    reducer.resultOrElse(empty[K, V])
  }

  private def emptyChildren[K, V]: Array[RadixTree[K, V]] = _emptyChildren.asInstanceOf[Array[RadixTree[K, V]]]

  private[this] val _emptyChildren = Array.empty[RadixTree[_, _]]

  trait Key[K] extends Eq[K] with Hash[K] { self ⇒

    /**
     * The empty key
     */
    def empty: K

    /**
     * The size of a key
     */
    def size(c: K): Int

    /**
     * An identity function for keys that can perform interning as an optimization
     */
    def intern(e: K): K

    def concat(a: K, b: K): K

    def slice(a: K, from: Int, until: Int): K

    /**
     * Compare key a at index ai with key b at index bi. This determines the order of keys in the tree
     */
    def compareAt(a: K, ai: Int, b: K, bi: Int): Int

    /**
     * Starting from a at ai and b at bi, compares elements of a and b until count elements have been compared or until
     * a difference has been found.
     */
    def indexOfFirstDifference(a: K, ai: Int, b: K, bi: Int, count: Int): Int

    def indexOf(a: K, b: K): Int = {
      val as = size(a)
      val bs = size(b)
      val l = as - bs
      @tailrec
      def find(ai: Int): Int = {
        if (ai > l) -1
        else if (regionMatches(a, ai, b, 0, bs)) ai
        else find(ai + 1)
      }
      find(0)
    }

    def regionMatches(a: K, ai: Int, b: K, bi: Int, count: Int) =
      indexOfFirstDifference(a, ai, b, bi, count) == ai + count

    def hash(e: K): Int

    def startsWith(a: K, b: K, ai: Int): Boolean = {
      val bs = size(b)
      (bs == 0) || (size(a) + ai >= bs) && (indexOfFirstDifference(a, ai, b, 0, bs) - ai == bs)
    }

    final def binarySearch[V](elems: Array[RadixTree[K, V]], elem: K, offset: Int): Int = {

      @tailrec
      def binarySearch0(low: Int, high: Int): Int =
        if (low <= high) {
          val mid = (low + high) >>> 1
          val c = compareAt(elem, offset, elems(mid).prefix, 0)
          if (c > 0)
            binarySearch0(mid + 1, high)
          else if (c < 0)
            binarySearch0(low, mid - 1)
          else
            mid
        } else -(low + 1)
      binarySearch0(0, elems.length - 1)
    }

    final def mergeChildren[V](a: Array[RadixTree[K, V]], b: Array[RadixTree[K, V]], f: (V, V) => V): Array[RadixTree[K, V]] = {
      val r = new Array[RadixTree[K, V]](a.length + b.length)
      var ri: Int = 0
      new BinaryMerge {

        def compare(ai: Int, bi: Int) = compareAt(a(ai).prefix, 0, b(bi).prefix, 0)

        def collision(ai: Int, bi: Int): Unit = {
          r(ri) = a(ai).mergeWith(b(bi), f)(self)
          ri += 1
        }

        def fromA(a0: Int, a1: Int, bi: Int): Unit = {
          System.arraycopy(a, a0, r, ri, a1 - a0)
          ri += a1 - a0
        }

        def fromB(ai: Int, b0: Int, b1: Int): Unit = {
          System.arraycopy(b, b0, r, ri, b1 - b0)
          ri += b1 - b0
        }

        merge0(0, a.length, 0, b.length)
      }
      r.resizeInPlace(ri)
    }
  }

  implicit val stringIsKey: Key[String] = StringKey

  private object StringKey extends Key[String] {

    override def empty: String = ""
    override def size(c: String): Int = c.length
    override def startsWith(a: String, b: String, ai: Int): Boolean = a.startsWith(b, ai)
    override def slice(a: String, from: Int, until: Int): String = a.substring(from, until)
    override def indexOf(a: String, b: String): Int = a.indexOf(b)
    override def regionMatches(a: String, ai: Int, b: String, bi: Int, count: Int): Boolean =
      a.regionMatches(ai, b, bi, count)

    @tailrec
    override def indexOfFirstDifference(a: String, ai: Int, b: String, bi: Int, count: Int): Int =
      if (count == 0 || a(ai) != b(bi)) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)

    override def concat(a: String, b: String): String = a + b
    override def eqv(a: String, b: String): Boolean = a == b
    override def intern(s: String): String = s
    override def compareAt(a: String, ai: Int, b: String, bi: Int): Int = a(ai) compare b(bi)
    override def hash(e: String): Int = scala.util.hashing.MurmurHash3.stringHash(e)
  }

  implicit val byteArrayIsKey: Key[Array[Byte]] = ByteArrayKey

  private object ByteArrayKey extends Key[Array[Byte]] {
    override val empty: Array[Byte] = Array.empty[Byte]
    override def size(c: Array[Byte]): Int = c.length
    override def slice(a: Array[Byte], from: Int, until: Int): Array[Byte] = a.slice(from, until)

    @tailrec
    override def indexOfFirstDifference(a: Array[Byte], ai: Int, b: Array[Byte], bi: Int, count: Int): Int =
      if (count == 0 || a(ai) != b(bi)) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)

    override def concat(a: Array[Byte], b: Array[Byte]): Array[Byte] = a ++ b
    override def intern(s: Array[Byte]): Array[Byte] = s
    override def compareAt(a: Array[Byte], ai: Int, b: Array[Byte], bi: Int): Int = a(ai) compare b(bi)
    override def eqv(a: Array[Byte], b: Array[Byte]): Boolean = java.util.Arrays.equals(a, b)
    override def hash(e: Array[Byte]): Int = java.util.Arrays.hashCode(e)
  }

  implicit val charArrayIsKey: Key[Array[Char]] = CharArrayKey

  private object CharArrayKey extends Key[Array[Char]] {
    override val empty: Array[Char] = Array.empty[Char]
    override def size(c: Array[Char]): Int = c.length
    override def slice(a: Array[Char], from: Int, until: Int): Array[Char] = a.slice(from, until)

    @tailrec
    override def indexOfFirstDifference(a: Array[Char], ai: Int, b: Array[Char], bi: Int, count: Int): Int =
      if (count == 0 || a(ai) != b(bi)) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)

    override def concat(a: Array[Char], b: Array[Char]): Array[Char] = a ++ b
    override def intern(s: Array[Char]): Array[Char] = s
    override def compareAt(a: Array[Char], ai: Int, b: Array[Char], bi: Int): Int = a(ai) compare b(bi)
    override def eqv(a: Array[Char], b: Array[Char]): Boolean = java.util.Arrays.equals(a, b)
    override def hash(e: Array[Char]): Int = java.util.Arrays.hashCode(e)
  }

  implicit def arrayIsKey[K: Order: Hash: ClassTag]: Key[Array[K]] =
    new ArrayKey

  private final class ArrayKey[K: Order: Hash: ClassTag] extends Key[Array[K]] {
    def size(c: Array[K]) = c.length
    val empty = Array.empty[K]
    def intern(e: Array[K]) = e
    def concat(a: Array[K], b: Array[K]): Array[K] = a ++ b
    def slice(a: Array[K], from: Int, until: Int) = a.slice(from, until)
    def compareAt(a: Array[K], ai: Int, b: Array[K], bi: Int) = Order.compare(a(ai), b(bi))
    def indexOfFirstDifference(a: Array[K], ai: Int, b: Array[K], bi: Int, count: Int) =
      if (count == 0 || Order.neqv(a(ai),b(bi))) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)
    def eqv(x: Array[K], y: Array[K]): Boolean = arrayEqv(x, y)(Hash[K])
    def hash(e: Array[K]): Int = {
      var hash = MurmurHash3.arraySeed
      var i = 0
      while(i < e.length) {
        hash = MurmurHash3.mix(hash, Hash.hash(e(i)))
        i += 1
      }
      hash
    }
  }
}
