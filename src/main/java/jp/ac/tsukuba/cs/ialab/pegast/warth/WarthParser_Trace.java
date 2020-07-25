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
public class WarthParser_Trace extends PackratParser {

    private static class Head {
        Peg.NonTerminal rule;
        Set<Peg.NonTerminal> involvedSet, evalSet;
        Head(Peg.NonTerminal nt, Set<Peg.NonTerminal> involvedSet, Set<Peg.NonTerminal> evalSet) {
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
        LR(AST seed, Peg.NonTerminal rule, Head head, LR next) {
            super("LR");
            this.seed = seed;
            this.rule = rule;
            this.head = head;
            this.next = next;
        }
    }
    private Map<Integer, Head> heads;

    private LR lrStack;
    public WarthParser_Trace(Grammar g, String input) {
        super(g, input);
    }
    @Override
    public AST parse() {
        heads = new HashMap<Integer,Head>();
        lrStack = null;
        return super.parse();
    }
    private void printf(String format, Object...args) {
        System.out.printf(format, args);
    }

    // ntに関する位置pでの左再帰の結果を「育てて」いく。
    // mに、最後の結果が入っているのを前提に、もう１回同じntのマッチをpから試す。
    // より長いマッチが得られたら、さらに繰り返す。
    private AST growLR(Peg.NonTerminal nt, int p, MemoEntry m, Head h) {
        printf("<GROW LR:%s>%n", nt.name());
        while (true) {
            setPos(p);
            h.evalSet = new HashSet<Peg.NonTerminal>(h.involvedSet);
            AST ans = evalBody(nt);
            printf("<eval:%s> = %s%n", nt.name(), ans);
            if (ans == AST.FAIL || pos() <= m.pos()) {
                break;
            }
            m.setAns(ans);
            m.setPos(pos());
        }
        heads.remove(p);
        setPos(m.pos());
        printf("</GROW LR:%s = %s>%n", nt.name(), m.ans());
        return m.ans();
    }
    protected AST applyRule(Peg.NonTerminal r, int p) {
        printf("<APPLY RULE:%s, %d>%n", r.name(), pos());
        MemoEntry m = recall(r, p);
        if (m == null) {
            // Create a new LR and push it onto the rule 
            // invocation stack.
            LR lr = new LR(AST.FAIL, r, null, lrStack);
            lrStack = lr;
            printlrStack();
                        // Memoize lr, then evaluate nt.
            m = new MemoEntry(lr, p);
            memo.store(r, p, m);
            AST ans = evalBody(r);
            printf("eval(%s.body) = %s\n", r.name(), ans);
            // Pop lr off the rule invocation stack.
            lrStack = lrStack.next;
            printf("LRStack pop %s\n", r.name());
            printlrStack();
            m.setPos(pos());
            if (lr.head != null) {
                // このntをヘッドとする左再帰が起きた。
                lr.seed = ans;
                AST tmp_ans = lrAnswer(r, p, m);
                printf("</APPLY RULE:%s, %d = (%s, %d)>%n", r.name(), p, tmp_ans, pos());
                return tmp_ans;
            } else {
                m.setAns(ans);
                printf("</APPLY RULE:%s, %d = (%s, %d)>%n", r.name(), p, ans, pos());
                return ans;            
            }
        } else {
            setPos(m.pos());
            if (m.ans() instanceof LR) {
                LR m_ans = (LR)m.ans();
                setupLR(r, m_ans);
                printlrStack();
                printf("</APPLY RULE:%s, %d = (%s, %d)>%n", r.name(), p, ((LR)m.ans()).seed, pos());
                return (m_ans.seed);
            } else {
                printf("</APPLY RULE:%s, %d = (%s, %d)>%n", r.name(), p, m.ans(), pos());
                return m.ans();
            }
        }
    }
    private void printlrStack() {
        printf("<print LRStack>%n");
        int i = 0;
        for (LR s = lrStack; s != null; s = s.next) {
            printf("\tLR%d(seed: %s, rule:%s, %s)%n", i++, s.seed, s.rule, s.head != null ? s.head.rule : "NULL");
        }
        printf("</print LRStack>%n");
    }
    private void print_MEMO(){
        printf("<print MEMO>%n");
        for (Peg.NonTerminal nt : memo.nonTerminals()) {
            for (int pos: memo.positions(nt)) {
                MemoEntry m = memo.probe(nt, pos);
                printf("(%s, %d) = ", nt, pos);
                if (m.ans() instanceof LR) {
                    printf("M(LR(%s))%n", ((LR)m.ans()).seed);
                } else {
                    printf("M(AST(%s))%n ", m.ans());
                }
            }
        }
        printf("</print MEMO>%n");
    }
    
