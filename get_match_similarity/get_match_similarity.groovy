@Grab(group='fun.mike', module='diff-match-patch', version='0.0.2')
// @Grab(g:fun.mike AND a:diff-match-patch AND v:0.0.2)

import fun.mike.dmp.Diff
import fun.mike.dmp.DiffMatchPatch

// String text1 = "ABCDELMN"
// String text2 = "ABCFGLMN"

String text1 = "Strongly agree."
String text2 = "Strongly disagree."

// String text1 = "<t0/>(0.38*2540) ÷ 1280 ≈ 0.75 <t1/>"
// String text2 = "<t0/>(0.38*2540) ÷ 1280 ≈ 0.75 <t1/> "

DiffMatchPatch dmp = new DiffMatchPatch()
LinkedList<Diff> diffs = dmp.diff_main(text1, text2)
// System.out.println(diffs);
// console.println(diffs)

int index = dmp.diff_levenshtein(diffs)
double dsimilarity = 100 - ((double)index / Math.max(text1.length(), text2.length()) * 100)
console.println("Similarity metric: " + dsimilarity.round(2))

// -------------------------------------
// @Grab(group='org.bitbucket.cowwoc.diff-match-patch', module='diff-match-patch', version='1.0')
// https://stackoverflow.com/a/58418246/2095577
// https://www.baeldung.com/java-difference-between-two-strings

// library:
// https://github.com/google/diff-match-patch
// https://opensource.google/projects/diff-match-patch
