package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;
import java.util.List;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private List<Instruction> preprocessedIR = new ArrayList<>();
    private Map<IRVariable, Integer> lastUsage = new HashMap<>();
    private List<String> asmCode = new ArrayList<>();

    // RISC-V 约定的可用临时寄存器池
    private static final String[] REGS = {"t0", "t1", "t2", "t3", "t4", "t5", "t6"};
    private IRVariable[] regContent = new IRVariable[REGS.length];
    private Map<IRVariable, Integer> varToReg = new HashMap<>();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        //IR 预处理与规范化
        for (Instruction inst : originInstructions) {
            InstructionKind kind = inst.getKind();

            if (kind == InstructionKind.ADD || kind == InstructionKind.SUB || kind == InstructionKind.MUL) {
                IRValue lhs = inst.getLHS();
                IRValue rhs = inst.getRHS();
                IRVariable result = inst.getResult();

                if (lhs.isImmediate() && rhs.isImmediate()) {
                    //双立即数二元运算：直接常量折叠
                    int l = ((IRImmediate) lhs).getValue();
                    int r = ((IRImmediate) rhs).getValue();
                    int res = 0;
                    if (kind == InstructionKind.ADD) res = l + r;
                    else if (kind == InstructionKind.SUB) res = l - r;
                    else if (kind == InstructionKind.MUL) res = l * r;
                    preprocessedIR.add(Instruction.createMov(result, IRImmediate.of(res)));
                } else if (kind == InstructionKind.ADD && lhs.isImmediate()) {
                    //左立即数+ADD：交换操作数，改为var + imm
                    preprocessedIR.add(Instruction.createAdd(result, rhs, lhs));
                } else if (kind == InstructionKind.SUB && lhs.isImmediate()) {
                    //左立即数+SUB：先MOV temp, imm，再做普通二元运算
                    IRVariable temp = IRVariable.temp();
                    preprocessedIR.add(Instruction.createMov(temp, lhs));
                    preprocessedIR.add(Instruction.createSub(result, temp, rhs));
                } else if (kind == InstructionKind.MUL && (lhs.isImmediate() || rhs.isImmediate())) {
                    //立即数+MUL：先MOV temp, imm，再做普通运算，RISC-V的mul不支持立即数
                    IRValue realLhs = lhs;
                    IRValue realRhs = rhs;
                    if (lhs.isImmediate()) {
                        IRVariable temp = IRVariable.temp();
                        preprocessedIR.add(Instruction.createMov(temp, lhs));
                        realLhs = temp;
                    }
                    if (rhs.isImmediate()) {
                        IRVariable temp = IRVariable.temp();
                        preprocessedIR.add(Instruction.createMov(temp, rhs));
                        realRhs = temp;
                    }
                    preprocessedIR.add(Instruction.createMul(result, realLhs, realRhs));
                } else {
                    preprocessedIR.add(inst);
                }
            } else {
                //MOV和RET等一元或其他指令直接保留
                preprocessedIR.add(inst);
            }
        }

        //统计每个变量最后一次使用的位置 (用于释放寄存器)
        for (int i = 0; i < preprocessedIR.size(); i++) {
            Instruction inst = preprocessedIR.get(i);
            InstructionKind kind = inst.getKind();

            //统计操作数
            for (IRValue op : inst.getOperands()) {
                if (op != null && op.isIRVariable()) {
                    lastUsage.put((IRVariable) op, i);
                }
            }
            //统计结果赋值变量
            if (kind == InstructionKind.ADD || kind == InstructionKind.SUB || kind == InstructionKind.MUL || kind == InstructionKind.MOV) {
                IRVariable res = inst.getResult();
                if (res != null) {
                    lastUsage.put(res, i);
                }
            }
        }
        //throw new NotImplementedException();
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        //边分配寄存器边生成汇编
        asmCode.add(".text");
        for (int i = 0; i < preprocessedIR.size(); i++) {
            Instruction inst = preprocessedIR.get(i);
            InstructionKind kind = inst.getKind();

            if (kind == InstructionKind.MOV) {
                IRValue from = inst.getFrom();
                String rs = "";
                //严谨获取操作数寄存器
                if (!from.isImmediate()) rs = getReg((IRVariable) from, i);

                //再获取目标寄存器 (此顺序允许目标抢占源寄存器)
                String rd = getReg(inst.getResult(), i);

                if (from.isImmediate()) {
                    asmCode.add(String.format("    li %s, %d", rd, ((IRImmediate) from).getValue()));
                } else {
                    asmCode.add(String.format("    mv %s, %s", rd, rs));
                }
            } else if (kind == InstructionKind.ADD || kind == InstructionKind.SUB) {
                IRValue lhs = inst.getLHS();
                IRValue rhs = inst.getRHS();

                String rs1 = getReg((IRVariable) lhs, i);
                String rs2 = "";
                if (!rhs.isImmediate()) rs2 = getReg((IRVariable) rhs, i);

                String rd = getReg(inst.getResult(), i);

                if (rhs.isImmediate()) {
                    String op = kind == InstructionKind.ADD ? "addi" : "subi";
                    asmCode.add(String.format("    %s %s, %s, %d", op, rd, rs1, ((IRImmediate) rhs).getValue()));
                } else {
                    String op = kind == InstructionKind.ADD ? "add" : "sub";
                    asmCode.add(String.format("    %s %s, %s, %s", op, rd, rs1, rs2));
                }
            } else if (kind == InstructionKind.MUL) {
                String rs1 = getReg((IRVariable) inst.getLHS(), i);
                String rs2 = getReg((IRVariable) inst.getRHS(), i);
                String rd = getReg(inst.getResult(), i);
                asmCode.add(String.format("    mul %s, %s, %s", rd, rs1, rs2));
            } else if (kind == InstructionKind.RET) {
                IRValue retVal = inst.getReturnValue();
                if (retVal.isImmediate()) {
                    asmCode.add(String.format("    li a0, %d", ((IRImmediate) retVal).getValue()));
                } else {
                    String rs = getReg((IRVariable) retVal, i);
                    asmCode.add(String.format("    mv a0, %s", rs));
                }
            }
        }
    }

    private String getReg(IRVariable var, int currentIndex) {
        //如果变量已在寄存器中，直接返回
        if (varToReg.containsKey(var)) {
            return REGS[varToReg.get(var)];
        }

        //尝试寻找空闲寄存器
        for (int i = 0; i < REGS.length; i++) {
            if (regContent[i] == null) {
                regContent[i] = var;
                varToReg.put(var, i);
                return REGS[i];
            }
        }

        //寄存器满，夺取不再使用的变量寄存器 (lastUsage<=当前索引)
        for (int i = 0; i < REGS.length; i++) {
            IRVariable occupant = regContent[i];
            if (occupant != null && lastUsage.getOrDefault(occupant, -1) <= currentIndex) {
                varToReg.remove(occupant); //旧变量移出
                regContent[i] = var;       //新变量入住
                varToReg.put(var, i);
                return REGS[i];
            }
        }

        throw new RuntimeException("Register allocation failed: No available registers.");
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asmCode);

        //throw new NotImplementedException();
    }
}

