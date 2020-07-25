# LeftRecursion
Demonstration code of the left-recursion capable packrat parsing algorithm presented in PRO-2020-2.
(With code for other related algorithms.)

This software is released under the MIT License, see LICENSE.md for terms and conditions.

To build, execute:

``mvn compile``

To test, execute:

``java -cp target/classes jp.ac.tsukuba.cs.ialab.pegast.LeftRecursionTest [option]``

where option is one of:
-      umeda   (default)
-      warth
-      goto
-      medeiros
