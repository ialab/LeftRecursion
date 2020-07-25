package jp.ac.tsukuba.cs.ialab.pegast.warth;

import jp.ac.tsukuba.cs.ialab.pegast.AST;
import jp.ac.tsukuba.cs.ialab.pegast.Grammar;
import jp.ac.tsukuba.cs.ialab.pegast.MemoEntry;
import jp.ac.tsukuba.cs.ialab.pegast.PackratParser;
import jp.ac.tsukuba.cs.ialab.pegast.Peg;

// 直接左再帰を扱えるバージョン
// growLRで、posが進む限り繰り返し呼び出す

public class WarthDirectOnly extends PackratParser {

    public WarthDirectOnly(Grammar g, String input) {
        super(g, input);
    }

    // ntに関する位置pでの左再帰の結果を「育てて」いく。
    // mに、最後の結果が入っているのを前提に、もう１回同じntのマッチをpから試す。
    // より長いマッチが得られたら、さらに繰り返す。
    private AST growLR(Peg.NonTerminal r, int p, MemoEntry m) {
        while (true) {
            setPos(p);
            AST ans = evalBody(r);
            if (ans == AST.FAIL || pos() <= m.pos()) {
                break;
            }
            m.setAns(ans);
            m.setPos(pos());
        }
        setPos(m.pos());
        return m.ans();
    }

    private static class LR extends AST {
        boolean detected;
        LR(boolean detected) {
            super("LR");
            this.detected = detected;
        }
    }

    @Deprecated
    protected AST applyRule(Peg.NonTerminal r, int p) {
        MemoEntry m = memo.probe(r, p);
        if (m == null) {
            LR lr = new LR(false);
            m = new MemoEntry(lr, p);
            memo.store(r, p, m);
            AST ans = evalBody(r);
            m.setAns(ans);
            m.setPos(pos());
            if (lr.detected && ans != AST.FAIL) {
                return growLR(r, p, m);
            } else {
                return ans;
            }
        } else {
            setPos(m.pos());
            if (m.ans() instanceof LR) {
                ((LR)m.ans()).detected = true;
                return AST.FAIL;
            } else {
                return m.ans();
            }
        }
    }
}