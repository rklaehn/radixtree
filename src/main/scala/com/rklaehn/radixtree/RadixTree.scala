package com.rklaehn.radixtree

import algebra.{Order, Monoid, Semigroup, Eq}
import com.rklaehn.sonicreducer.Reducer

import scala.annotation.tailrec
import scala.collection.AbstractTraversable
import scala.reflect.ClassTag
import scala.util.hashing.{MurmurHash3, Hashing}

// scalastyle:off equals.hash.code
final class RadixTree[K, V](
    val prefix: K, private[radixtree] val children: Array[RadixTree[K, V]], private val valueOpt: Opt[V]
  )(implicit
    e: RadixTree.Family[K, V]
  ) {

  private def childrenAsAnyRefArray =
    children.asInstanceOf[Array[AnyRef]]

  def packed: RadixTree[K, V] = {
    val keyMemo = Memo.simple[K](e, e)
    val valueMemo = Memo.simple[V](e.valueEq, e.valueHashing)
    val nodeMemo = Memo.simple[RadixTree[K, V]](RadixTree.Eq, Hashing.default)
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

  override def equals(obj: Any) = obj match {
    case that: RadixTree[K, V] =>
      implicit def veq = e.valueEq
      this.hashCode == that.hashCode &&
        e.eqv(this.prefix, that.prefix) &&
        Opt.same(this.valueOpt, that.valueOpt) &&
        java.util.Arrays.equals(this.childrenAsAnyRefArray, that.childrenAsAnyRefArray)
    case _ => false
  }

  override lazy val hashCode = {
    import scala.util.hashing.MurmurHash3._

    mixLast(
      mix(
        e.hash(prefix),
        java.util.Arrays.hashCode(this.childrenAsAnyRefArray)
      ),
      if (valueOpt.isDefined) e.valueHashing.hash(valueOpt.get) else 0
    )
  }

  override def toString: String = entries
    .toArray
    .map { case (k, v) => s"$k -> $v" }
    .mkString("RadixTree(", ",", ")")

  def printStructure: String = children
    .mkString(s"RadixTree($prefix, $valueOpt, [", ",", "])")

  def isEmpty = e.size(prefix) == 0

  def prepend(prefix: K): RadixTree[K, V] =
    new RadixTree[K, V](e.concat(prefix, this.prefix), children, valueOpt)

  def startsWith(prefix: K) =
    filterPrefix(prefix) eq this

  def filterPrefix(prefix: K): RadixTree[K, V] =
    filterPrefix0(prefix, 0)

  def subtreeWithPrefix(prefix: K) = {
    val tree1 = filterPrefix(prefix)
    if (e.startsWith(tree1.prefix, prefix, 0))
      tree1.copy(prefix = e.slice(tree1.prefix, e.size(prefix), e.size(tree1.prefix)))
    else
      e.emptyTree
  }

  //    override protected[this] def newBuilder: mutable.Builder[(K, V), Traversable[(K, V)]] = new ArrayBuffer[(K, V)]()

  def entries: Traversable[(K, V)] = new AbstractTraversable[(K, V)] {
    def foreach[U](f: ((K, V)) => U) = foreachEntry(e.empty, f)
  }

  def values: Traversable[V] = new AbstractTraversable[V] {
    def foreach[U](f: V => U) = foreachValue(f)
  }

  def keys: Traversable[K] = new AbstractTraversable[K] {
    def foreach[U](f: K => U) = foreachKey(e.empty, f)
  }

  private def foreachChild[U](f: RadixTree[K, V] => U) {
    var i = 0
    while (i < children.length) {
      f(children(i))
      i += 1
    }
  }

  private def foreachEntry[U](prefix: K, f: ((K, V)) => U) {
    val newPrefix = e.concat(prefix, this.prefix)
    if (valueOpt.isDefined)
      f((newPrefix, valueOpt.get))
    foreachChild(_.foreachEntry(newPrefix, f))
  }

  private def foreachValue[U](f: V => U) {
    if (valueOpt.isDefined)
      f(valueOpt.get)
    foreachChild(_.foreachValue(f))
  }

  private def foreachKey[U](prefix: K, f: K => U) {
    val newPrefix = e.concat(prefix, this.prefix)
    if (valueOpt.isDefined)
      f(newPrefix)
    foreachChild(_.foreachKey(newPrefix, f))
  }

  private def filterPrefix0(pre: K, offset: Int): RadixTree[K, V] = {
    val ps = e.size(prefix)
    val pres = e.size(pre)
    val maxFd = ps min (pres - offset)
    val fd = e.indexOfFirstDifference(prefix, 0, pre, offset, maxFd)
    if (fd == maxFd) {
      if (maxFd < ps || pres - offset == ps)
        this
      else {
        val index = e.binarySearch(children, pre, offset + ps)
        if (index >= 0) {
          val child1 = children(index).filterPrefix0(pre, offset + ps)
          val children1 =
            if (child1.isEmpty) e.emptyTree.children
            else Array(child1)
          copy(valueOpt = Opt.empty[V], children = children1)
        } else
          e.emptyTree
      }
    } else
      e.emptyTree
  }

  def modifyOrRemove(f: (K, V, Int) => Option[V]): RadixTree[K, V] =
    modifyOrRemove0(f, e.empty)

  private def modifyOrRemove0(f: (K, V, Int) => Option[V], prefix: K): RadixTree[K, V] = {
    val newPrefix = e.concat(prefix, this.prefix)
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
      else temp
    val valueOpt1 = if (valueOpt.isDefined) Opt.fromOption(f(newPrefix, valueOpt.get, children1.length)) else Opt.empty
    copy(children = children1, valueOpt = valueOpt1)
  }

  def filter(f: (K, V) => Boolean): RadixTree[K, V] =
    filter0(f, e.empty)

  private def filter0(f: (K, V) => Boolean, prefix: K): RadixTree[K, V] = {
    val prefix1 = e.concat(prefix, this.prefix)
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
      else temp
    val newValueOpt = if (valueOpt.isDefined && f(prefix1, valueOpt.get)) valueOpt else Opt.empty
    copy(children = children1, valueOpt = newValueOpt)
  }

  private def copy(prefix: K = this.prefix, valueOpt: Opt[V] = this.valueOpt, children: Array[RadixTree[K, V]] = this.children): RadixTree[K, V] = {
    def same(a: Opt[V], b: Opt[V]): Boolean =
      if (a.isDefined && b.isDefined)
        a.get.asInstanceOf[AnyRef] eq b.get.asInstanceOf[AnyRef]
      else a.isDefined == b.isDefined
    if (e.eqv(prefix, this.prefix) && same(valueOpt, this.valueOpt) && ((children eq this.children) || (children.length == 0 && this.children.length == 0)))
      this
    else if (valueOpt.isEmpty)
      children.length match {
        case 0 => e.emptyTree
        case 1 => children(0).prepend(prefix)
        case _ => e.mkNode(e.intern(prefix), valueOpt, children)
      }
    else
      e.mkNode(e.intern(prefix), valueOpt, children)
  }

  def merge(other: RadixTree[K, V]): RadixTree[K, V] =
    merge0(other, 0, null)

  def merge(other: RadixTree[K, V], collision: (V, V) => V): RadixTree[K, V] =
    merge0(other, 0, collision)

  def apply(key: K) = get0(key, 0).get

  def contains(key: K) = get0(key, 0).isDefined

  def get(key: K): Option[V] = get0(key, 0).toOption

  @tailrec
  private def get0(key: K, offset: Int): Opt[V] =
    if (e.startsWith(key, prefix, offset)) {
      val newOffset = offset + e.size(prefix)
      if (e.size(key) == newOffset) valueOpt
      else {
        val index = e.binarySearch(children, key, newOffset)
        if (index >= 0) children(index).get0(key, newOffset)
        else Opt.empty
      }
    } else
      Opt.empty

  private def merge0(that: RadixTree[K, V], offset: Int, collision: (V, V) => V): RadixTree[K, V] = {
    val ps = e.size(prefix)
    val tps = e.size(that.prefix)
    val tps1 = tps - offset
    val maxFd = ps min tps1
    val fd = e.indexOfFirstDifference(prefix, 0, that.prefix, offset, maxFd)
    if (fd == maxFd) {
      // prefixes match
      if (maxFd < ps) {
        // this.prefix is longer than (that.prefix.size - offset)
        val prefix0 = e.slice(prefix, 0, fd)
        val prefix1 = e.slice(prefix, fd, ps)
        val this1 = copy(prefix = prefix1)
        val children1 = e.mergeChildren(Array(this1), that.children, collision)
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
        e.mkNode(this.prefix, mergedValueOpt, e.mergeChildren(this.children, that.children, collision))
      } else {
        val childOffset = offset + e.size(prefix)
        val index = e.binarySearch(children, that.prefix, childOffset)
        val children1 = if (index >= 0) {
          val child1 = children(index).merge0(that, childOffset, collision)
          ArrayOps(children).updated(index, child1)
        } else {
          val tp1 = e.slice(that.prefix, childOffset, tps)
          val child1 = that.copy(prefix = tp1)
          ArrayOps(children).patched(-index - 1, child1)
        }
        copy(children = children1)
      }
    } else {
      // both trees have a common prefix (might be "")
      val commonPrefix = e.slice(prefix, 0, fd)
      val p1 = e.slice(this.prefix, fd, ps)
      val tp1 = e.slice(that.prefix, offset + fd, tps)
      val childA = this.copy(prefix = p1)
      val childB = that.copy(prefix = tp1)
      val children1 =
        if (e.compareAt(childA.prefix, 0, childB.prefix, 0) < 0)
          Array(childA, childB)
        else
          Array(childB, childA)
      e.mkNode(commonPrefix, Opt.empty, children1)
    }
  }

  def filterKeysContaining(fragment: K) = {
    val memo = new scala.collection.mutable.AnyRefMap[RadixTree[K, V], RadixTree[K, V]]

    def filter(tree: RadixTree[K, V]) =
      memo.getOrElseUpdate(tree, filter0(e.empty, tree))

    def filter0(prefix: K, tree: RadixTree[K, V]): RadixTree[K, V] = {
      val prefix1 = e.concat(prefix, tree.prefix)
      if (e.indexOf(prefix1, fragment) >= 0) tree
      else {
        val p1s = e.size(prefix1)
        val fs = e.size(fragment)
        val children1 = tree.children.flatMap { child =>
          val prefixEnd = e.slice(prefix1, (p1s - fs + 1) max 0, p1s)
          val pes = e.size(prefixEnd)
          var result = filter(child)
          for (i <- 1 until (fs min (pes + 1))) {
            if (e.regionMatches(fragment, 0, prefixEnd, pes - i, i))
              result = result merge child.filterPrefix(e.slice(fragment, i, fs))
          }
          if (result.isEmpty) None else Some(result)
        }
        tree.copy(valueOpt = Opt.empty[V], children = children1)
      }
    }

    filter0(e.empty, this)
  }
}

object RadixTree {

  implicit def Eq[K, V]: Eq[RadixTree[K, V]] = new Eq[RadixTree[K, V]] {
    def eqv(x: RadixTree[K, V], y: RadixTree[K, V]): Boolean = x == y
  }

  implicit def Monoid[K, V](implicit f: Family[K, V]): Monoid[RadixTree[K, V]] = new Monoid[RadixTree[K, V]] {

    def empty = RadixTree.empty[K, V]

    def combine(x: RadixTree[K, V], y: RadixTree[K, V]) = x merge y
  }

  def empty[K, V](implicit family: Family[K, V]): RadixTree[K, V] =
    family.emptyTree

  def singleton[K, V](key: K, value: V)(implicit family: Family[K, V]): RadixTree[K, V] =
    new RadixTree[K, V](key, family.emptyTree.children, Opt(value))

  def apply[K, V](kvs: (K, V)*)(implicit family: Family[K, V]): RadixTree[K, V] = {
    val reducer = Reducer[RadixTree[K, V]](_ merge _)
    for ((k, v) <- kvs)
      reducer.apply(singleton(k, v))
    reducer.result().getOrElse(empty[K, V])
  }

  trait Family[K, V] extends Eq[K] with Hashing[K] {

    /**
     * The Eq instance to be used for values. We can not use equals because we want this to work for Array[Byte]
     */
    def valueEq: Eq[V]

    /**
     * The hashing to be used for values. We can not use hashcode because we want this to work for Array[Byte]
     */
    def valueHashing: Hashing[V]

    /**
     * The empty key
     */
    def empty: K

    /**
      * The empty tree
      */
    def emptyTree: RadixTree[K, V]

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

    final def mkNode(prefix: K, valueOpt: Opt[V], children: Array[RadixTree[K, V]]): RadixTree[K, V] =
      new RadixTree[K, V](prefix, children, valueOpt)(this)

    final def binarySearch(elems: Array[RadixTree[K, V]], elem: K, offset: Int): Int = {

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

    final def mergeChildren(a: Array[RadixTree[K, V]], b: Array[RadixTree[K, V]], f: (V, V) => V): Array[RadixTree[K, V]] = {
      val r = new Array[RadixTree[K, V]](a.length + b.length)
      var ri: Int = 0
      new BinaryMerge {

        def compare(ai: Int, bi: Int) = compareAt(a(ai).prefix, 0, b(bi).prefix, 0)

        def collision(ai: Int, bi: Int): Unit = {
          r(ri) = a(ai).merge(b(bi), f)
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

  implicit def stringIsKey[V: Eq: Hashing]: Family[String, V] =
    new StringTreeFamily[V](implicitly[Eq[V]], implicitly[Hashing[V]])

  private final class StringTreeFamily[V](val valueEq: Eq[V], val valueHashing: Hashing[V])
      extends Family[String, V] {

    override val emptyTree: RadixTree[String, V] = new RadixTree[String, V]("", Array.empty, Opt.empty)(this)

    override def empty: String = emptyTree.prefix

    override def size(c: String): Int =
      c.length

    override def startsWith(a: String, b: String, ai: Int): Boolean =
      a.startsWith(b, ai)

    override def slice(a: String, from: Int, until: Int): String =
      a.substring(from, until)

    override def indexOf(a: String, b: String): Int =
      a.indexOf(b)

    override def regionMatches(a: String, ai: Int, b: String, bi: Int, count: Int): Boolean =
      a.regionMatches(ai, b, bi, count)

    @tailrec
    override def indexOfFirstDifference(a: String, ai: Int, b: String, bi: Int, count: Int): Int =
      if (count == 0 || a(ai) != b(bi)) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)

    override def concat(a: String, b: String): String =
      a + b

    override def eqv(a: String, b: String): Boolean =
      a == b

    override def intern(s: String): String = s

    override def compareAt(a: String, ai: Int, b: String, bi: Int): Int =
      a(ai) compare b(bi)

    override def hash(e: String): Int =
      scala.util.hashing.MurmurHash3.stringHash(e)
  }

  implicit def byteArrayIsKey[V: Eq: Hashing]: Family[Array[Byte], V] =
    new ByteArrayTreeFamily[V](implicitly[Eq[V]], implicitly[Hashing[V]])

  private final class ByteArrayTreeFamily[V](val valueEq: Eq[V], val valueHashing: Hashing[V])
      extends Family[Array[Byte], V] {

    override def empty: Array[Byte] =
      emptyTree.prefix

    override val emptyTree: RadixTree[Array[Byte], V] =
      new RadixTree[Array[Byte], V](Array.empty, Array.empty, Opt.empty)(this)

    override def size(c: Array[Byte]): Int =
      c.length

    override def slice(a: Array[Byte], from: Int, until: Int): Array[Byte] =
      a.slice(from, until)

    @tailrec
    override def indexOfFirstDifference(a: Array[Byte], ai: Int, b: Array[Byte], bi: Int, count: Int): Int =
      if (count == 0 || a(ai) != b(bi)) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)

    override def concat(a: Array[Byte], b: Array[Byte]): Array[Byte] =
      a ++ b

    override def intern(s: Array[Byte]): Array[Byte] = s

    override def compareAt(a: Array[Byte], ai: Int, b: Array[Byte], bi: Int): Int =
      a(ai) compare b(bi)

    override def eqv(a: Array[Byte], b: Array[Byte]): Boolean =
      java.util.Arrays.equals(a, b)

    override def hash(e: Array[Byte]): Int =
      java.util.Arrays.hashCode(e)
  }

  implicit def charArrayIsKey[V: Eq: Hashing]: Family[Array[Char], V] =
    new CharArrayTreeFamily[V](implicitly[Eq[V]], implicitly[Hashing[V]])

  private final class CharArrayTreeFamily[V](val valueEq: Eq[V], val valueHashing: Hashing[V])
      extends Family[Array[Char], V] {

    override def empty: Array[Char] =
      emptyTree.prefix

    override val emptyTree: RadixTree[Array[Char], V] =
      new RadixTree[Array[Char], V](Array.empty, Array.empty, Opt.empty)(this)

    override def size(c: Array[Char]): Int =
      c.length

    override def slice(a: Array[Char], from: Int, until: Int): Array[Char] =
      a.slice(from, until)

    @tailrec
    override def indexOfFirstDifference(a: Array[Char], ai: Int, b: Array[Char], bi: Int, count: Int): Int =
      if (count == 0 || a(ai) != b(bi)) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)

    override def concat(a: Array[Char], b: Array[Char]): Array[Char] =
      a ++ b

    override def intern(s: Array[Char]): Array[Char] = s

    override def compareAt(a: Array[Char], ai: Int, b: Array[Char], bi: Int): Int =
      a(ai) compare b(bi)

    override def eqv(a: Array[Char], b: Array[Char]): Boolean =
      java.util.Arrays.equals(a, b)

    override def hash(e: Array[Char]): Int =
      java.util.Arrays.hashCode(e)
  }

  implicit def arrayIsKey[K: Order: Hashing: ClassTag, V: Eq: Hashing]: Family[Array[K], V] =
    new ArrayTreeFamily

  private final class ArrayTreeFamily[K, V]
  ( implicit
    val keyOrder: Order[K],
    val keyHashing: Hashing[K],
    val keyClassTag: ClassTag[K],
    val valueEq: Eq[V],
    val valueHashing: Hashing[V]
  ) extends Family[Array[K], V] {
    def size(c: Array[K]) = c.length
    val empty = Array.empty[K]
    val emptyTree = new RadixTree(empty, Array.empty[RadixTree[Array[K], V]], Opt.empty[V])(this)
    def intern(e: Array[K]) = e
    def concat(a: Array[K], b: Array[K]): Array[K] = a ++ b
    def slice(a: Array[K], from: Int, until: Int) = a.slice(from, until)
    def compareAt(a: Array[K], ai: Int, b: Array[K], bi: Int) = keyOrder.compare(a(ai), b(bi))
    def indexOfFirstDifference(a: Array[K], ai: Int, b: Array[K], bi: Int, count: Int) =
      if (count == 0 || keyOrder.neqv(a(ai),b(bi))) ai
      else indexOfFirstDifference(a, ai + 1, b, bi + 1, count - 1)
    def eqv(x: Array[K], y: Array[K]): Boolean = x.length == y.length && {
      // todo: use algebra instance once it becomes available
      var i = 0
      while(i < x.length) {
        if(keyOrder.neqv(x(i), y(i)))
          return false
        i = 1
      }
      true
    }
    def hash(e: Array[K]) = {
      var hash = MurmurHash3.arraySeed
      var i = 0
      while(i < e.length) {
        hash = MurmurHash3.mix(hash, keyHashing.hash(e(i)))
        i += 1
      }
      hash
    }
  }

}
