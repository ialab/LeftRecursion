package jp.ac.tsukuba.cs.ialab.pegast.warth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jp.ac.tsukuba.cs.ialab.pegast.AST;
import jp.ac.tsukuba.cs.ialab.pegast.Grammar;
import jp.ac.tsukuba.cs.ialab.pegast.MemoEntry;
import jp.ac.tsukuba.cs.ialab.pegast.PackratParser;
import jp.ac.tsukuba.cs.ialab.pegast.Peg;

// 間接左再帰も扱うフルバージョン。いちおう動いた。
public class WarthParser extends PackratParser {
 
    private static class Head {
        Peg.NonTerminal rule;
        Set<Peg.NonTerminal> involvedSet, evalSet;
        Head(final Peg.NonTerminal nt, final Set<Peg.NonTerminal> involvedSet, final Set<Peg.NonTerminal> evalSet) {
            this.rule = nt;
            this.involvedSet = involvedSet;
            this.evalSet = evalSet;
        }
    }

    private static class LR extends AST {
        AST seed;
        Peg.NonTerminal rule;
        Head head;
        LR next;

        LR(final AST seed, final Peg.NonTerminal rule, final Head head, final LR next) {
            super("LR");
            this.seed = seed;
            this.rule = rule;
            this.head = head;
            this.next = next;
        }
    }

    private Map<Integer, Head> heads;

    private LR lrStack;

    public WarthParser(final Grammar g, final String input) {
        super(g, input);
    }

    @Override
    public AST parse() {
        heads = new HashMap<Integer, Head>();
        lrStack = null;
        return super.parse();
    }

    // ntに関する位置pでの左再帰の結果を「育てて」いく。
    // mに、最後の結果が入っているのを前提に、もう１回同じntのマッチをpから試す。
    // より長いマッチが得られたら、さらに繰り返す。
    private AST growLR(final Peg.NonTerminal nt, final int p, final MemoEntry m, final Head h) {
        heads.put(p, h);
        while (true) {
            setPos(p);
            h.evalSet = new HashSet<Peg.NonTerminal>(h.involvedSet);
            final AST ans = evalBody(nt);
            if (ans == AST.FAIL || pos() <= m.pos()) {
                break;
            }
            m.setAns(ans);
            m.setPos(pos());
        }
        heads.remove(p);
        setPos(m.pos());
        return m.ans();
    }

    @Override
    protected AST applyRule(final Peg.NonTerminal r, final int p) {
        MemoEntry m = recall(r, p);
        if (m == null) {
            // Create a new LR and push it onto the rule
            // invocation stack.
            final LR lr = new LR(AST.FAIL, r, null, lrStack);
            lrStack = lr;
            // Memoize lr, then evaluate nt.
            m = new MemoEntry(lr, p);
            memo.store(r, p, m);
            final AST ans = evalBody(r);
            // Pop lr off the rule invocation stack.
            lrStack = lrStack.next;
            m.setPos(pos());
            if (lr.head != null) {
                // このntをヘッドとする左再帰が起きた。
                lr.seed = ans;
                return lrAnswer(r, p, m);
            } else {
                m.setAns(ans);
                return ans;
            }
        } else {
            setPos(m.pos());
            if (m.ans() instanceof LR) {
                final LR m_ans = (LR) m.ans();
                setupLR(r, m_ans);
                return (m_ans.seed);
            } else {
                return m.ans();
            }
        }
    }

    private void setupLR(final Peg.NonTerminal nt, final LR l) {
        if (l.head == null) {
            l.head = new Head(nt, new HashSet<Peg.NonTerminal>(), new HashSet<Peg.NonTerminal>());
        }
        for (LR s = lrStack; s.head != l.head; s = s.next) {
            s.head = l.head;
            l.head.involvedSet.add(s.rule);
        }
    }

    private AST lrAnswer(final Peg.NonTerminal r, final int p, final MemoEntry m) {
        final Head h = ((LR) m.ans()).head;
        if (h.rule != r) {
            return ((LR) m.ans()).seed;
        } else {
            m.setAns(((LR) m.ans()).seed);
            if (m.ans() == AST.FAIL) {
                return AST.FAIL;
            } else {
                return growLR(r, p, m, h);
            }
        }
    }

    private MemoEntry recall(final Peg.NonTerminal r, final int p) {
        final MemoEntry m = memo.probe(r, p);
        final Head h = heads.get(p);
        // If not growing a seed parse, just return what is stored
        // in the memo table.
        if (h == null) {
            return m;
        }
        // Do not evaluate any rule that is not involved in this
        // left recursion.
        if (m == null && r != h.rule && !h.involvedSet.contains(r)) {
            return new MemoEntry(AST.FAIL, p);
        }
        // Allow involved rules to be evaluated, but only once,
        // during a seed-growing iteration.
        if (h.evalSet.contains(r)) {
            h.evalSet.remove(r);
            final AST ans = evalBody(r);
            m.setAns(ans);
            m.setPos(pos());
        }
        return m;
    }
}