    private void setupLR(Peg.NonTerminal nt, LR l) {
        printf("<SETUP LR:%s>%n", nt.name());
        if (l == null) {
            printf("error:lr is null%n");
            throw new RuntimeException();
        }
        // lはlrStack内にある。
        // l.ruleはntと同じ名前のはず。(ntのメモにlが入っていたので。)
        if (l.head == null) {
            l.head = new Head(nt, new HashSet<Peg.NonTerminal>(),
                                new HashSet<Peg.NonTerminal>());
        }
        // lrStack内にあって、lより浅い位置にあるエントリーのルールは、
        // この左再帰についてのinvolvedルール。(一意か？)
        if (lrStack == null) {
            printf("error:LRStack has no LR object%n");
            throw new RuntimeException();
        }
        for (LR s = lrStack; s.head != l.head; s = s.next) {
            s.head = l.head;
            l.head.involvedSet.add(s.rule);
        }
        printlrStack();
        printf("</SETUP LR:%s>%n", nt.name());
    }
    private AST lrAnswer(Peg.NonTerminal r, int p, MemoEntry m) {
        printf("<LR_ANSWER:%s>%n", r.name());
        print_MEMO();
        Head h = ((LR)m.ans()).head;
        if (h.rule != r) {
            printf("%s != %s%n", r.name(), h.rule.name());
            printf("</LR_ANSWER:%s = %s>%n", r.name(), ((LR)m.ans()).seed);
            return ((LR)m.ans()).seed;
        } else {
            printf("%s == %s%n", r.name(), h.rule.name());
            m.setAns(((LR)m.ans()).seed);
            if (m.ans() == AST.FAIL) {
                printf("</LR_ANSWER:%s = FAIL>%n", r.name());
                return AST.FAIL;
            } else {
                AST ans = growLR(r, p, m, h);
                printf("</LR_ANSWER:%s = %s>%n", r.name(), ans);
                return ans;
            }
        }
    }
    private MemoEntry recall(Peg.NonTerminal r, int p) {
        printf("<RECALL:%s>%n", r.name());
        MemoEntry m = memo.probe(r, p);
        Head h = heads.get(p);
        // If not growing a seed parse, just return what is stored 
        // in the memo table.
        if (h == null) {
            printf("no heads(p)%n");
            printf("</RECALL:%s>%n", r.name());
                return m;
        }
        // Do not evaluate any rule that is not involved in this 
        // left recursion.
        if (m == null && r != h.rule && ! h.involvedSet.contains(r)) {
            printf("memo%n");
            printf("</RECALL:%s>%n", r.name());
            return new MemoEntry(AST.FAIL, p);
        }
        // Allow involved rules to be evaluated, but only once,
        // during a seed-growing iteration.
        if (h.evalSet.contains(r)) {
            h.evalSet.remove(r);
            AST ans = evalBody(r);
            printf("<eval:%s>=%s%n", r.name(), ans);
            m.setAns(ans);
            m.setPos(pos());
        }
        printf("</RECALL:%s>%n", r.name());
        return m;
    }
}