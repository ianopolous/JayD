package com;

import java.io.*;
import java.util.*;

public class ObjdumpCompare
{

    public static void main(String[] args) throws IOException
    {
        int mode = 32;
       
        Map<String, String> invalid = new HashMap();
        for (String file: args)
        {
            int lineCount = 0;
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (true) 
            {
                String line = br.readLine();
                if (line == null)
                    break;
                lineCount++;

                if (line.indexOf("\t") < 0)
                    continue;
                if (line.length() < 5)
                    continue;
                
                String[] parts = line.split("\t");
                String addrString = parts[0].trim();
                if (!addrString.endsWith(":"))
                    continue;
                    //throw new IllegalStateException("Invalid address prefix on line "+lineCount + " of " + file);
                long address = Long.parseLong(addrString.substring(0, addrString.length()-1), 16);
                
                String x86Bytes = parts[1].trim();
                String[] byteStrings = parts[1].trim().split(" ");
                byte[] rawX86 = new byte[byteStrings.length];
                for (int i=0; i<rawX86.length; i++)
                    rawX86[i] = (byte) Integer.parseInt(byteStrings[i], 16);
                
                String objdump = parts[2].trim().replaceAll("\\s+", " ").replace("eiz+", "").replace("+eiz*1", "").replace("+eiz*2", "").replace("+eiz*4", "").replace("+eiz*8", "").replace("*1","").replace("DWORD PTR ", "").replace("XMMWORD PTR ", "").replace("st(7)", "st7").replace("st(6)", "st6").replace("st(5)", "st5").replace("st(4)", "st4").replace("st(3)", "st3").replace("st(2)", "st2").replace("st(1)", "st1").replace("st(0)", "st0").replace("st,", "st0,").replace(",1",",0x1").replace("gs ","").replace("es ","").replace("data32 ","");
                if (objdump.endsWith(",st"))
                    objdump = objdump.replace(",st", ",st0");
                int end = objdump.indexOf("<");
                if (end >0)
                    objdump = objdump.substring(0, end).trim();
                if (objdump.contains("?") || objdump.contains("bad") || objdump.contains("fs:gs:") || objdump.contains(".byte") || objdump.contains("addr16") || objdump.contains("data16") || objdump.startsWith("nop") || objdump.equals("lock") || objdump.equals("cs") || objdump.equals("ds") || objdump.equals("es") || objdump.equals("fs") || objdump.equals("gs") || objdump.equals("ss") || objdump.equals("fnop") || objdump.equals("xgetbv"))
                    continue;

                List<String> excludes = Arrays.asList("insb", "insd", "outsb", "outsw", "outsd", "movsb", "movsw", "movsd", "lodsb", "lodsw", "lodsd", "stosb", "stosw", "stosd", "scasb", "scasw", "scasd", "cmpsb", "cmpsw", "cmpsd", "prefetch", "prefetcht0", "prefetchnta", "ret", "iretd", "fld", "lea", "fxch", "fcom", "fcomp", "pause", "sahf", "mov", "popad", "popfd", "pushfd", "pushad", "xlatb", "frstor", "fnsave", "fldenv", "fnstenv", "rcl", "jle", "je", "jbe", "int1", "push", "wait", "popa", "pshufw", "movq", "movlps", "movlpd", "movhpd", "call", "jmp", "bound", "fsub", "fsubrp", "pop", "arpl", "aam", "dec", "and", "add", "fiadd", "fisttp", "sub", "enter", "sldt", "les", "lds", "lfs", "hlt", "str", "cmpxchg8b");

                Dis.ReversibleInputStream in = new Dis.ReversibleInputStream(rawX86);
                Instruction x;
                try
                {
                    x = Dis.decode(in, mode);
                    x.eip = address;
                } catch (Exception e)
                {
                    if (!objdump.startsWith("v")) // AVX
                        System.out.printf("Disassemble error on : %s (%s)", x86Bytes, objdump);
                    continue;
                    //throw e;
                    //e.printStackTrace();
                }
                try
                {
                    if (x.operator.equals("invalid"))
                    {
                        String opname = objdump.substring(0, objdump.indexOf(" "));
                        if (!invalid.containsKey(opname))
                        {
                            invalid.put(opname, x86Bytes+" " + objdump);
                            System.out.println(x86Bytes + " -> " + x + " != " + objdump);
                        }
                        continue;
                    }
                    if ((x.operator != "nop") || (!objdump.contains("xchg")))
                        if (!excludes.contains(x.operator))
                            if (!x.toString().replace("near ", "").replace("0x", "").replace("DWORD PTR ", "").equals(objdump.replace("0x", "")))
                                System.out.println(x86Bytes + " -> " + x + " != " + objdump);
                } catch (Exception e)
                {
                    System.out.printf("Print error on : %s (%s)", x86Bytes, objdump);
                    throw e;
                }
            }
        }
    }  
}