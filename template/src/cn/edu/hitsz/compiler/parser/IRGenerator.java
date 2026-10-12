package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private SymbolTable symbolTable;
    private final List<Instruction> instructions = new ArrayList<>();
    //中间代码生成栈存储IRValue或为了同步压入的null/Token
    private final Stack<Object> irStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        //当遇到标识符或常数时，将其转换为IRValue并压栈
        String identifier = currentToken.getKind().getIdentifier();
        if ("IntConst".equals(identifier)) {
            irStack.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else if ("id".equals(identifier)) {
            irStack.push(IRVariable.named(currentToken.getText()));
        } else {
            //其他终结符压入占位
            irStack.push(currentToken);
        }

        //throw new NotImplementedException();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        switch (production.index()) {
            case 6: // S -> id = E
                IRValue eVal = (IRValue) irStack.pop();
                irStack.pop(); // pop '='
                IRVariable idVar = (IRVariable) irStack.pop();
                instructions.add(Instruction.createMov(idVar, eVal));
                irStack.push(null);
                break;
            case 7: // S -> return E
                IRValue retVal = (IRValue) irStack.pop();
                irStack.pop(); // pop 'return'
                instructions.add(Instruction.createRet(retVal));
                irStack.push(null);
                break;
            case 8: // E -> E + A
                IRValue aValAdd = (IRValue) irStack.pop();
                irStack.pop(); // pop '+'
                IRValue eValAdd = (IRValue) irStack.pop();
                IRVariable tempAdd = IRVariable.temp();
                instructions.add(Instruction.createAdd(tempAdd, eValAdd, aValAdd));
                irStack.push(tempAdd);
                break;
            case 9: // E -> E - A
                IRValue aValSub = (IRValue) irStack.pop();
                irStack.pop(); // pop '-'
                IRValue eValSub = (IRValue) irStack.pop();
                IRVariable tempSub = IRVariable.temp();
                instructions.add(Instruction.createSub(tempSub, eValSub, aValSub));
                irStack.push(tempSub);
                break;
            case 11: // A -> A * B
                IRValue bValMul = (IRValue) irStack.pop();
                irStack.pop(); // pop '*'
                IRValue aValMul = (IRValue) irStack.pop();
                IRVariable tempMul = IRVariable.temp();
                instructions.add(Instruction.createMul(tempMul, aValMul, bValMul));
                irStack.push(tempMul);
                break;
            case 13: // B -> ( E )
                irStack.pop(); // pop ')'
                IRValue eValParen = (IRValue) irStack.pop();
                irStack.pop(); // pop '('
                irStack.push(eValParen); // 传递 E 的值
                break;
            case 10: // E -> A
            case 12: // A -> B
            case 14: // B -> id
            case 15: // B -> IntConst
                // 单个非终结符的推导，直接把子节点的值传递上去
                IRValue val = (IRValue) irStack.pop();
                irStack.push(val);
                break;
            default:
                // 其他无中间代码生成的产生式，直接出栈并压入 null 保持栈同步
                int size = production.body().size();
                for (int i = 0; i < size; i++) {
                    irStack.pop();
                }
                irStack.push(null);
                break;
        }

        //throw new NotImplementedException();
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        //throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
        //throw new NotImplementedException();
    }

    public List<Instruction> getIR() {
        // TODO
        return instructions;
        //throw new NotImplementedException();
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

