package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.stream.StreamSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String sourceCode;             // 源代码缓冲区
    private final List<Token> tokens;      // 生成的 Token 列表

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.tokens = new ArrayList<>();
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 读取文件全部内容进入缓冲区
        try {
            sourceCode = Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read source code file: " + path, e);
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        int cursor = 0;
        int length = sourceCode.length();

        while (cursor < length) {
            char c = sourceCode.charAt(cursor);

            //1跳过空白字符（空格、制表符、换行）
            if (Character.isWhitespace(c)) {
                cursor++;
                continue;
            }

            //2识别标识符id或关键字
            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (cursor < length && Character.isLetterOrDigit(sourceCode.charAt(cursor))) {
                    sb.append(sourceCode.charAt(cursor));
                    cursor++;
                }
                String word = sb.toString();

                //判断是否是关键字
                if (word.equals("int") || word.equals("return")) {
                    tokens.add(Token.simple(word));
                } else {
                    // 是用户自定义的标识符，加入符号表
                    if (!symbolTable.has(word)) {
                        symbolTable.add(word);
                    }
                    tokens.add(Token.normal("id", word));
                }
            }
            //3识别常数IntConst
            else if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (cursor < length && Character.isDigit(sourceCode.charAt(cursor))) {
                    sb.append(sourceCode.charAt(cursor));
                    cursor++;
                }
                tokens.add(Token.normal("IntConst", sb.toString()));
            }
            //4识别运算符和分界符
            else {
                switch (c) {
                    case '=' -> tokens.add(Token.simple("="));
                    case ';' -> tokens.add(Token.simple("Semicolon"));
                    case '+' -> tokens.add(Token.simple("+"));
                    case '-' -> tokens.add(Token.simple("-"));
                    case '*' -> tokens.add(Token.simple("*"));
                    case '/' -> tokens.add(Token.simple("/"));
                    case '(' -> tokens.add(Token.simple("("));
                    case ')' -> tokens.add(Token.simple(")"));
                    default -> throw new RuntimeException("Invalid character encountered: " + c);
                }
                cursor++;
            }
        }

        //最后追加 EOF 标记
        tokens.add(Token.eof());
    }
    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
