package com;

import java.io.*;

public class PatternGenerator
{

    public static void main(String[] args)
    {
        byte[] data = new byte[15];
        Dis.ReversibleInputStream in = new Dis.ReversibleInputStream(data);
        int mode = 32;
        
        int index=0;
        while (true)
        {
            Instruction x;
            try
            {
                x = Dis.decode(in, mode);
            }
            catch (IllegalStateException e)
            {
                index = advance(index, data);
                if (index == -1)
                    break;
                continue;
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                index--;
                if ((index == 0) && (data[0] == -1))
                    break;
                data[index]++;
                for (int i = index+1; i < data.length; i++)
                    data[i] = (byte)0;
                continue;
            }
            if (x.operator.equals("invalid"))
            {
                index = advance(index, data);
                if (index == -1)
                    break;
                continue;
            }
            StringBuffer pat = new StringBuffer();
            for (int c=0; c < x.x86Length; c++)
                pat.append(String.format("%02x", data[c]&0xFF));
            // mask out with DD and II
            String disam = x.toString();
            for (Instruction.Operand op: x.operand)
            {
                if (op.type == null)
                {
                    disam = "Unknown";
                }
                if (op.type.equals("OP_IMM") || op.type.equals("OP_JIMM"))
                {
                    for (int c=op.imm_start; c < op.imm_start+op.size/8; c++)
                    {
                        pat.setCharAt(2*c, 'I');
                        pat.setCharAt(2*c+1, 'I');
                    }
                }
                else if (op.type.equals("OP_MEM"))
                {
                    if (op.offset > 0)
                    {
                        for (int c=op.dis_start; c < op.dis_start+op.offset/8; c++)
                        {
                            pat.setCharAt(2*c, 'D');
                            pat.setCharAt(2*c+1, 'D');
                        }
                    }
                }
                else if (op.type.equals("OP_PTR"))
                {
                    if (op.size == 32)
                    {
                        for (int c=op.dis_start; c < op.dis_start+2; c++)
                        {
                            pat.setCharAt(2*c, 'D');
                            pat.setCharAt(2*c+1, 'D');
                        }
                        for (int c=op.dis_start+2; c < op.dis_start+4; c++)
                        {
                            pat.setCharAt(2*c, 'S');
                            pat.setCharAt(2*c+1, 'S');
                        }
                    }
                    else if (op.size == 48)
                    {
                        for (int c=op.dis_start; c < op.dis_start+4; c++)
                        {
                            pat.setCharAt(2*c, 'D');
                            pat.setCharAt(2*c+1, 'D');
                        }
                        for (int c=op.dis_start+4; c < op.dis_start+6; c++)
                        {
                            pat.setCharAt(2*c, 'S');
                            pat.setCharAt(2*c+1, 'S');
                        }
                    }
                }
            }
            System.out.println(pat + " " + disam);
            // find last byte that is not II SS or DD and increment it, zeroing above it
            index = lastOpcodeByteBefore(pat.toString(), pat.length()-1);
            index = advance(index, data);
            if (index == -1)
                break;
        }
        
    }

    public static int advance(int index, byte[] data)
    {
        while ((index > 0) && !((data[index] & 0xFF) < 255))
            index--;
        if ((index == 0) && (data[0] == -1))
            return -1;
        data[index]++;
        for (int i = index+1; i < data.length; i++)
            data[i] = (byte)0;
        return index;
    }

    public static int lastOpcodeByteBefore(String pat, int index)
    {
        while ((index > 0) && ((pat.charAt(index) == 'I') || (pat.charAt(index) == 'D') || (pat.charAt(index) == 'S')))
            index--;
        return index/2;
    }
}