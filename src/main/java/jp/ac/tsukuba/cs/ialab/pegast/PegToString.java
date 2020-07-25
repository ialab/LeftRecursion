package jp.ac.tsukuba.cs.ialab.pegast;

import jp.ac.tsukuba.cs.ialab.pegast.Peg.Choice;
import jp.ac.tsukuba.cs.ialab.pegast.Peg.Constant;
import jp.ac.tsukuba.cs.ialab.pegast.Peg.NonTerminal;
import jp.ac.tsukuba.cs.ialab.pegast.Peg.NotPredicate;
import jp.ac.tsukuba.cs.ialab.pegast.Peg.RegExp;
import jp.ac.tsukuba.cs.ialab.pegast.Peg.Repeat;
import jp.ac.tsukuba.cs.ialab.pegast.Peg.Sequence;

public class PegToString implements Peg.PegVisitor<String> {

    private static String join(Peg p, String separator, PegToString visitor) {
        StringBuilder sb = new StringBuilder("(");
        String sep = "";
        for (AST c : p.children()) {
            sb.append(sep);
            sb.append(((Peg) c).accept(visitor));
            sep = separator;
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String visit(Sequence s) {
        return join(s, " ", this);
    }

    @Override
    public String visit(Choice ch) {
        return join(ch, "/", this);
    }

    @Override
    public String visit(Constant c) {
        return '"' + c.string() + '"';
    }

    @Override
    public String visit(RegExp re) {
        return '/' + re.pattern().pattern() + '/';
    }

    @Override
    public String visit(NonTerminal nt) {
        return nt.name();
    }

    @Override
    public String visit(Repeat r) {
        return "(" + r.children().get(0) + ")*";
    }

    @Override
    public String visit(NotPredicate n) {
        return "!(" + n.children().get(0) + ")";
    }

    
}