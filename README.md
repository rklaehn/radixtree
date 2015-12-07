# radixtree

A generic and fast immutable radix tree for [spire](https://github.com/non/spire).

[![Build Status](https://travis-ci.org/rklaehn/radixtree.png)](https://travis-ci.org/rklaehn/radixtree)
[![codecov.io](http://codecov.io/github/rklaehn/radixtree/coverage.svg?branch=master)](http://codecov.io/github/rklaehn/radixtree?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.rklaehn/radixtree_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.rklaehn/radixtree_2.11)

This is an immutable generic radix tree. It works for both immutable objects which override equals and hashcode, and
objects that *do not* override equals and hashcode, such as raw byte arrays.

## Internal representation

A radix tree node consists of a prefix of type K, an optional value of type V, and a (possibly empty) array of children.
The internal representation is very compact, since it is using a flat array for the children of a radix tree node. But
this puts a number of constraints on the key type if maximum efficiency is desired: The key type has to be some kind of
sequence, and the element type of the sequence should not have too many distinct values.

E.g. when using String as key type, Char is the element type. Theoretically, it has 2^16 possible values. But
in real world strings it is very rare to find a radix tree node of type string with more than 60 children.

When using Array of Byte as the key type, Byte is the element type. So there are a maximum of 256 children in each
RadixTree node, which is still OK.

Using a sequence of Longs as key type on the other hand might be less than optimal from a performance POV when creating
trees.

## Space usage

A big advantage of a radix tree over a sorted map or hash map is that keys are not stored completely. So when storing
a large number of long strings such as urls, a radix tree will have a space advantage over a HashMap or SortedMap. To
optimise space usage even more, an interning scheme for keys can be used.

## Optimized operations

### Merge

A merge of disjoint or mostly disjoint radix trees can be very fast. E.g. if you have a RadixTree with all english words
starting with a, and one with all english words starting with b, merging will be O(1) regardless of the size of the 
subtrees, and will use structural sharing.

### FilterPrefix

Filtering by prefix is *extremely fast* with a radix tree (worst case O(log(N)), whereas it is worse than O(N) with
SortedMap and HashMap. Filtering by prefix will also benefit a lot from structural sharing.

### Lookup

Successful lookup is O(k), where k is the number of elements in the key. Hashtable lookup is O(k) as well. Even if the
hashcode matches, the key to be looked up has to be compared with the key in the hash map, which is O(k).

### Contains

Filters keys containing a substring
