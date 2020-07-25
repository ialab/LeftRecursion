package jp.ac.tsukuba.cs.ialab.pegast;

import static jp.ac.tsukuba.cs.ialab.pegast.Peg.*;

import java.util.HashMap;
import java.util.Map;

import jp.ac.tsukuba.cs.ialab.pegast.goTo.GotoParser;
import jp.ac.tsukuba.cs.ialab.pegast.medeiros.MedeirosLeftRecursionParser;
import jp.ac.tsukuba.cs.ialab.pegast.umeda.UmedaParser;
import jp.ac.tsukuba.cs.ialab.pegast.warth.WarthParser;

public class LeftRecursionTest {
    private interface ParserFactory {
        public PegParser create(Grammar g, String input);
    }
    private static void printGrammar(Grammar g) {
        System.out.println("== Rules ==");
        for (Peg.NonTerminal nt: g.nonTerminals()) {
            System.out.println(nt + " <- " + g.getBody(nt));
        }
        System.out.println("Start symbol: " + g.start());
    }
    private static void testGrammar(Grammar g, String input, ParserFactory factory) {
        printGrammar(g);
        System.out.println("== Input ==");
        System.out.println(input);
        PegParser pv = factory.create(g, input);
        long start = System.nanoTime();
        try {
            AST r = pv.parse();
            System.out.println("== Result ==");
            if (r != AST.FAIL) {
                r.prettyPrint();
            } else {
                System.out.println("Failed.");
            }
        } catch (Exception e) {
            System.out.println("Exception caught during parsing: " + e);
           // e.printStackTrace(System.out);
        }
        long end = System.nanoTime();
        System.out.println("Time: " + ((double)(end - start)/1e6) + "ms");
    }
    private static class TestData {
        String description;   // このテストの説明
        Grammar g;
        String input;
        TestData(String description, Grammar g, String input) { this.description = description; this.g = g; this.input = input; }
    }
    public static void main( String[] args )
    {   
        Grammar javaPrimary = (new Grammar())
        .addRule(nt("Primary"),nt("PrimaryNoNewArray"))
        .addRule(nt("PrimaryNoNewArray"),
            choice(nt("ClassInstanceCreationExpression"),
                nt("MethodInvocation"),
                nt("FieldAccess"),
                nt("ArrayAccess"),
                constant("this")))
        .addRule(nt("ClassInstanceCreationExpression"),
            choice(seq(constant("new "),nt("ClassOrInterfaceType"), constant("()")),
                        seq(nt("Primary"), constant(".new "), nt("Identifier"), constant("()") )))
        .addRule(nt("MethodInvocation"),
            choice(seq(nt("Primary"), constant("."), nt("Identifier"), constant("()")),
                seq(nt("MethodName"), constant("()"))))
        .addRule(nt("FieldAccess"),
            choice(seq(nt("Primary"), constant("."), nt("Identifier")),
                seq(constant("super."), nt("Identifier"))))
        .addRule(nt("ArrayAccess"),
            choice(seq(nt("Primary"), constant("["), nt("Expression"), constant("]") ),
                seq(nt("ExpressionName"), constant("["), nt("Expression"), constant("]"))))
        .addRule(nt("ClassOrInterfaceType"),
            choice(nt("ClassName"), nt("InterfaceTypeName")))
        .addRule(nt("ClassName"),
            choice(constant("C"), constant("D") ))
        .addRule(nt("InterfaceTypeName"),
            choice(constant("I"), constant("J") ))
        .addRule(nt("Identifier"),
            choice(constant("x"), constant("y"), nt("ClassOrInterfaceType")))
        .addRule(nt("MethodName"),
            choice(constant("m"), constant("n") ))
        .addRule(nt("ExpressionName"),nt("Identifier") )
        .addRule(nt("Expression"),
            choice(constant("i"), constant("j") ))
        .setStart(nt("Primary"));
        TestData[] data = {
            new TestData("基本的な右再帰",
                (new Grammar())
                .addRule(nt("exp"), choice(seq(nt("num"), constant("+"),nt("exp")),
                                            nt("num")))
                .addRule(nt("num"), regExp("[0-9]+"))
                .setStart(nt("exp")),
                "123+456+789"
            ),
            new TestData( "Warthらの論文の例",
                (new Grammar())
                .addRule(nt("x"), nt("expr"))
                .addRule(nt("expr"), choice(seq(nt("x"), constant("-"), nt("num")),nt("num")))
                .addRule(nt("num"), regExp("[0-9]+"))
                .setStart(nt("x")),
                "123-456-789"),

            new TestData(
                "後藤らの論文で、「Warthのアルゴリズムでは異常終了する」とされている例（問題点(1))",
                (new Grammar())
                .addRule(nt("S"), choice(seq(nt("A"), constant("b")),constant("b")))
                .addRule(nt("A"), choice(seq(nt("A"), constant("a")),
                                         seq(nt("S"), constant("a"))))
                .setStart(nt("S")),
                "baab"),

            new TestData("後藤らの論文で、「意図した結果とは違う木になる」とされている例(問題点(2))",
                (new Grammar())
                    .addRule(nt("S"), choice(seq(nt("A"), constant("a")),constant("a")))
                    .addRule(nt("A"), nt("S"))
                    .setStart(nt("S")),
                "aa"),
            new TestData("Medeirosらの論文で、E <- E '+' E / 'n' は右結合になると書いてあった",
                (new Grammar())
                    .addRule(nt("E"), choice(seq(nt("E"), constant("+"), nt("E")),
                                            constant("n")))
                    .setStart(nt("E")),
                "n+n+n"),
            new TestData("CPEG論文で使った、メモ化しないと指数関数的時間がかかる文法",
                (new Grammar())
                    .addRule(nt("num"), regExp("[0-9]+"))
                    .addRule(nt("add"), choice(seq(nt("num"), constant("+"), nt("add"), constant(";")),
                                                seq(nt("num"), constant("+"), nt("add")),
                                                nt("num")))
                    .setStart(nt("add")),
                "1+2+3+4+5+6+7+8+9+10+11+12"),
/*                new TestData("指数関数的時間がかかる文法のバリエーション",
                (new Grammar())
                    .addRule(nt("num"), regExp("[0-9]+"))
                    .addRule(nt("add1"), seq(nt("num"), constant("+"), nt("add"), constant(";")))
                    .addRule(nt("add2"), seq(nt("num"), constant("+"), nt("add")))
                    .addRule(nt("add"), choice(nt("add1"),
                                                nt("add2"),
                                                nt("num")))
                    .setStart(nt("add")),
                "1+2+3+4+5+6+7+8+9+10+11+12"),
*/
                new TestData("梅田くんが論文で挙げた、複数箇所で左再帰が発生する例",
                (new Grammar())
                .addRule(nt("S"), seq(nt("A"), constant("-"), nt("A")))
                .addRule(nt("A"), choice(seq(nt("B"), constant("b")),
                                          constant("b")))
                .addRule(nt("B"), choice(seq(nt("B"), constant("a")),
                                          seq(nt("A"), constant("a"))))
                .setStart(nt("S")),
                "bab-bab"),
                // Java Primary
                new TestData("Javaプライマリ文法にthisを与える", javaPrimary, "this"),
                new TestData("Javaプライマリ文法にthis.x.yを与える", javaPrimary, "this.x.y"),
                new TestData("Javaプライマリ文法にthis.x.m()を与える", javaPrimary, "this.x.m()"),
                new TestData("Javaプライマリ文法にx[i][j].yを与える", javaPrimary, "x[i][j].y"),
            };
        final Map<String,ParserFactory> map = new HashMap<String,ParserFactory>();
        map.put("umeda", new ParserFactory() {
            public PegParser create(Grammar g, String input) {
                return new UmedaParser(g, input);  // 梅田の提案手法
            }
        });
        map.put("medeiros", new ParserFactory() {
            public PegParser create(Grammar g, String input) {
                return new MedeirosLeftRecursionParser(g, input);
            }
        });
        map.put("warth", new ParserFactory() {
            public PegParser create(Grammar g, String input) {
                return new WarthParser(g, input);
            }
        });
        map.put("goto", new ParserFactory() {
            public PegParser create(Grammar g, String input) {
                return new GotoParser(g, input);
            }
        });
        String parserName = "umeda";
        if (args.length > 0) {
            parserName = args[0];
        }
        for (TestData t : data) {
            System.out.println("// " + t.description);
            testGrammar(t.g, t.input, map.get(parserName));
        }
    }
}