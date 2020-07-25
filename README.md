# LeftRecursion
Demonstration code of the left-recursion capable packrat parsing algorithm presented in PRO-2020-2.
(With code for other related algorithms.)

This software is released under the MIT License, see the file LICENSE for terms and conditions.

You can just open the LeftRecursion directory with VS Code and launch
``Debug (Launch)-LRTest`` target.

Or, build using Maven by:

``mvn compile``

and execute:

``java -cp target/classes jp.ac.tsukuba.cs.ialab.pegast.LeftRecursionTest [option]``

where option is one of:
- umeda   (default)
- warth
- goto
- medeiros
