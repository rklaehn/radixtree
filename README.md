[![Build Status](https://travis-ci.org/rklaehn/radixtree.png)](https://travis-ci.org/rklaehn/radixtree)
[![codecov.io](http://codecov.io/github/rklaehn/radixtree/coverage.svg?branch=master)](http://codecov.io/github/rklaehn/radixtree?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.rklaehn/radixtree_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.rklaehn/radixtree_2.11)

# radixtree

A generic and fast immutable radix tree, using [algebra](https://github.com/non/algebra) and [cats](https://github.com/non/cats) typeclasses.

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

When a tree is used mostly for reading, there is also a method to pack the tree into a very compact representation using interning of keys, values and nodes.

### Space usage benchmark

Here is an example of the space usage of different string sets using the radix tree as well as standard scala collections. 

Numbers from 0 until 10000 is the textual representation of all numbers from 0 to 10000, e.g. "nine thousand, nine hundred ninety"

English words is a list of words of the english language from [here](http://www-01.sil.org/linguistics/wordlists/english/wordlist/wordsEn.txt)
```
sbt instrumentedTest/test:run

...
[info] Numbers from 0 until 10000:
[info] 	Elements:           1398888
[info] 	HashSet:            1746600
[info] 	SortedSet:          1678952
[info] 	RadixTree:          1229904
[info] 	RadixTree (packed): 5648
[info] English words:
[info] 	Elements:           9644728
[info] 	HashSet:            13833088
[info] 	SortedSet:          12713112
[info] 	RadixTree:          11527264
[info] 	RadixTree (packed): 2098712
```

The radix tree is much more efficient regarding space usage than the standard scala collections. It consumes just slightly more space than the elements. When packing the radix tree, it consumes **extremely** little space. Less than the list of words itself!

## Optimized operations

### Merge

A merge of disjoint or mostly disjoint radix trees can be very fast. E.g. if you have a RadixTree with all english words
starting with a, and one with all english words starting with b, merging will be O(1) regardless of the size of the 
subtrees, and will use structural sharing.

### Prepend

Prepending a prefix to all keys in a radix tree is an O(1) operation with a RadixTree, whereas it would be at least O(n log(n)) with a SortedMap or HashMap

### FilterPrefix

Filtering by prefix is *extremely fast* with a radix tree (worst case O(log(N)), whereas it is worse than O(N) with
SortedMap and HashMap. Filtering by prefix will also benefit a lot from structural sharing.

### Lookup

### Contains

Filters keys containing a substring
