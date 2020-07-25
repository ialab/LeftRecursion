package jp.ac.tsukuba.cs.ialab.pegast;

public class PackratParser extends PegParser {
    protected Memo<MemoEntry> memo;
    public PackratParser(Grammar g, String input) {
        super(g, input);
    }
    @Override
    public AST parse() {
        memo = new Memo<MemoEntry>();
        return super.parse();
    }

    protected AST applyRule(Peg.NonTerminal r, int p) {
        MemoEntry m = memo.probe(r, p);
        if (m == null) {
            AST ans = evalBody(r);
            memo.store(r, p, new MemoEntry(ans, p));
            return ans;
        } else {
            setPos(m.pos());
            return m.ans();
        }
    }
    @Override
    public AST visit(Peg.NonTerminal r) {
        return labelNode(r.name(), applyRule(r, pos()));
    }
}