package jp.ac.tsukuba.cs.ialab.pegast.warth;

import jp.ac.tsukuba.cs.ialab.pegast.AST;
import jp.ac.tsukuba.cs.ialab.pegast.Grammar;
import jp.ac.tsukuba.cs.ialab.pegast.MemoEntry;
import jp.ac.tsukuba.cs.ialab.pegast.PackratParser;
import jp.ac.tsukuba.cs.ialab.pegast.Peg;

// 左再帰の場合、無限再帰を止めるバージョン
// 非終端記号の右辺を評価する前に、まずメモにFailを書いておく。

public class WarthTerminatingParser extends PackratParser {

    public WarthTerminatingParser(Grammar g, String input) {
        super(g, input);
    }
    @Override
    protected AST applyRule(Peg.NonTerminal r, int p) {
        MemoEntry m = memo.probe(r, p);
        if (m == null) {
            m = new MemoEntry(AST.FAIL, p);
            memo.store(r, p, m);
            m.setAns(evalBody(r));
            return m.ans();
        } else {
            setPos(m.pos());
            return m.ans();
        }
    }
}