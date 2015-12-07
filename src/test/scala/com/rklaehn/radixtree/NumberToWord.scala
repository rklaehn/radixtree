package com.rklaehn.radixtree

/**
 * Helper object to generate realistic test cases for the radix tree. Used with permission from www.source-code.biz
 */
object NumberToWord {

  def apply(n: Int): String = convertNumberToWords(n)

  // This snippet may be used freely, as long as the authorship note remains in the source code.
  private val lowNames = Array("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
  private val tensNames = Array("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
  private val bigNames = Array("thousand", "million", "billion")

  /**
   * Converts an integer number into words (american english).
   * @author Christian d'Heureuse, Inventec Informatik AG, Switzerland, www.source-code.biz
   */
  private def convertNumberToWords(n0: Int): String = {
    var n = n0
    if (n < 0) "minus " + convertNumberToWords(-n)
    else if (n <= 999) convert999(n)
    else {
      var s: String = null
      var t = 0
      while (n > 0) {
        if (n % 1000 != 0) {
          var s2 = convert999(n % 1000)
          if (t > 0) {
            s2 = s2 + " " + bigNames(t - 1)
          }
          if (s == null) {
            s = s2
          } else {
            s = s2 + ", " + s
          }
        }
        n /= 1000
        t += 1
      }
      s
    }
  }

  private def convert999(n: Int): String = {
    val s1 = lowNames(n / 100) + " hundred"
    val s2 = convert99(n % 100)
    if (n <= 99) s2
    else if (n % 100 == 0) s1
    else s1 + " " + s2
  }

  private def convert99(n: Int): String = {
    if (n < 20) lowNames(n)
    else {
      val s = tensNames(n / 10 - 2)
      if (n % 10 == 0) s
      else s + "-" + lowNames(n % 10)
    }
  }
}
