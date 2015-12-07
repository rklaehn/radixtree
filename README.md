# intervalsets

Efficient immutable interval sets for [spire](https://github.com/non/spire).

Two of these data structures have since been included into [spire-extras](https://github.com/non/spire/tree/master/extras/src/main/scala/spire/math/extras/interval).

[![Build Status](https://travis-ci.org/rklaehn/intervalset.png)](https://travis-ci.org/rklaehn/intervalset)
[![codecov.io](http://codecov.io/github/rklaehn/intervalset/coverage.svg?branch=master)](http://codecov.io/github/rklaehn/intervalset?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.rklaehn/intervalset_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.rklaehn/intervalset_2.11)

This repository contains two data structures for sets of *non-overlapping* intervals. Boundaries are either inclusive or exclusive, so (0..2] is different to [1..2]. 

## IntervalTrie

[IntervalTrie](IntervalTrie.md) is based on a binary TRIE and requires the element type to be convertable to a long while preserving order. This is the case for all primitive types.

## IntervalSeq

[IntervalSeq](IntervalSeq.md) is based on sorted sequences of boundary values and works for any element type for which an Order instance is available.

## IntervalMap

This is the most generic data structure. It requires an Order[K] for the keys, and something like a Bool[V] for the values.

## QuickArrayMerge

An utility to merge two sorted arrays with a close to optimal number of comparisons
