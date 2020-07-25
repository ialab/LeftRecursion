package jp.ac.tsukuba.cs.ialab.pegast;

import java.util.LinkedList;

// 後藤らのアルゴリズム。オリジナルに近いバージョン。
public class GotoParserOriginal extends PegParser {
    // 元の論文でわからないところ
    // 図9: applyRule terminalってなんだ？
    // 図10: updateMemo ruleはRの間違い?
    // 図11: growLR oldPosはグローバル？ローカル？
    public GotoParserOriginal(Grammar g, String input) {
        super(g, input);
    }
    private LinkedList<Peg.NonTerminal> call;
    private int maxPos;
    private Memo<MemoEntry> memo;
    private boolean growState;
    @Override
    public AST parse() {
        call = new LinkedList<Peg.NonTerminal>();
        maxPos = 0;
        memo = new Memo<MemoEntry>();
        growState = false;
        return super.parse();
    }
    @Override
    public AST visit(Peg.NonTerminal r) {
        return labelNode(r.name(), applyRule(r, pos()));
    }

    private AST applyRule(Peg.NonTerminal n, int p) {
        MemoEntry m = memo.probe(n, p);
        if (m != null && call.contains(n)) {
            setPos(m.pos());
            return m.ans();
        } else {
            return updateMemo(n, p);
        }
    }
    private AST updateMemo(Peg.NonTerminal n, int p) {
        call.push(n);
        MemoEntry m = memo.probe(n, p);
        if (m == null) {
            m = new MemoEntry(AST.FAIL, p);
            memo.store(n, p, m);
        }
        AST ans = evalBody(n);
        if (ans != AST.FAIL) {
            m.setPos(pos());
        }
        if (maxPos < m.pos()) {
            maxPos = m.pos();
        }
        m.setAns(ans);
        call.pop();
        if (growState == false && n == grammar().start()) {
            while (true) {
                ans = growLR(n, p, m);
                if (ans == AST.FAIL || pos() >= input().length()) {
                    break;
                }
            }
        }
        return ans;
    }
    private AST growLR(Peg.NonTerminal n, int p, MemoEntry m) {
        call.push(n);
        growState = true;
        int oldPos = maxPos;
        setPos(p);
        AST ans = evalBody(n);
        if (ans != AST.FAIL && pos() > m.pos()) {
            memo.store(n, p, new MemoEntry(ans, pos()));
        }
        growState = false;
        call.pop();
        if (oldPos != maxPos) {
            return ans;
        } else {
            // setPos(p);
            return AST.FAIL;
        }
    }
}