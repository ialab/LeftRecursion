package jp.ac.tsukuba.cs.ialab.pegast.medeiros;

import java.util.List;
import java.util.regex.Matcher;

import jp.ac.tsukuba.cs.ialab.pegast.AST;
import jp.ac.tsukuba.cs.ialab.pegast.Grammar;
import jp.ac.tsukuba.cs.ialab.pegast.Peg;
import jp.ac.tsukuba.cs.ialab.pegast.PegParser;
import static jp.ac.tsukuba.cs.ialab.pegast.Peg.*;

// Medeirosらの論文 LR in PEG での意味論に基づくパーサ
public class MedeirosParser extends PegParser {
   
    public MedeirosParser(Grammar g, String input) {
        super(g,input);
    }

    @Override
    public AST visit(Sequence s) {
        return conRule(s.children());
    }
    private AST conRule(List<AST> seq) {
        int backtrackPos = pos();
        if (seq.isEmpty()) { 
            return ast("empty");
        }
        AST r1 = ((Peg)seq.get(0)).accept(this);
        if (r1 == FAIL) {
            // (con.3)
            return FAIL;
        }
        AST r2 = conRule(seq.subList(1, seq.size()));
        if (r2 != FAIL) {
            // (con.1)
            return ast("Con", r1, r2);
        }
        // (con.2)
        setPos(backtrackPos);
        return FAIL;
    }

    @Override
    public AST visit(Choice ch) {
        return ordRule(ch.children());
    }
    private AST ordRule(List<AST> choices) {
        if (choices.isEmpty()) {
            return FAIL;
        }
        AST r1 = ((Peg)choices.get(0)).accept(this);
        if (r1 != FAIL) {
            // (ord.1)
            return r1;
        }
        AST r2 = ordRule(choices.subList(1, choices.size()));
        if (r2 == FAIL) {
            // (ord.2)
            return FAIL;
        }
        // (ord.3)
        return r2;
    }

    @Override
    public AST visit(Constant c) {
        String string = c.string();
        if (input().regionMatches(pos(), string, 0, string.length())) {
            // (char.1)
            setPos(pos() + string.length());
            return ast("String:\"" + string + "\"");
        }
        // (char.2) (char.3)
        return FAIL;
    }

    @Override
    public AST visit(RegExp re) {
        Matcher m = re.pattern().matcher(input());
        m.region(pos(), input().length());
        if (m.lookingAt()) {
            String matched = m.group();
            setPos(pos() + matched.length());
            return ast("REMatch:'" + matched + "'");
        } else {
            return FAIL;
        }   
    }

    @Override
    public AST visit(NonTerminal nt) {
        AST r = evalBody(nt);
        if (r == FAIL) {
            // (var.2)
            return FAIL;
        }
        // (var.1)
        return labelNode(nt.name(), r);
    }

    @Override
    public AST visit(Peg.Repeat r) {
        return repRule(r.children().get(0));
    }
    private AST repRule(AST child) {
        Peg body = (Peg)child;
        AST result = body.accept(this);
        if (result == FAIL) {
            // (rep.1)
            return ast("empty");
        } else {
            // (rep.2)
            AST restResult = body.accept(this);
            return ast("Rep", result, restResult);
        }
    }

    @Override
    public AST visit(Peg.NotPredicate n) {
        return notRule(n.children().get(0));
    }
    private AST notRule(AST child) {
        Peg body = (Peg)child;
        int backtrackPos = pos();
        AST result = body.accept(this);
        if (result == FAIL) {
            // (not.1)
            return ast("empty");
        } else {
            // (not.2)
            setPos(backtrackPos);
            return FAIL;
        }
    }
}