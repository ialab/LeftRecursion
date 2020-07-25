package jp.ac.tsukuba.cs.ialab.pegast.umeda;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jp.ac.tsukuba.cs.ialab.pegast.AST;
import jp.ac.tsukuba.cs.ialab.pegast.Grammar;
import jp.ac.tsukuba.cs.ialab.pegast.Memo;
import jp.ac.tsukuba.cs.ialab.pegast.MemoEntry;
import jp.ac.tsukuba.cs.ialab.pegast.Peg;
import jp.ac.tsukuba.cs.ialab.pegast.PegParser;

public class UmedaParser extends PegParser {

    public static class Entry extends MemoEntry {
        boolean grow;
        Entry(AST ans, int pos, boolean grow) {
            super(ans, pos);
            this.grow = grow;
        }
        public boolean grow() { return grow; }
        public void setGrow(boolean b) { grow = b; }
    }
    final private Memo<Entry> memo;
    public UmedaParser(Grammar g, String input) {
        super(g, input);
        memo = new Memo<Entry>();
    }
    @Override
    public AST parse() {
        setPos(0);
        return applyRule(grammar().start(), 0);
    }

    private AST applyRule(Peg.NonTerminal n, int p) {
        Entry m = memo.probe(n, p);
        if (m == null) {
            m = new Entry(AST.FAIL, p, false);
            memo.store(n, p, m);
            AST ans = evalBody(n);
            m.setAns(ans);
            m.setPos(pos());
            if (m.grow()) {
                growLR(n, p);
                m.setGrow(false);
                ans = m.ans();
                setPos(m.pos());
            }
            return ans;
        } else if (m.ans() == AST.FAIL) {
            m.setAns(AST.FAIL);
            m.setGrow(true);
            return AST.FAIL;
        } else {
            setPos(m.pos());
            return m.ans();
        }
    }

    private void growLR(Peg.NonTerminal n, int p) {
        while (true) {
            int oldPos = pos();
            setPos(p);
            Set<Peg.NonTerminal> set = new HashSet<Peg.NonTerminal>();
            set.add(n);
            AST ans = evalGrow(grammar().getBody(n), p, set);
            if (ans == AST.FAIL || pos() <= oldPos) {
                break;
            }
            Entry m = memo.probe(n, p);
            m.setAns(ans);
            m.setPos(pos());
        }
    }
    private AST applyRuleGrow(Peg.NonTerminal n, int p, Set<Peg.NonTerminal> limits) {
        limits.add(n);
        Peg exp = grammar().getBody(n);
        AST ans = evalGrow(exp, p, limits);
        Entry m = memo.probe(n, p);
        if (ans == AST.FAIL || pos() <= m.pos()) {
            ans = m.ans();
            setPos(m.pos());
        } else {
            m.setAns(ans);
            m.setPos(pos());
        }
        return ans;
    }
    AST evalGrow(Peg e, int p, Set<Peg.NonTerminal> limits) {
        if (e instanceof Peg.NonTerminal) {
            Peg.NonTerminal n = (Peg.NonTerminal)e;
            AST ans;
            if (pos() == p && ! limits.contains(n)) {
                ans = applyRuleGrow(n, pos(), limits);
            } else {
                ans = applyRule(n, pos());
            }
            return labelNode(n.name(), ans);
        } else if (e instanceof Peg.Choice) {
            for (AST c: e.children()) {
                AST ans = evalGrow((Peg)c, p, limits);
                if (ans != AST.FAIL) {
                    return ans;
                }
            }
            return AST.FAIL;
        } else if (e instanceof Peg.Sequence) {
            List<AST> astList = new LinkedList<AST>();
            for (AST c: e.children()) {
                AST ans = evalGrow((Peg)c, p, limits);
                if (ans == AST.FAIL) {
                    setPos(p);
                    return AST.FAIL;
                }
                mergeList(astList, ans);
            }
            return list(astList);
        } else {
            // Constant, RegExp
            return e.accept(this);
        }
    }
    @Override
    public AST visit(Peg.NonTerminal n) {
        return labelNode(n.name(), applyRule(n, pos()));
    }
}