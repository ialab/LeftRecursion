package jp.ac.tsukuba.cs.ialab.pegast;

import static jp.ac.tsukuba.cs.ialab.pegast.AST.FAIL;
import static jp.ac.tsukuba.cs.ialab.pegast.AST.ast;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Pegに対する操作を定義するVisitorの一つで、構文解析操作を定義するもの。 Pegの各構文ごとにvisitメソッドを用意する。
 * 
 * コンストラクタに、Grammarとinputを与える。これはずっと固定。
 * parse()メソッドを呼び出すと、inputについてGrammarの開始シンボルから構文解析を開始する。
 * posフィールドはparse()の最初に0にセットされ、visitメソッド内で、構文解析が進むにつれた増えていく。
 * SequenceやChoideで子供を構文解析する際には、子供のacceptに自分自身を与える。
 * 
 * visitorから戻ったとき、成功したらposは(一般に)増えている。Failしたら呼び出し時のまま。
 * だから、Choiceでは開始時のposをバックトラック用に覚えておく必要はない。
 * Sequenceの開始時のposは覚えておいて、途中で失敗したら戻してreturnする必要がある。
 */
public class PegParser implements Peg.PegVisitor<AST> {
    final private Grammar g;
    final private String input;
    private int pos;

    public PegParser(Grammar g, String input) {
        this.g = g;
        this.input = input;
    }

    protected Grammar grammar() {
        return g;
    }

    protected String input() {
        return input;
    }

    protected int pos() {
        return pos;
    }

    protected void setPos(int newPos) {
        pos = newPos;
    }

    protected void addPos(int increment) {
        pos += increment;
    }

    public AST parse() {
        pos = 0;
        return g.start().accept(this);
    }

    final String listMarker = "List";

    protected AST list(List<AST> children) {
        if (children.size() == 1) {
            return children.get(0);
        } else {
            return ast(listMarker, children);
        }
    }

    protected void mergeList(List<AST> children, AST child) {
        if (child.label() == listMarker) {
            children.addAll(child.children());
        } else {
            children.add(child);
        }
    }

    @Override
    public AST visit(Peg.Sequence s) {
        int backTrackPos = pos;
        List<AST> astList = new LinkedList<AST>();
        for (AST child : s.children()) {
            Peg c = (Peg) child;
            AST r = c.accept(this);
            if (r == AST.FAIL) {
                pos = backTrackPos;
                return AST.FAIL;
            }
            mergeList(astList, r);
        }
        return list(astList);
    }

    @Override
    public AST visit(Peg.Choice ch) {
        for (AST child : ch.children()) {
            Peg c = (Peg) child;
            AST r = c.accept(this);
            if (r != AST.FAIL) {
                return r;
            }
        }
        return FAIL;
    }

    final protected AST evalBody(Peg.NonTerminal nt) {
        return grammar().getBody(nt).accept(this);
    }

    final protected AST labelNode(String label, AST body) {
        // mergeListを用いて、余計なネストを最小限に整形する。
        if (body == FAIL)
            return body;
        List<AST> bodyAST = new LinkedList<AST>();
        mergeList(bodyAST, body);
        return ast(label, bodyAST);
    }

    @Override
    public AST visit(Peg.NonTerminal nt) {
        return labelNode(nt.name(), evalBody(nt));
    }

    @Override
    public AST visit(Peg.Constant c) {
        String string = c.string();
        if (input.regionMatches(pos, string, 0, string.length())) {
            pos += string.length();
            return ast("String:\"" + string + "\"");
        }
        return FAIL;
    }

    @Override
    public AST visit(Peg.RegExp re) {
        Matcher m = re.pattern().matcher(input);
        m.region(pos, input.length());
        if (m.lookingAt()) {
            String matched = m.group();
            pos += matched.length();
            return ast("REMatch:'" + matched + "'");
        } else {
            return FAIL;
        }
    }

    @Override
    public AST visit(Peg.Repeat s) {
        Peg child = (Peg) s.children().get(0);
        LinkedList<AST> result = new LinkedList<AST>();
        while (true) {
            AST r = child.accept(this);
            if (r == AST.FAIL) {
                break;
            }
            result.add(r);
        }
        return list(result);
    }

    @Override
    public AST visit(Peg.NotPredicate n) {
        int pos = pos();
        if (n.accept(this) == AST.FAIL) {
            return list(new LinkedList<AST>());
        } else {
            setPos(pos);
            return AST.FAIL;
        }
    }
}