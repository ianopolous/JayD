package com.gen;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class TableGen
{
    private static Set<String> spl_mnm_types = new HashSet();
    private static Map<String, String> vend_dict = new HashMap();
    private static Map<String, String> mode_dict = new HashMap();
    private static Map<String, String[]> operand_dict = new HashMap();
    private static Map<String, String> pfx_dict = new HashMap();
    private static String default_opr = "O_NONE, O_NONE, O_NONE";

    static
    {
        spl_mnm_types.add("d3vil");
        spl_mnm_types.add("na");
        spl_mnm_types.add("grp_reg");
        spl_mnm_types.add("grp_rm");
        spl_mnm_types.add("grp_vendor");
        spl_mnm_types.add("grp_x87");
        spl_mnm_types.add("grp_mode");
        spl_mnm_types.add("grp_osize");
        spl_mnm_types.add("grp_asize");
        spl_mnm_types.add("grp_mod");
        spl_mnm_types.add("none");
        vend_dict.put("AMD", "00");
        vend_dict.put("INTEL", "01");
        mode_dict.put("16", "00");
        mode_dict.put("32", "01");
        mode_dict.put("64", "02");
        operand_dict.put("Ap", new String[]{"OP_A", "SZ_P"});
        operand_dict.put("E", new String[]{"OP_E", "SZ_NA"});
        operand_dict.put("Eb", new String[]{"OP_E", "SZ_B"});
        operand_dict.put("Ew", new String[]{"OP_E", "SZ_W"});
        operand_dict.put("Ev", new String[]{"OP_E", "SZ_V"});
        operand_dict.put("Ed", new String[]{"OP_E", "SZ_D"});
        operand_dict.put("Ez", new String[]{"OP_E", "SZ_Z"});
        operand_dict.put("Ex", new String[]{"OP_E", "SZ_MDQ"});
        operand_dict.put("Ep", new String[]{"OP_E", "SZ_P"});
        operand_dict.put("G", new String[]{"OP_G", "SZ_NA"});
        operand_dict.put("Gb", new String[]{"OP_G", "SZ_B"});
        operand_dict.put("Gw", new String[]{"OP_G", "SZ_W"});
        operand_dict.put("Gv", new String[]{"OP_G", "SZ_V"});
        operand_dict.put("Gvw", new String[]{"OP_G", "SZ_MDQ"});
        operand_dict.put("Gd", new String[]{"OP_G", "SZ_D"});
        operand_dict.put("Gx", new String[]{"OP_G", "SZ_MDQ"});
        operand_dict.put("Gz", new String[]{"OP_G", "SZ_Z"});
        operand_dict.put("M", new String[]{"OP_M", "SZ_NA"});
        operand_dict.put("Mb", new String[]{"OP_M", "SZ_B"});
        operand_dict.put("Mw", new String[]{"OP_M", "SZ_W"});
        operand_dict.put("Ms", new String[]{"OP_M", "SZ_W"});
        operand_dict.put("Md", new String[]{"OP_M", "SZ_D"});
        operand_dict.put("Mq", new String[]{"OP_M", "SZ_Q"});
        operand_dict.put("Mt", new String[]{"OP_M", "SZ_T"});
        operand_dict.put("I1", new String[]{"OP_I1", "SZ_NA"});
        operand_dict.put("I3", new String[]{"OP_I3", "SZ_NA"});
        operand_dict.put("Ib", new String[]{"OP_I", "SZ_B"});
        operand_dict.put("Isb", new String[]{"OP_I", "SZ_SB"});
        operand_dict.put("Iw", new String[]{"OP_I", "SZ_W"});
        operand_dict.put("Iv", new String[]{"OP_I", "SZ_V"});
        operand_dict.put("Iz", new String[]{"OP_I", "SZ_Z"});
        operand_dict.put("Jv", new String[]{"OP_J", "SZ_V"});
        operand_dict.put("Jz", new String[]{"OP_J", "SZ_Z"});
        operand_dict.put("Jb", new String[]{"OP_J", "SZ_B"});
        operand_dict.put("R", new String[]{"OP_R", "SZ_RDQ"});
        operand_dict.put( "C", new String[]{"OP_C", "SZ_NA"});
        operand_dict.put("D", new String[]{"OP_D", "SZ_NA"});
        operand_dict.put("S", new String[]{"OP_S", "SZ_NA"});
        operand_dict.put("Ob", new String[]{"OP_O", "SZ_B" });
        operand_dict.put("Ow", new String[]{"OP_O", "SZ_W" });
        operand_dict.put("Ov", new String[]{"OP_O", "SZ_V" });
        operand_dict.put("V", new String[]{"OP_V", "SZ_NA"});
        operand_dict.put("W", new String[]{"OP_W", "SZ_NA"});
        operand_dict.put("P", new String[]{"OP_P", "SZ_NA"});
        operand_dict.put("Q", new String[]{"OP_Q", "SZ_NA"});
        operand_dict.put("VR", new String[]{"OP_VR", "SZ_NA"});
        operand_dict.put("PR", new String[]{"OP_PR", "SZ_NA"});
        operand_dict.put("AL", new String[]{"OP_AL", "SZ_NA"});
        operand_dict.put("CL", new String[]{"OP_CL", "SZ_NA"});
        operand_dict.put("DL", new String[]{"OP_DL", "SZ_NA"});
        operand_dict.put("BL", new String[]{"OP_BL", "SZ_NA"});
        operand_dict.put("AH", new String[]{"OP_AH", "SZ_NA"});
        operand_dict.put("CH", new String[]{"OP_CH", "SZ_NA"});
        operand_dict.put("DH", new String[]{"OP_DH", "SZ_NA"});
        operand_dict.put("BH", new String[]{"OP_BH", "SZ_NA"});
        operand_dict.put("AX", new String[]{"OP_AX", "SZ_NA"});
        operand_dict.put("CX", new String[]{"OP_CX", "SZ_NA"});
        operand_dict.put("DX", new String[]{"OP_DX", "SZ_NA"});
        operand_dict.put("BX", new String[]{"OP_BX", "SZ_NA"});
        operand_dict.put("SI", new String[]{"OP_SI", "SZ_NA"});
        operand_dict.put("DI", new String[]{"OP_DI", "SZ_NA"});
        operand_dict.put("SP", new String[]{"OP_SP", "SZ_NA"});
        operand_dict.put("BP", new String[]{"OP_BP", "SZ_NA"});
        operand_dict.put("eAX", new String[]{"OP_eAX", "SZ_NA"});
        operand_dict.put("eCX", new String[]{"OP_eCX", "SZ_NA"});
        operand_dict.put("eDX", new String[]{"OP_eDX", "SZ_NA"});
        operand_dict.put("eBX", new String[]{"OP_eBX", "SZ_NA"});
        operand_dict.put("eSI", new String[]{"OP_eSI", "SZ_NA"});
        operand_dict.put("eDI", new String[]{"OP_eDI", "SZ_NA"});
        operand_dict.put("eSP", new String[]{"OP_eSP", "SZ_NA"});
        operand_dict.put("eBP", new String[]{"OP_eBP", "SZ_NA"});
        operand_dict.put("rAX", new String[]{"OP_rAX", "SZ_NA"});
        operand_dict.put("rCX", new String[]{"OP_rCX", "SZ_NA"});
        operand_dict.put("rBX", new String[]{"OP_rBX", "SZ_NA"});
        operand_dict.put("rDX", new String[]{"OP_rDX", "SZ_NA"});
        operand_dict.put("rSI", new String[]{"OP_rSI", "SZ_NA"});
        operand_dict.put("rDI", new String[]{"OP_rDI", "SZ_NA"});
        operand_dict.put("rSP", new String[]{"OP_rSP", "SZ_NA"});
        operand_dict.put("rBP", new String[]{"OP_rBP", "SZ_NA"});
        operand_dict.put("ES", new String[]{"OP_ES", "SZ_NA"});
        operand_dict.put("CS", new String[]{"OP_CS", "SZ_NA"});
        operand_dict.put("DS", new String[]{"OP_DS", "SZ_NA"});
        operand_dict.put("SS", new String[]{"OP_SS", "SZ_NA"});
        operand_dict.put("GS", new String[]{"OP_GS", "SZ_NA"});
        operand_dict.put("FS", new String[]{"OP_FS", "SZ_NA"});
        operand_dict.put("ST0", new String[]{"OP_ST0", "SZ_NA"});
        operand_dict.put("ST1", new String[]{"OP_ST1", "SZ_NA"});
        operand_dict.put("ST2", new String[]{"OP_ST2", "SZ_NA"});
        operand_dict.put("ST3", new String[]{"OP_ST3", "SZ_NA"});
        operand_dict.put("ST4", new String[]{"OP_ST4", "SZ_NA"});
        operand_dict.put("ST5", new String[]{"OP_ST5", "SZ_NA"});
        operand_dict.put("ST6", new String[]{"OP_ST6", "SZ_NA"});
        operand_dict.put("ST7", new String[]{"OP_ST7", "SZ_NA"});
        operand_dict.put(null  , new String[]{"OP_NONE", "SZ_NA"});
        operand_dict.put("ALr8b" , new String[]{"OP_ALr8b", "SZ_NA"});
        operand_dict.put("CLr9b" , new String[]{"OP_CLr9b", "SZ_NA"});
        operand_dict.put("DLr10b", new String[]{"OP_DLr10b", "SZ_NA"});
        operand_dict.put("BLr11b", new String[]{"OP_BLr11b", "SZ_NA"});
        operand_dict.put("AHr12b", new String[]{"OP_AHr12b", "SZ_NA"});
        operand_dict.put("CHr13b", new String[]{"OP_CHr13b", "SZ_NA"});
        operand_dict.put("DHr14b", new String[]{"OP_DHr14b", "SZ_NA"});
        operand_dict.put("BHr15b", new String[]{"OP_BHr15b", "SZ_NA"});
        operand_dict.put("rAXr8" , new String[]{"OP_rAXr8", "SZ_NA"});
        operand_dict.put("rCXr9" , new String[]{"OP_rCXr9", "SZ_NA"});
        operand_dict.put("rDXr10", new String[]{"OP_rDXr10", "SZ_NA"});
        operand_dict.put("rBXr11", new String[]{"OP_rBXr11", "SZ_NA"});
        operand_dict.put("rSPr12", new String[]{"OP_rSPr12", "SZ_NA"});
        operand_dict.put("rBPr13", new String[]{"OP_rBPr13", "SZ_NA"});
        operand_dict.put("rSIr14", new String[]{"OP_rSIr14", "SZ_NA"});
        operand_dict.put("rDIr15", new String[]{"OP_rDIr15", "SZ_NA"});
        operand_dict.put("jWP", new String[]{"OP_J", "SZ_WP"});
        operand_dict.put("jDP", new String[]{"OP_J", "SZ_DP"});  
        pfx_dict.put("aso", "P_aso");   
        pfx_dict.put("oso", "P_oso");   
        pfx_dict.put("rexw", "P_rexw"); 
        pfx_dict.put("rexb", "P_rexb");  
        pfx_dict.put("rexx", "P_rexx");  
        pfx_dict.put("rexr", "P_rexr");
        pfx_dict.put("inv64", "P_inv64"); 
        pfx_dict.put("def64", "P_def64"); 
        pfx_dict.put("depM", "P_depM");
        pfx_dict.put("cast1", "P_c1");    
        pfx_dict.put("cast2", "P_c2");    
        pfx_dict.put("cast3", "P_c3"); 
    }

    public static List<String> mnm_list = new ArrayList();

    public static void main(String[] args) throws IOException
    {
        Document dom = parseXML();
        NodeList list = dom.getElementsByTagName("instruction");
        Map<String, Map<String, Map<String, Object>>> tables = new HashMap();
        Map<String, Integer> table_sizes = new HashMap();
        for (int i=0; i < list.getLength(); i++)
        {
            Node n = list.item(i);
            String mnemonic = n.getAttributes().getNamedItem("mnemonic").getNodeValue();
            //System.out.println(mnemonic);
            if (mnm_list.contains(mnemonic))
                throw new IllegalStateException("Multiple opcode definition for "+mnemonic);
            mnm_list.add(mnemonic);
            String iclass = "";
            String vendor = "";
            NodeList children = n.getChildNodes();
            for (int j=0; j < children.getLength(); j++)
            {
                Node c = children.item(j);
                if (!(c instanceof Element))
                    continue;
                if (c.getNodeName().equals("vendor"))
                    vendor = c.getTextContent().trim();
                if (c.getNodeName().equals("class"))
                    iclass = c.getTextContent().trim();
            }
            
            // get each opcode definition
            for (int j=0; j < children.getLength(); j++)
            {
                Node c = children.item(j);
                if (!c.getNodeName().equals("opcode"))
                    continue;
                String opcode = c.getTextContent();
                String[] parts = opcode.split(";");
                List<String> flags = new ArrayList();
                String[] opc;
                String[] opr = new String[0];
                String[] pfx = new String[0];
                List<String> pfx_c = new ArrayList();

                Node cast = c.getAttributes().getNamedItem("cast");
                if (null != cast)
                    pfx_c.add("P_c"+cast.getNodeValue());

                Node imp = c.getAttributes().getNamedItem("imp_addr");
                if ((null != imp) && (Integer.parseInt(imp.getNodeValue())!=0))
                    pfx_c.add("P_ImpAddr");

                Node mode = c.getAttributes().getNamedItem("mode");
                if (null != mode)
                {
                    String[] modef = mode.getNodeValue().trim().split(" ");
                    for (int m = 0; m < modef.length; m++)
                        if (!pfx_dict.containsKey(modef[m]))
                            System.out.println("Warning: unrecognised mode attribute "+modef[m]);
                        else
                            pfx_c.add(pfx_dict.get(modef[m]));
                }

                // prefices, opcode bytes, operands
                if (parts.length == 1)
                    opc = parts[0].split(" ");
                else if (parts.length == 2)
                {
                    opc = parts[0].split(" ");
                    opr = parts[1].trim().split(" ");
                    for (int p=0; p < opc.length; p++)
                        if (pfx_dict.containsKey(opc[p]))
                        {
                            pfx = parts[0].split(" ");
                            opc = parts[1].split(" ");
                            break;
                        }
                }
                else if (parts.length == 3)
                {
                    pfx = parts[0].trim().split(" ");
                    opc = parts[1].trim().split(" ");
                    opr = parts[2].trim().split(" ");
                }
                else
                    throw new IllegalStateException("Invalid opcode definition for "+mnemonic);
                for (int k=0; k < opc.length; k++)
                    opc[k] = opc[k].toUpperCase();

                if (mnemonic.equals("pause") || (mnemonic.equals("nop") && opc[0].equals("90")) || mnemonic.equals("invalid") || mnemonic.equals("db"))
                    continue;

                // prefix
                for (int k=0; k < pfx.length; k++)
                {
                    if ((pfx[k].length()>0) && !pfx_dict.containsKey(pfx[k]))
                        System.out.println("Error: invalid prefix specification: "+pfx[k]);
                    if (pfx[k].trim().length() > 0)
                        pfx_c.add(pfx_dict.get(pfx[k]));
                }
                if (pfx.length == 0 || ((pfx.length == 1) && pfx[0].trim().length()==0))
                    pfx_c.add("P_none");
                pfx = pfx_c.toArray(new String[0]);

                // operands
                String[] opr_c = new String[]{"O_NONE", "O_NONE", "O_NONE"};
                for (int k=0; k < opr.length; k++)
                {
                    if ((opr[k].length()>0) && !operand_dict.containsKey(opr[k]))
                        System.out.println("Error: Invalid operand "+opr[k]);
                    if (opr[k].trim().length() == 0)
                        opr[k] = "NONE";
                    opr_c[k]= "O_"+opr[k];
                }
                opr = new String[]{String.format("%-8s %-8s %s", opr_c[0]+",", opr_c[1]+",", opr_c[2])};

                String table_sse = "";
                String table_name = "itab__1byte";
                int table_size = 256;
                String table_index = "";

                for (int k=0; k < opc.length; k++)
                {
                    String op = opc[k];
                    if (op.startsWith("SSE"))
                        table_sse = op;
                    else if (op.equals("0F") && (table_sse.length()>0))
                    {
                        table_name = "itab__pfx_"+table_sse+"__0f";
                        table_size = 256;
                        table_sse = "";
                    }
                    else if (op.equals("0F"))
                    {
                        table_name = "itab__0f";
                        table_size = 256;
                    }
                    else if (op.startsWith("/X87="))
                    {
                        Map tmp = new HashMap();
                        tmp.put("type", "grp_x87");
                        tmp.put("name", table_name+"__op_"+table_index+"__x87");
                        tables.get(table_name).put(table_index, tmp);
                        table_name  = (String) tables.get(table_name).get(table_index).get("name");
                        table_index = String.format("%02X", Integer.parseInt(op.substring(5, 7), 16));
                        table_size = 64;
                    }
                    else if (op.startsWith("/RM="))
                    {
                        Map tmp = new HashMap();
                        tmp.put("type", "grp_rm");
                        tmp.put("name", table_name+"__op_"+table_index+"__rm");
                        tables.get(table_name).put(table_index, tmp);
                        table_name  = (String) tables.get(table_name).get(table_index).get("name");
                        table_index = String.format("%02X", Integer.parseInt(op.substring(4, op.length()), 16));
                        table_size = 8;
                    }
                    else if (op.startsWith("/MOD="))
                    {
                        Map tmp = new HashMap();
                        tmp.put("type", "grp_mod");
                        tmp.put("name", table_name+"__op_"+table_index+"__mod");
                        tables.get(table_name).put(table_index, tmp);
                        table_name  = (String) tables.get(table_name).get(table_index).get("name");
                        String v = op.substring(5, 7);
                        if (op.length() == 8)
                            v = op.substring(5, 8);
                        
                        if (v.equals("!11"))
                            table_index = "00";
                        else if (v.equals("11"))
                            table_index = "01";
                        table_size  = 2;
                    }
                    else if (op.startsWith("/O"))
                    {
                        Map tmp = new HashMap();
                        tmp.put("type", "grp_osize");
                        tmp.put("name", table_name+"__op_"+table_index+"__osize");
                        tables.get(table_name).put(table_index, tmp);
                        table_name  = (String) tables.get(table_name).get(table_index).get("name");
                        table_index = mode_dict.get(op.substring(2,4));
                        table_size  = 3;
                    }
                    else if (op.startsWith("/A"))
                    {
                        Map tmp = new HashMap();
                        tmp.put("type", "grp_asize");
                        tmp.put("name", table_name+"__op_"+table_index+"__asize");
                        tables.get(table_name).put(table_index, tmp);
                        table_name  = (String) tables.get(table_name).get(table_index).get("name");
                        table_index = mode_dict.get(op.substring(2, 4));
                        table_size  = 3;
                    }
                    else if (op.startsWith("/M"))
                    {
                        Map tmp = new HashMap();
                        tmp.put("type", "grp_mode");
                        tmp.put("name", table_name+"__op_"+table_index+"__mode");
                        tables.get(table_name).put(table_index, tmp);
                        table_name  = (String) tables.get(table_name).get(table_index).get("name");
                        table_index = mode_dict.get(op.substring(2, 4));
                        table_size  = 3;
                    }
                    else if (op.startsWith("/3DNOW"))
                    {
                        table_name  = "itab__3dnow";
                        table_size  = 256;
                    }
                    else if (op.startsWith("/"))
                    {
                        Map tmp = new HashMap();
                        tmp.put("type", "grp_reg");
                        tmp.put("name", table_name+"__op_"+table_index+"__reg");
                        tables.get(table_name).put(table_index, tmp);
                        table_name  = (String) tables.get(table_name).get(table_index).get("name");
                        table_index = String.format("%02X", Integer.parseInt(op.substring(1, 2)));
                        table_size  = 8;
                    }
                    else
                        table_index = op;

                    if (!tables.containsKey(table_name))
                    {
                        tables.put(table_name, new HashMap());
                        table_sizes.put(table_name, table_size);
                    }
                }
                if (vendor.length()>0)
                {
                    Map tmp = new HashMap();
                    tmp.put("type", "grp_vendor");
                    tmp.put("name", table_name+"__op_"+table_index+"__vendor");
                        tables.get(table_name).put(table_index, tmp);
                    table_name  = (String) tables.get(table_name).get(table_index).get("name");
                    table_index = vend_dict.get(vendor);
                    table_size = 2;
                    if (!tables.containsKey(table_name))
                    {
                        tables.put(table_name, new HashMap());
                        table_sizes.put(table_name, table_size);
                    }
                }

                Map<String, Object> leaf = new HashMap();
                leaf.put("type", "leaf");
                leaf.put("name", mnemonic);
                String pfx_string = "";
                if (pfx.length >0)
                {
                    pfx_string = pfx[0];
                    for (int cc = 1; cc < pfx.length; cc++)
                        pfx_string += "|"+pfx[cc];
                }
                leaf.put("pfx", pfx_string);
                String opr_string = "";
                if (opr.length >0)
                {
                    opr_string = opr[0];
                    for (int cc=1; cc < opr.length; cc++)
                        opr_string += "|"+opr[cc];
                }
                leaf.put("opr", opr_string);
                leaf.put("flags", flags);
                tables.get(table_name).put(table_index, leaf);
            }
        }

        // now print to file
        BufferedWriter w = new BufferedWriter(new FileWriter("com/Table.java"));
        w.write("package com;\n\n");
        w.write("import java.util.*;\n");
        w.write("import static com.ZygoteOperand.*;\n\n");
        w.write("public class Table\n{\n");
        
        w.write("\n");
        w.write("public static final int ITAB__VENDOR_INDX__AMD = 0;\n");
        w.write("public static final int ITAB__VENDOR_INDX__INTEL = 1;\n");

        w.write("\n");
        w.write("public static final int ITAB__MODE_INDX__16 = 0;\n");
        w.write("public static final int ITAB__MODE_INDX__32 = 1;\n");
        w.write("public static final int ITAB__MODE_INDX__64 = 2;\n");

        w.write("\n");
        w.write("public static final int ITAB__MOD_INDX__NOT_11 = 0;\n");
        w.write("public static final int ITAB__MOD_INDX__11 = 1;\n");

        // Generate enumeration of the tables
        List<String> table_names = new ArrayList();
        table_names.addAll(tables.keySet());
        Collections.sort(table_names);
        w.write("\n");
        int h=0;
        for (String name: table_names)
        {
            w.write("public static final int "+name.toUpperCase() + " = "+h+";\n");
            h++;
        }

        // Generate operators list
        w.write("\npublic static List<String> operator = Arrays.asList(new String[]{\n");
        for (String m: mnm_list)
            w.write("  \""+m+"\",\n");
        w.write("});\n\n");

        w.write("\npublic static List<String> operator_spl = Arrays.asList(new String[]{\n");
        for (String m: spl_mnm_types)
            w.write("  \""+m+"\",\n");
        w.write("});\n\n");

        w.write("\npublic static List<String> operators_str = Arrays.asList(new String[]{\n");
        for (String m: mnm_list)
            w.write("  \""+m+"\",\n");
        w.write("});\n\n");

        // Generate instruction tables
        for (String t:table_names)
        {
            w.write("private ZygoteInstruction[] "+t.toLowerCase()+" = new ZygoteInstruction[]{\n");
            for (int i=0; i < table_sizes.get(t); i++)
            {
                String index = String.format("%02X", i);
                HashMap tmp = new HashMap();
                tmp.put("type", "invalid");
                if (tables.get(t).containsKey(index))
                    w.write(centry(index, (Map)tables.get(t).get(index)));
                else
                    w.write(centry(index, tmp));
            }
            w.write("};\n");
        }
        
        w.write("\n// the order of this table matches itab_index ");
        w.write("\npublic ZygoteInstruction[][] itab_list = new ZygoteInstruction[][]{\n");
        for (String name: table_names)
            w.write("    "+name.toLowerCase()+",\n");
        w.write("};\n");
        w.write("}\n");
        w.flush();
        w.close();
    }
    
    private static String centry(String i, Map<String, Object> defmap)
    {
        String opr, mnm, pfx;
        if (((String)defmap.get("type")).substring(0, 3).equals("grp"))
        {
            opr = default_opr;
            mnm = "\""+((String)defmap.get("type")).toLowerCase()+"\"";
            pfx = ((String)defmap.get("name")).toUpperCase();
        }
        else if (((String)defmap.get("type")).equals("leaf"))
        {
            mnm = "\""+((String)defmap.get("name")).toLowerCase()+"\"";
            opr = (String) defmap.get("opr");
            pfx = (String) defmap.get("pfx");
            if (mnm.length() == 0)
                mnm = "\'na\'";
            if (opr.length() == 0)
                opr = default_opr;
            if ((pfx == null) || (pfx.length() == 0))
                pfx = "P_none";
        }
        else
        {
            opr = default_opr;
            mnm = "\"invalid\"";
            pfx = "P_none";
        }
        return String.format("  new ZygoteInstruction( %-16s %-26s %s ),\n", mnm+",", opr+",", pfx);
    }

    public static Document parseXML()
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse("x86optable.xml");
        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch(SAXException se) {
            se.printStackTrace();
        }catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }
}