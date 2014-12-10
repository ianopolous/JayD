package com;

import java.io.*;
import static com.ZygoteOperand.*;
import static com.Table.*;

public class Dis
{

    static ZygoteInstruction[][] itab = new Table().itab_list;
    public static final int vendor = VENDOR_INTEL;
    public static ZygoteInstruction ie_invalid = new ZygoteInstruction("invalid", O_NONE, O_NONE, O_NONE, P_none);
    public static ZygoteInstruction ie_pause = new ZygoteInstruction("pause", O_NONE, O_NONE,    O_NONE, P_none);
    public static ZygoteInstruction ie_nop = new ZygoteInstruction("nop", O_NONE, O_NONE, O_NONE, P_none);

    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            Instruction d = decode(new ReversibleInputStream(new byte[5]), 32);
            System.out.println(d);
            return;
        }
        int size =32;
        if (args[0].equals("-rm"))
        {
            size = 16;
            String[] tmp = new String[args.length-1];
            System.arraycopy(args, 1, tmp, 0, tmp.length);
            args = tmp;
        }
        byte[] data = new byte[args.length];
        for (int i=0; i < args.length; i++)
            data[i] = (byte)Integer.parseInt(args[i], 16);
        System.out.printf("Raw bytes: ");
        for (int i=0; i < args.length; i++)
            System.out.printf("%2x ", data[i]);
        System.out.println();
        Instruction x = decode(new ReversibleInputStream(data), size);
        System.out.println(x);
    }

    public static Instruction decode(ReversibleInputStream input, int mode)
    {
        input.resetCounter();
        Instruction in = new Instruction();
        get_prefixes(mode, input, in);
        search_table(mode, input, in);
        do_mode(mode, input, in);
        disasm_operands(mode, input, in);
        resolve_operator(mode, input, in);
        in.x86Length = input.getCounter();
        return in;
    }

    public static class ReversibleInputStream
    {
        byte[] data;
        int index=0;

        public ReversibleInputStream(byte[] data)
        {
            this.data = data;
        }

        public void resetCounter()
        {
            index = 0;
        }

        public int getCounter()
        {
            return index;
        }

        public long read(long bits)
        {
            if (bits == 8)
                return data[index++];
            if (bits == 16)
                return read16();
            if (bits == 32)
                return read32();
            if (bits == 64)
                return read32() | (((long)read32()) << 32);
            throw new IllegalStateException("unimplemented read amount " + bits);
        }

        public int read32()
        {
            return (data[index++] &0xFF) | ((data[index++] & 0xFF) << 8) | ((data[index++] & 0xFF) << 16) | ((data[index++] & 0xFF) << 24);
        }

        public int read16()
        {
            return (data[index++] & 0xFF) | (data[index++] << 8);
        }

        public int getByte()
        {
            return data[index] & 0xFF;
        }

        public void forward()
        {
            index++;
        }

        public void reverse()
        {
            index--;
        }
    }

    private static void get_prefixes(int mode, ReversibleInputStream input, Instruction inst)
    {
        int curr;
        int i=0;
        while(true)
        {
            curr = input.getByte();
            input.forward();
            i++;

            if ((mode == 64) && ((curr & 0xF0) == 0x40))
                inst.pfx.rex = curr;
            else
            {
                if (curr == 0x2E)
                {
                    inst.pfx.seg = "cs";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x36)
                {
                    inst.pfx.seg = "ss";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x3E)
                {
                    inst.pfx.seg = "ds";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x26)
                {
                    inst.pfx.seg = "es";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x64)
                {
                    inst.pfx.seg = "fs";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x65) 
                {
                    inst.pfx.seg = "gs";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x67) //adress-size override prefix
                { 
                    inst.pfx.adr = 0x67;
                    inst.pfx.rex = 0;
                }
                else if (curr == 0xF0)
                {
                    inst.pfx.lock = 0xF0;
                    inst.pfx.rex  = 0;
                }
                else if (curr == 0x66)
                {
                    // the 0x66 sse prefix is only effective if no other sse prefix
                    // has already been specified.
                    if (inst.pfx.insn == 0)
                        inst.pfx.insn = 0x66;
                    inst.pfx.opr = 0x66;         
                    inst.pfx.rex = 0;
                }
                else if (curr == 0xF2)
                {
                    inst.pfx.insn  = 0xF2;
                    inst.pfx.repne = 0xF2;
                    inst.pfx.rex   = 0;
                }
                else if (curr == 0xF3)
                {
                    inst.pfx.insn = 0xF3;
                    inst.pfx.rep  = 0xF3;
                    inst.pfx.repe = 0xF3;
                    inst.pfx.rex  = 0;
                }
                else 
                    //No more prefixes
                    break;
            }
        }
        if (i >= MAX_INSTRUCTION_LENGTH)
            throw new IllegalStateException("Max instruction length exceeded");
        
        input.reverse();

        // speculatively determine the effective operand mode,
        // based on the prefixes and the current disassembly
        // mode. This may be inaccurate, but useful for mode
        // dependent decoding.
        if (mode == 64)
        {
            if (REX_W(inst.pfx.rex) != 0)
                inst.opr_mode = 64;
            else if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else if (P_DEF64(inst.zygote.prefix) != 0)
                inst.opr_mode = 64;
            else
                inst.opr_mode = 32;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32;
            else
                inst.adr_mode = 64;
        }
        else if (mode == 32)
        {
            if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else
                inst.opr_mode = 32;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 16;
            else
                inst.adr_mode = 32;
        }
        else if (mode == 16)
        {
            if (inst.pfx.opr != 0)
                inst.opr_mode = 32;
            else
                inst.opr_mode = 16;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32;
            else
                inst.adr_mode = 16;
        }
    }

    private static void search_table(int mode, ReversibleInputStream input, Instruction inst)
    {
        boolean did_peek = false;
        int peek;
        int curr = input.getByte();
        input.forward();
        
        int table=0;
        ZygoteInstruction e;

        // resolve xchg, nop, pause crazyness
        if (0x90 == curr)
        {
            if (!((mode == 64) && (REX_B(inst.pfx.rex) != 0)))
            {
                if (inst.pfx.rep != 0)
                {
                    inst.pfx.rep = 0;
                    e = ie_pause;
                }
                else
                    e = ie_nop;
                inst.zygote = e;
                inst.operator = inst.zygote.operator;
                return;
            }
        }
        else if (curr == 0x0F)
        {
            table = ITAB__0F;
            curr = input.getByte();
            input.forward();

            // 2byte opcodes can be modified by 0x66, F3, and F2 prefixes
            if (0x66 == inst.pfx.insn)
            {
                if (!itab[ITAB__PFX_SSE66__0F][curr].operator.equals("invalid"))
                {
                    table = ITAB__PFX_SSE66__0F;
                    //inst.pfx.opr = 0;
                }
            }
            else if (0xF2 == inst.pfx.insn)
            {
                if (!itab[ITAB__PFX_SSEF2__0F][curr].operator.equals("invalid"))
                {
                    table = ITAB__PFX_SSEF2__0F;
                    inst.pfx.repne = 0;
                }
            }
            else if (0xF3 == inst.pfx.insn)
            {
                if (!itab[ITAB__PFX_SSEF3__0F][curr].operator.equals("invalid"))
                {
                    table = ITAB__PFX_SSEF3__0F;
                    inst.pfx.repe = 0;
                    inst.pfx.rep  = 0;
                }
            }
        }
        else
            table = ITAB__1BYTE;
        
        int index = curr;

        while (true)
        {
            e = itab[table][index];
            // if operator constant is a standard instruction constant
            // our search is over.
            if (operator.contains(e.operator))
            {
                if (e.operator.equals("invalid"))
                    if (did_peek)
                        input.forward();
                inst.zygote = e;
                inst.operator = e.operator;
                return;
            }

            table = e.prefix;

            if (e.operator.equals("grp_reg"))
            {
                peek     = input.getByte();
                did_peek = true;
                index    = MODRM_REG(peek);
            }
            else if (e.operator.equals("grp_mod"))
            {
                peek     = input.getByte();
                did_peek = true;
                index    = MODRM_MOD(peek);
                if (index == 3)
                    index = ITAB__MOD_INDX__11;
                else
                    index = ITAB__MOD_INDX__NOT_11;
            }
            else if (e.operator.equals("grp_rm"))
            {
                curr = input.getByte();
                input.forward();
                did_peek = false;
                index = MODRM_RM(curr);
            }
            else if (e.operator.equals("grp_x87"))
            {
                curr = input.getByte();
                input.forward();
                did_peek = false;
                index    = curr - 0xC0;
            }
            else if (e.operator.equals("grp_osize"))
            {
                if (inst.opr_mode == 64)
                    index = ITAB__MODE_INDX__64;
                else if (inst.opr_mode == 32) 
                    index = ITAB__MODE_INDX__32;
                else
                    index = ITAB__MODE_INDX__16;
            }
            else if (e.operator.equals("grp_asize"))
            {
                if (inst.adr_mode == 64)
                    index = ITAB__MODE_INDX__64;
                else if (inst.adr_mode == 32) 
                    index = ITAB__MODE_INDX__32;
                else
                    index = ITAB__MODE_INDX__16;
            }
            else if (e.operator.equals("grp_mode"))
            {
                if (mode == 64)
                    index = ITAB__MODE_INDX__64;
                else if (mode == 32)
                    index = ITAB__MODE_INDX__32;
                else
                    index = ITAB__MODE_INDX__16;
            }
            else if (e.operator.equals("grp_vendor"))
            {
                if (vendor == VENDOR_INTEL) 
                    index = ITAB__VENDOR_INDX__INTEL;
                else if (vendor == VENDOR_AMD)
                    index = ITAB__VENDOR_INDX__AMD;
                else
                    throw new RuntimeException("unrecognized vendor id");
            }
            else if (e.operator.equals("d3vil"))
                throw new RuntimeException("invalid instruction operator constant Id3vil");
            else
                throw new RuntimeException("invalid instruction operator constant");
        }
            //inst.zygote = e;
            //inst.operator = e.operator;
            //return;
    }

    private static void do_mode(int mode, ReversibleInputStream input, Instruction inst)
    {
        // propagate prefix effects 
        if (mode == 64)  // set 64bit-mode flags
        {
            // Check validity of  instruction m64 
            if ((P_INV64(inst.zygote.prefix) != 0))
                throw new IllegalStateException("Invalid instruction");

            // effective rex prefix is the  effective mask for the 
            // instruction hard-coded in the opcode map.
            inst.pfx.rex = ((inst.pfx.rex & 0x40) 
                            |(inst.pfx.rex & REX_PFX_MASK(inst.zygote.prefix)));

            // calculate effective operand size 
            if ((REX_W(inst.pfx.rex) != 0) || (P_DEF64(inst.zygote.prefix) != 0))
                inst.opr_mode = 64;
            else if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else
                inst.opr_mode = 32;

            // calculate effective address size
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32; 
            else
                inst.adr_mode = 64;
        }
        else if (mode == 32) // set 32bit-mode flags
        { 
            if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else
                inst.opr_mode = 32;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 16;
            else 
                inst.adr_mode = 32;
        }
        else if (mode == 16) // set 16bit-mode flags
        {
            if (inst.pfx.opr != 0)
                inst.opr_mode = 32;
            else 
                inst.opr_mode = 16;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32;
            else 
                inst.adr_mode = 16;
        }
    }

    private static void resolve_operator(int mode, ReversibleInputStream input, Instruction inst)
    {
        // far/near flags 
        inst.branch_dist = null;
        // readjust operand sizes for call/jmp instrcutions 
        if (inst.operator.equals("call") || inst.operator.equals("jmp"))
        {
            if (inst.operand[0].size == SZ_WP)
            {
                // WP: 16bit pointer 
                inst.operand[0].size = 16;
                inst.branch_dist = "far";
            }
            else if (inst.operand[0].size == SZ_DP)
            {
                // DP: 32bit pointer
                inst.operand[0].size = 32;
                inst.branch_dist = "far";
            }
            else if (inst.operand[0].size == 8)
                inst.branch_dist = "near";
        }
        else if (inst.operator.equals("3dnow"))
        {
            // resolve 3dnow weirdness 
            inst.operator = itab[ITAB__3DNOW][input.getByte()].operator;
        }
        // SWAPGS is only valid in 64bits mode
        if ((inst.operator.equals("swapgs")) && (mode != 64))
            throw new IllegalStateException("SWAPGS only valid in 64 bit mode");
    }
    
    private static void disasm_operands(int mode, ReversibleInputStream input, Instruction inst)
    {
        // get type
        int[] mopt = new int[inst.zygote.operand.length];
        for (int i=0; i < mopt.length; i++)
            mopt[i] = inst.zygote.operand[i].type;
        // get size
        int[] mops = new int[inst.zygote.operand.length];
        for (int i=0; i < mops.length; i++)
            mops[i] = inst.zygote.operand[i].size;
        
        if (mopt[2] != OP_NONE)
            inst.operand = new Instruction.Operand[]{new Instruction.Operand(), new Instruction.Operand(), new Instruction.Operand()};
        else if (mopt[1] != OP_NONE)
            inst.operand = new Instruction.Operand[]{new Instruction.Operand(), new Instruction.Operand()};
        else if (mopt[0] != OP_NONE)
            inst.operand = new Instruction.Operand[]{new Instruction.Operand()};
    
        // These flags determine which operand to apply the operand size
        // cast to.
        if (inst.operand.length > 0)
            inst.operand[0].cast = P_C0(inst.zygote.prefix);
        if (inst.operand.length > 1)
            inst.operand[1].cast = P_C1(inst.zygote.prefix);
        if (inst.operand.length > 2)
            inst.operand[2].cast = P_C2(inst.zygote.prefix);

        // iop = instruction operand 
        //iop = inst.operand
        
        if (mopt[0] == OP_A)
            decode_a(mode, inst, input, inst.operand[0]);
        // M[b] ... 
        // E, G/P/V/I/CL/1/S 
        else if ((mopt[0] == OP_M) || (mopt[0] == OP_E))
        {
            if ((mopt[0] == OP_M) && (MODRM_MOD(input.getByte()) == 3))
                throw new IllegalStateException("");
            if (mopt[1] == OP_G)
            {
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_GPR");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
                else if (mopt[2] == OP_CL)
                {
                    inst.operand[2].type = "OP_REG";
                    inst.operand[2].base = "cl";
                    inst.operand[2].size = 8;
                }
            }
            else if (mopt[1] == OP_P)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_MMX");
            else if (mopt[1] == OP_V)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_XMM");
            else if (mopt[1] == OP_S)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_SEG");
            else
            {
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", null, 0, "T_NONE");
                if (mopt[1] == OP_CL)
                {
                    inst.operand[1].type = "OP_REG";
                    inst.operand[1].base = "cl";
                    inst.operand[1].size = 8;
                }
                else if (mopt[1] == OP_I1)
                {
                    inst.operand[1].type = "OP_IMM";
                    inst.operand[1].lval = 1;
                }
                else if (mopt[1] == OP_I)
                    decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            }
        }
        // G, E/PR[,I]/VR 
        else if (mopt[0] == OP_G)
        {
            if (mopt[1] == OP_M)
            {
                if (MODRM_MOD(input.getByte()) == 3)
                    throw new IllegalStateException("invalid");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_GPR");
            }
            else if (mopt[1] == OP_E)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_GPR");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_PR)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_GPR");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_VR)
            {
                if (MODRM_MOD(input.getByte()) != 3)
                    throw new IllegalStateException("Invalid instruction");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_GPR");
            }
            else if (mopt[1] == OP_W)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_GPR");
        }
        // AL..BH, I/O/DX 
        else if (ops8.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("8").get(mopt[0] - OP_AL);
            inst.operand[0].size = 8;
            
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            else if (mopt[1] == OP_DX)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "dx";
                inst.operand[1].size = 16;
            }
            else if (mopt[1] == OP_O)
                decode_o(mode, inst, input, mops[1], inst.operand[1]);
        }
        // rAX[r8]..rDI[r15], I/rAX..rDI/O
        else if (ops2.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = resolve_gpr64(mode, inst, mopt[0]);
            
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            else if (ops64.contains(mopt[1]))
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = resolve_gpr64(mode, inst, mopt[1]);
            }
            else if (mopt[1] == OP_O)
            {
                decode_o(mode, inst, input, mops[1], inst.operand[1]);
                inst.operand[0].size = resolve_operand_size(mode, inst, mops[1]);
            }
        }
        else if (ops3.contains(mopt[0]))
        {
            int gpr = (mopt[0] - OP_ALr8b +(REX_B(inst.pfx.rex) << 3));
            /*if ((gpr in ["ah",	"ch",	"dh",	"bh",
              "spl",	"bpl",	"sil",	"dil",
              "r8b",	"r9b",	"r10b",	"r11b",
              "r12b",	"r13b",	"r14b",	"r15b",
                         ]) && (inst.pfx.rex != 0)) 
                         gpr = gpr + 4;*/
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("8").get(gpr);
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // eAX..eDX, DX/I 
        else if (ops32.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = resolve_gpr32(inst, mopt[0]);
            if (mopt[1] == OP_DX)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "dx";
                inst.operand[1].size = 16;
            }
            else if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // ES..GS 
        else if (ops_segs.contains(mopt[0]))
        {
            // in 64bits mode, only fs and gs are allowed 
            if (mode == 64)
                if ((mopt[0] != OP_FS) && (mopt[0] != OP_GS))
                    throw new IllegalStateException("only fs and gs allowed in 64 bit mode");
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("T_SEG").get(mopt[0] - OP_ES);
            inst.operand[0].size = 16;
        }
        // J 
        else if (mopt[0] == OP_J)
        {
            decode_imm(mode, inst, input, mops[0], inst.operand[0]);
            // MK take care of signs
            long bound = 1L << (inst.operand[0].size - 1);
            if (inst.operand[0].lval > bound)
                inst.operand[0].lval = -(((2 * bound) - inst.operand[0].lval) % bound);
            inst.operand[0].type = "OP_JIMM";
        }
        // PR, I 
        else if (mopt[0] == OP_PR)
        {
            if (MODRM_MOD(input.getByte()) != 3)
                throw new IllegalStateException("Invalid instruction");
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_MMX", null, 0, "T_NONE");
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // VR, I 
        else if (mopt[0] == OP_VR)
        {
            if (MODRM_MOD(input.getByte()) != 3)
                throw new IllegalStateException("Invalid instruction");
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_XMM", null, 0, "T_NONE");
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // P, Q[,I]/W/E[,I],VR 
        else if (mopt[0] == OP_P)
        {
            if (mopt[1] == OP_Q)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_MMX");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_W)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_MMX");
            else if (mopt[1] == OP_VR)
            {
                if (MODRM_MOD(input.getByte()) != 3)
                    throw new IllegalStateException("Invalid instruction");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_MMX");
            }
            else if (mopt[1] == OP_E)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_MMX");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
        }
        // R, C/D 
        else if (mopt[0] == OP_R)
        {
            if (mopt[1] == OP_C)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_CRG");
            else if (mopt[1] == OP_D)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_DBG");
        }
        // C, R 
        else if (mopt[0] == OP_C)
            decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_CRG");
        // D, R 
        else if (mopt[0] == OP_D)
            decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_DBG");
        // Q, P 
        else if (mopt[0] == OP_Q)
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_MMX", inst.operand[1], mops[1], "T_MMX");
        // S, E 
        else if (mopt[0] == OP_S)
            decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_SEG");
        // W, V 
        else if (mopt[0] == OP_W)
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_XMM", inst.operand[1], mops[1], "T_XMM");
        // V, W[,I]/Q/M/E 
        else if (mopt[0] == OP_V)
        {
            if (mopt[1] == OP_W)
            {
                // special cases for movlps and movhps 
                if (MODRM_MOD(input.getByte()) == 3)
                {
                    if (inst.operator.equals("movlps"))
                        inst.operator = "movhlps";
                    else if (inst.operator.equals("movhps"))
                        inst.operator = "movlhps";
                }
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_XMM");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_Q)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_XMM");
            else if (mopt[1] == OP_M)
            {
                if (MODRM_MOD(input.getByte()) == 3)
                    throw new IllegalStateException("Invalid instruction");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_XMM");
            }
            else if (mopt[1] == OP_E)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_XMM");
            else if (mopt[1] == OP_PR)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_XMM");
        }
        // DX, eAX/AL
        else if (mopt[0] == OP_DX)
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = "dx";
            inst.operand[0].size = 16;

            if (mopt[1] == OP_eAX)
            {
                inst.operand[1].type = "OP_REG";   
                inst.operand[1].base = resolve_gpr32(inst, mopt[1]);
            }
            else if (mopt[1] == OP_AL)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "al";
                inst.operand[1].size = 8;
            }
        }
        // I, I/AL/eAX
        else if (mopt[0] == OP_I)
        {
            decode_imm(mode, inst, input, mops[0], inst.operand[0]);
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            else if (mopt[1] == OP_AL)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "al";
                inst.operand[1].size = 8;
            }
            else if (mopt[1] == OP_eAX)
            {
                inst.operand[1].type = "OP_REG";  
                inst.operand[1].base = resolve_gpr32(inst, mopt[1]);
            }
        }
        // O, AL/eAX
        else if (mopt[0] == OP_O)
        {
            decode_o(mode, inst, input, mops[0], inst.operand[0]);
            inst.operand[1].type = "OP_REG";
            inst.operand[1].size = resolve_operand_size(mode, inst, mops[0]);
            if (mopt[1] == OP_AL)
                inst.operand[1].base = "al";
            else if (mopt[1] == OP_eAX)
                inst.operand[1].base = resolve_gpr32(inst, mopt[1]);
            else if (mopt[1] == OP_rAX)
                inst.operand[1].base = resolve_gpr64(mode, inst, mopt[1]);     
        }
        // 3
        else if (mopt[0] == OP_I3)
        {
            inst.operand[0].type = "OP_IMM";
            inst.operand[0].lval = 3;
        }
        // ST(n), ST(n) 
        else if (ops_st.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("T_ST").get(mopt[0] - OP_ST0);
            inst.operand[0].size = 0;

            if (ops_st.contains(mopt[1]))
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = GPR.get("T_ST").get(mopt[1] - OP_ST0);
                inst.operand[1].size = 0;
            }
        }
        // AX 
        else if (mopt[0] == OP_AX)
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = "ax";
            inst.operand[0].size = 16;
        }
        // none 
        else
            for (int i=0; i < inst.operand.length; i++)
                inst.operand[i].type = null;
    }

    private static void decode_a(int mode, Instruction inst, ReversibleInputStream input, Instruction.Operand op)
    {
        //Decodes operands of the type seg:offset.
        if (inst.opr_mode == 16)
        {
            // seg16:off16 
            op.type = "OP_PTR";
            op.size = 32;
            op.dis_start = input.getCounter();
            op.ptr = new Instruction.Ptr(input.read16(), input.read16());
        }
        else
        {
            // seg16:off32 
            op.type = "OP_PTR";
            op.size = 48;
            op.dis_start = input.getCounter();
            op.ptr = new Instruction.Ptr(input.read32(), input.read16());
        }
    }

    private static void decode_modrm(int mode, Instruction inst, ReversibleInputStream input, Instruction.Operand op, int s, String rm_type, Instruction.Operand opreg, int reg_size, String reg_type)
    {
        // get mod, r/m and reg fields
        int mod = MODRM_MOD(input.getByte());
        int rm  = (REX_B(inst.pfx.rex) << 3) | MODRM_RM(input.getByte());
        int reg = (REX_R(inst.pfx.rex) << 3) | MODRM_REG(input.getByte());

	if (reg_type.equals("T_DBG") || reg_type.equals("T_CRG")) // force these to be reg ops (mod is ignored)
	    mod = 3;

        op.size = resolve_operand_size(mode, inst, s);

        // if mod is 11b, then the m specifies a gpr/mmx/sse/control/debug 
        if (mod == 3)
        {
            op.type = "OP_REG";
            if (rm_type ==  "T_GPR")
                op.base = decode_gpr(mode, inst, op.size, rm);
            else   
                op.base = resolve_reg(rm_type, (REX_B(inst.pfx.rex) << 3) |(rm&7));
        }
        // else its memory addressing 
        else
        {
            op.type = "OP_MEM";
            op.seg = inst.pfx.seg;
            // 64bit addressing 
            if (inst.adr_mode == 64)
            {
                op.base = GPR.get("64").get(rm);
            
                // get offset type
                if (mod == 1)
                    op.offset = 8;
                else if (mod == 2)
                    op.offset = 32;
                else if ((mod == 0) &&((rm & 7) == 5))
                {     
                    op.base = "rip";
                    op.offset = 32;
                }
                else
                    op.offset = 0;

                // Scale-Index-Base(SIB)
                if ((rm & 7) == 4)
                {
                    input.forward();
                
                    op.scale = (1 << SIB_S(input.getByte())) & ~1;
                    op.index = GPR.get("64").get((SIB_I(input.getByte()) |(REX_X(inst.pfx.rex) << 3)));
                    op.base  = GPR.get("64").get((SIB_B(input.getByte()) |(REX_B(inst.pfx.rex) << 3)));

                    // special conditions for base reference
                    if (op.index.equals("rsp"))
                    {
                        op.index = null;
                        op.scale = 0;
                    }

                    if ((op.base.equals("rbp")) || (op.base.equals("r13")))
                    {
                        if (mod == 0) 
                            op.base = null;
                        if (mod == 1)
                            op.offset = 8;
                        else
                            op.offset = 32;
                    }
                }
            }
            // 32-Bit addressing mode 
            else if (inst.adr_mode == 32)
            {
                // get base 
                op.base = GPR.get("32").get(rm);

                // get offset type 
                if (mod == 1)
                    op.offset = 8;
                else if (mod == 2)
                    op.offset = 32;
                else if ((mod == 0) && (rm == 5))
                {
                    op.base = null;
                    op.offset = 32;
                }
                else
                    op.offset = 0;

                // Scale-Index-Base(SIB)
                if ((rm & 7) == 4)
                {
                    input.forward();

                    op.scale = (1 << SIB_S(input.getByte())) & ~1;
                    op.index = GPR.get("32").get(SIB_I(input.getByte()) |(REX_X(inst.pfx.rex) << 3));
                    op.base  = GPR.get("32").get(SIB_B(input.getByte()) |(REX_B(inst.pfx.rex) << 3));

                    if (op.index.equals("esp"))
                    {
                        op.index = null;
                        op.scale = 0;
                    }

                    // special condition for base reference 
                    if (op.base.equals("ebp"))
                    {
                        if (mod == 0)
                            op.base = null;
                        if (mod == 1)
                            op.offset = 8;
                        else
                            op.offset = 32;
                    }
                }
            }
            // 16bit addressing mode 
            else
            {
                if (rm == 0)
                {
                    op.base = "bx";
                    op.index = "si";
                }
                else if (rm == 1)
                {
                    op.base = "bx";
                    op.index = "di";
                }
                else if (rm == 2)
                {
                    op.base = "bp";
                    op.index = "si";
                }
                else if (rm == 3) 
                {
                    op.base = "bp";
                    op.index = "di";
                }
                else if (rm == 4) 
                    op.base = "si";
                else if (rm == 5) 
                    op.base = "di";
                else if (rm == 6) 
                    op.base = "bp";
                else if (rm == 7) 
                    op.base = "bx";
                
                if ((mod == 0) && (rm == 6))
                {
                    op.offset = 16;
                    op.base = null;
                }
                else if (mod == 1)
                    op.offset = 8;
                else if (mod == 2) 
                    op.offset = 16;
            }
        }
        input.forward();
        // extract offset, if any 
        if ((op.offset==8) || (op.offset==16) ||(op.offset==32) || (op.offset==64))
        {
            op.dis_start = input.getCounter();
            op.lval  = input.read(op.offset);
            long bound = 1L << (op.offset - 1);
            if (op.lval > bound)
                op.lval = -(((2 * bound) - op.lval) % bound);
        }

        // resolve register encoded in reg field
        if (opreg != null)
        {
            opreg.type = "OP_REG";
            opreg.size = resolve_operand_size(mode, inst, reg_size);
            if (reg_type.equals("T_GPR"))
                opreg.base = decode_gpr(mode, inst, opreg.size, reg);
            else
                opreg.base = resolve_reg(reg_type, reg);
        }
    }

    private static void decode_imm(int mode, Instruction inst, ReversibleInputStream input, int s, Instruction.Operand op)
    {
        op.size = resolve_operand_size(mode, inst, s);
        op.type = "OP_IMM";
        op.imm_start = input.getCounter();
        op.lval = input.read(op.size);
    }

    private static void decode_o(int mode, Instruction inst, ReversibleInputStream input, int s, Instruction.Operand op)
    {
        // offset
        op.seg = inst.pfx.seg;
        op.offset = inst.adr_mode;
        op.dis_start = input.getCounter();
        op.lval = input.read(inst.adr_mode);
        op.type = "OP_MEM";
        op.size = resolve_operand_size(mode, inst, s);
    }

    private static String resolve_gpr32(Instruction inst, int gpr_op)
    {
        int index = gpr_op - OP_eAX;
        if(inst.opr_mode == 16)
            return GPR.get("16").get(index);
        return GPR.get("32").get(index);
    }

    private static String resolve_gpr64(int mode, Instruction inst, int gpr_op)
    {
        int index = 0;
        if ((OP_rAXr8 <= gpr_op) && (OP_rDIr15 >= gpr_op))
            index = (gpr_op - OP_rAXr8) |(REX_B(inst.pfx.rex) << 3);
        else
            index = gpr_op - OP_rAX;
        if (inst.opr_mode == 16)
            return GPR.get("16").get(index);
        else if ((mode == 32) || !((inst.opr_mode == 32) && (REX_W(inst.pfx.rex) == 0)))
            return GPR.get("32").get(index);
        return GPR.get("64").get(index);
    }

    private static int resolve_operand_size(int mode, Instruction inst, int s)
    {
        if (s ==  SZ_V)
            return inst.opr_mode;
        else if (s ==  SZ_Z)  
            if (inst.opr_mode == 16)
                return 16;
            else
                return 32;
        else if (s ==  SZ_P)  
            if (inst.opr_mode == 16)
                return SZ_WP;
            else
                return SZ_DP;
        else if (s ==  SZ_MDQ)
            if (inst.opr_mode == 16)
                return 32;
            else
                return inst.opr_mode;
        else if (s ==  SZ_RDQ)
            if (mode == 64)
                return 64;
            else
                return 32;
        else
            return s;
    }

    private static String decode_gpr(int mode, Instruction inst, int s, int rm)
    {
        s = resolve_operand_size(mode, inst, s);
          
        if (s == 64)
            return GPR.get("64").get(rm);
        else if ((s == SZ_DP) || (s == 32))
            return GPR.get("32").get(rm);
        else if ((s == SZ_WP) || (s == 16))
            return GPR.get("16").get(rm);
        else if (s == 8)
        {
            if ((mode == 64) && (inst.pfx.rex != 0))
            {
                if (rm >= 4)
                    return GPR.get("8").get(rm+4);
                return GPR.get("8").get(rm);
            }
            else
                return GPR.get("8").get(rm);
        }
        else
            return null;
    }

    private static String resolve_reg(String regtype, int i)
    {
        return GPR.get(regtype).get(i);
    }
}