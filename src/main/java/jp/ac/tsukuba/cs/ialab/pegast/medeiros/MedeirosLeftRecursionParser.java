package jp.ac.tsukuba.cs.ialab.pegast.medeiros;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jp.ac.tsukuba.cs.ialab.pegast.AST;
import jp.ac.tsukuba.cs.ialab.pegast.Grammar;
import jp.ac.tsukuba.cs.ialab.pegast.MemoEntry;
import jp.ac.tsukuba.cs.ialab.pegast.Peg;

// MedeirosらのLR in PEG論文で、左再帰を扱える版のアルゴリズム(Figure 2)
public class MedeirosLeftRecursionParser extends MedeirosParser {
    private Map<Peg.NonTerminal,Map<Integer,LinkedList<MemoEntry>>> memo;  // \mathcal{L}
    public MedeirosLeftRecursionParser(Grammar g, String input) {
		super(g, input);
		memo = new HashMap<Peg.NonTerminal,Map<Integer,LinkedList<MemoEntry>>>();
    }

    // (nt, pos)に対するメモエントリ (スタック)が必ず存在することを保証する
    private LinkedList<MemoEntry> getStack(Peg.NonTerminal nt, int pos) {
        Map<Integer,LinkedList<MemoEntry>> map = memo.get(nt);
        LinkedList<MemoEntry> stack;
        if (map == null) {
            map = new HashMap<Integer,LinkedList<MemoEntry>>();
            memo.put(nt, map);
            stack = new LinkedList<MemoEntry>();
            map.put(pos, stack);
        } else {
            stack = map.get(pos);
            if (stack == null) {
                stack = new LinkedList<MemoEntry>();
                map.put(pos, stack);
            }
        }
        return stack;
    }
    private MemoEntry probeMemo(Peg.NonTerminal nt, int pos) {
        LinkedList<MemoEntry> stack = getStack(nt, pos);
        if (stack.isEmpty()) {
            return null;
        } else {
            return stack.peek();
        }
    }
    private void pushMemo(Peg.NonTerminal nt, int pos, int endPos, AST r) {
        LinkedList<MemoEntry> stack = getStack(nt, pos);
        stack.push(new MemoEntry(r, endPos));
    }
    private void popMemo(Peg.NonTerminal nt, int pos) {
        LinkedList<MemoEntry> stack = getStack(nt, pos);
        stack.pop();
    }
    private MemoEntry increaseBound(Peg.NonTerminal a, int pos) {
        MemoEntry m = probeMemo(a, pos);
        setPos(pos);
        AST r = evalBody(a);   // 前件のPEG関係を試す
        if (r == AST.FAIL) {
            // (inc.2)
            return m;
        }
        if (pos() <= m.pos()) {
            // (inc.3)
            return m;
        }
        // (inc.1)
        pushMemo(a, pos, pos(), r);
        MemoEntry nextInc = increaseBound(a, pos);
        popMemo(a, pos);
        return nextInc;
    }
    public AST visit(Peg.NonTerminal a) {
        int startPos = pos();
        MemoEntry m = probeMemo(a, pos());
        if (m == null) {
            pushMemo(a, startPos, startPos, AST.FAIL);
            AST seed = evalBody(a);
            popMemo(a, startPos);
            if (seed != AST.FAIL) {
                // (lvar.1)
                pushMemo(a, startPos, pos(), seed);
                MemoEntry r = increaseBound(a, startPos);
                popMemo(a, startPos);
                setPos(r.pos());
                return labelNode(a.name(), r.ans());
            } else {
                // (lvar.2)
                return AST.FAIL;
            }
        } else if (m.ans() == AST.FAIL) {
            // (lvar.3)
            return AST.FAIL;
        } else {
            // (lvar.4)
            setPos(m.pos());
            return labelNode(a.name(), m.ans());
        }
    }
}