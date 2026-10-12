package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable symbolTable;
    //语义分析栈用于存储Token或Type
    private final Stack<Object> semanticStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        //throw new NotImplementedException();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        switch (production.index()) {
            case 4:   //S -> D id
                //弹出右部的id和D
                Token idToken = (Token) semanticStack.pop();
                SourceCodeType type = (SourceCodeType) semanticStack.pop();
                //更新符号表中id类型
                symbolTable.get(idToken.getText()).setType(type);
                semanticStack.push(null);
                break;
            case 5:  //D -> int
                semanticStack.pop();   // 弹出int关键字的Token
                semanticStack.push(SourceCodeType.Int);
                break;
            default:
                //对其他产生式按右部长度出栈，并压入null占位同步
                int size = production.body().size();
                for (int i = 0; i < size; i++) {
                    semanticStack.pop();
                }
                semanticStack.push(null);
                break;
        }
        //throw new NotImplementedException();
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        //移进时，将当前词法单元直接压入栈中
        semanticStack.push(currentToken);
        //throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        this.symbolTable = table;
        //throw new NotImplementedException();
    }
}

