package de.bb.jcc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.bb.jcc.ConstantPool.Entry;
import de.bb.util.Pair;
import de.bb.util.SingleMap;

/**
 * Reflects a Java method. To create or manipulate code.
 * 
 * @author bebbo
 */
public class Code {
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    ConstantPool cp;

    private ArrayList<Ex> exceptions = new ArrayList<Ex>();

    private ArrayList<Goto> gotos = new ArrayList<Goto>();

    private HashMap<String, Goto> labels = new HashMap<String, Goto>();

    private int maxLocal = 1;

    private int maxStack = 0, curStack = 0;

    private int[][] lineNumberTable;

    ArrayList<Instruction> instructions = new ArrayList<Instruction>();

    SingleMap<Integer, Pair<String, String>> localsMap = new SingleMap<Integer, Pair<String, String>>();

    int localCount;

    private DC dc;

    ClassDefinition cd;

    Code(ConstantPool cp) {
        this.cp = cp;
    }

    /**
     * @param label
     */
    private int addGoto(String label) {
        if (label.length() == 0)
            throw new RuntimeException("empty label");
        gotos.add(new Goto(true, false, label, bos.size(), curStack));

        return getOffset(label);
    }

    /**
     * Insert code at the head of this function.
     * 
     * @param insertedCode
     * @throws IOException
     */
    public void addHead(Code insertedCode) throws IOException {
        ByteArrayOutputStream merged = new ByteArrayOutputStream();
        merged.write(insertedCode.bos.toByteArray());

        // update all goto marks
        for (Iterator<Goto> i = gotos.iterator(); i.hasNext();) {
            Goto g = i.next();
            g.offset += merged.size();
        }

        gotos.addAll(insertedCode.gotos);
        labels.putAll(insertedCode.labels);

        if (insertedCode.maxStack > maxStack)
            maxStack = insertedCode.maxStack;
        if (insertedCode.maxLocal > maxLocal)
            maxLocal = insertedCode.maxLocal;

        merged.write(bos.toByteArray());
        bos = merged;

    }

    /**
     * @param label
     * @return true if the label was not already defined.
     */
    public boolean addLabel(String label) {
        boolean fix = false;
        if (gotos.size() > 0) {
            Goto g = gotos.get(gotos.size() - 1);
            if (g.isGoto && g.isUnconditional) {
                fix = g.offset + 3 == bos.size();
            }
        }
        Goto g = new Goto(false, fix, label, bos.size(), curStack);
        gotos.add(g);
        return null == labels.put(label, g);
    }

    /**
     * walk through the provided binary code and create - an instruction for
     * each instruction - a label for each location jumped to - a label for each
     * entry point, that is - code start - after each jump - try block end - a
     * try if a try catch block starts - a catch at start of the catch block
     * 
     * @param code
     *            the binary code
     * @param exx
     * @throws IOException
     */
    public void assignCode(byte[] code, int[][] exx) throws IOException {
        bos.reset();
        bos.write(code);

        HashMap<Integer, Instruction> labels = new HashMap<Integer, Instruction>();
        for (int i = 0; i < exx.length; ++i) {
            int[] ex = exx[i];
            Integer lstart = new Integer(ex[0]);
            Integer lend = new Integer(ex[1]);
            Integer lcatch = new Integer(ex[2]);

            Label label = (Label) labels.get(lstart);
            Catch iCatch;
            if (label == null)
                iCatch = new Catch(ex[3], labels.size(), cp);
            else
                iCatch = new Catch(ex[3], label.no, cp);
            labels.put(lcatch, iCatch); // catch start

            label = (Label) labels.get(lstart);
            Instruction iTry;
            if (label == null)
                iTry = new Try(iCatch, labels.size());
            else
                iTry = new Try(iCatch, label.no);
            labels.put(lstart, iTry); // try start

            label = (Label) labels.get(lend);
            if (label == null)
                label = new Label(labels.size());
            labels.put(lend, label); // try end
        }

        // first determine all labels.
        for (int i = 0; i < code.length;) {
            int opcode = 0xff & code[i];
            int size = Instruction.CODESIZETABLE[opcode];
            // handle wide
            if (opcode == 196) {
                opcode = 0xff & code[i + 1];
                size = Instruction.CODESIZETABLE[opcode];
                size += (size - 1) * 2;
            }

            switch (opcode) {
            // variants of cmp
            case 0x99:
            case 0x9a:
            case 0x9b:
            case 0x9c:
            case 0x9d:
            case 0x9e:
            case 0x9f:
            case 0xa0:
            case 0xa1:
            case 0xa2:
            case 0xa3:
            case 0xa4:
            case 0xa5:
            case 0xa6:
                // goto
            case 0xa7:
                // jsr
            case 0xa8:
                // cmp null / not null
            case 0xc6:
            case 0xc7: {
                int offset;
                if (size == 3)
                    offset = Util.toInt2(code, i + 1);
                else
                    offset = Util.toInt4(code, i + 2);
                Integer l;
                l = new Integer(i + size);
                Label lab = (Label) labels.get(l);
                if (lab == null)
                    labels.put(l, new Label(labels.size())); // label behind
                                                             // jump instruction
                l = new Integer(i + offset);
                lab = (Label) labels.get(l);
                if (lab == null)
                    labels.put(l, new Label(labels.size())); // label to jump to
            }
                break;

            // handle tableswitch (170)
            // tableswitch Ldefault, lobyte, hibyte, L1, ...
            case 170: {
                int j = (i + 4) & ~3;
                int def = Util.toInt4(code, j) + i;
                Integer l = new Integer(def);
                Label lab = (Label) labels.get(l);
                if (lab == null)
                    labels.put(l, new Label(labels.size())); // default label
                int lo = Util.toInt4(code, j += 4);
                int hi = Util.toInt4(code, j += 4);
                int count = hi - lo + 1;
                while (count-- > 0) {
                    int pos = Util.toInt4(code, j += 4) + i;
                    l = new Integer(pos);
                    lab = (Label) labels.get(l);
                    if (lab == null)
                        labels.put(l, new Label(labels.size())); // case label
                }
                size = j + 4 - i;
            }
                break;
            // and lookupswitch (171)
            // lookupswitch Ldefault, (value, L1), ...
            case 171: {
                int j = (i + 4) & ~3;
                int def = Util.toInt4(code, j) + i;
                Integer l = new Integer(def);
                Label lab = (Label) labels.get(l);
                if (lab == null)
                    labels.put(l, new Label(labels.size())); // default label
                int count = Util.toInt4(code, j += 4);
                while (count-- > 0) {
                    // int val =
                    Util.toInt4(code, j += 4);
                    int pos = Util.toInt4(code, j += 4) + i;
                    l = new Integer(pos);
                    lab = (Label) labels.get(l);
                    if (lab == null)
                        labels.put(l, new Label(labels.size())); // case label
                }
                size = j + 4 - i;
            }
                break;
            }
            i += size;
        }

        // resolve the code into separate instructions
        for (int i = 0; i < code.length;) {
            Integer l = new Integer(i);
            Instruction ins = labels.get(l);
            if (ins != null) {
                instructions.add(ins);
            }
            int opcode = 0xff & code[i];
            int size = Instruction.CODESIZETABLE[opcode];
            if (size == 0 && opcode != 170)
                System.err.println("sz == 0");
            // handle wide
            boolean isWide = opcode == 196;
            if (isWide) {
                opcode = 0xff & code[i + 1];
                size = Instruction.CODESIZETABLE[opcode];
                size += (size - 1) * 2;
            }

            switch (opcode) {
            // variants of cmp
            case 0x99:
            case 0x9a:
            case 0x9b:
            case 0x9c:
            case 0x9d:
            case 0x9e:
            case 0x9f:
            case 0xa0:
            case 0xa1:
            case 0xa2:
            case 0xa3:
            case 0xa4:
            case 0xa5:
            case 0xa6:
                // goto
            case 0xa7:
                // jsr
            case 0xa8:
                // cmp null / not null
            case 0xc6:
            case 0xc7: {
                int offset;
                if (size == 3)
                    offset = Util.toInt2(code, i + 1);
                else
                    offset = Util.toInt4(code, i + 2);
                l = new Integer(i + offset);
                ins = new Jump(opcode, (Label) labels.get(l));
                instructions.add(ins);
            }
                break;

            // handle tableswitch (170)
            // tableswitch Ldefault, offset, L1, ...
            case 170: {
                int j = (i + 4) & ~3;
                int def = Util.toInt4(code, j) + i;
                int lo = Util.toInt4(code, j += 4);
                int hi = Util.toInt4(code, j += 4);
                TableSwitch ts = new TableSwitch(lo, hi, (Label) labels.get(new Integer(def)));
                int count = hi - lo + 1;
                while (count-- > 0) {
                    int pos = Util.toInt4(code, j += 4) + i;
                    ts.addCase((Label) labels.get(new Integer(def)));
                }
                instructions.add(ts);
                size = j + 4 - i;
            }
                break;
            // and lookupswitch (171)
            // lookupswitch Ldefault, (value, L1), ...
            case 171: {
                int j = (i + 4) & ~3;
                int def = Util.toInt4(code, j) + i;
                LookupSwitch ls = new LookupSwitch((Label) labels.get(new Integer(def)));
                int count = Util.toInt4(code, j += 4);
                while (count-- > 0) {
                    int val = Util.toInt4(code, j += 4);
                    int pos = Util.toInt4(code, j += 4) + i;
                    ls.addCase(val, (Label) labels.get(new Integer(pos)));
                }
                instructions.add(ls);
                size = j + 4 - i;
            }
                break;
            default:
                if (Instruction.USESCONSTANT[opcode]) {
                    int index;
                    switch (opcode) {
                    case 0x12: // ldc
                        index = 0xff & code[i + 1];
                        break;
                    default:
                        index = Util.toInt2(code, i + 1) & 0xffff;
                    }

                    // invoke interface
                    if (opcode >= 0xb6 && opcode <= 0xba) {
                        ins = new CallInstruction(opcode, index, cp);
                    } else {
                        ins = new ConstantInstruction(opcode, index, cp);
                    }
                    instructions.add(ins);
                    break;
                }

                if (opcode == 0xbc) { // newarray
                    ins = new NewArray(code[i + 1] - 4);
                    instructions.add(ins);
                    break;
                }

                if (opcode == 0x84) {
                    int val1, val2;

                    if (isWide) {
                        val1 = Util.toInt2(code, i + 2);
                        val2 = Util.toInt2(code, i + 4);
                    } else {
                        val1 = code[i + 1] & 0xff;
                        val2 = code[i + 2] & 0xff;
                    }
                    ins = new Instruction2Param(opcode, val1, val2);
                    instructions.add(ins);
                    break;
                }

                int offset = i + 1;
                int iSize = size - 1;
                if (isWide) {
                    ++offset;
                    --iSize;
                }
                if (iSize > 0) {
                    if (Instruction.NAME[opcode] != null) {
                        int val1;

                        if (isWide) {
                            val1 = Util.toInt2(code, i + 2);
                        } else {
                            val1 = code[i + 1] & 0xff;
                        }
                        ins = new Instruction1Param(opcode, val1);
                        instructions.add(ins);
                        break;
                    }

                    ins = new SimpleInstruction(opcode, code, offset, iSize);
                } else {
                    ins = new Instruction(opcode);
                }
                instructions.add(ins);
                break;
            }
            i += size;
        }
    }

    public void addTryCatch(int startPc, int endPc, int handlerPc, int exceptionConstantIndex) {
        // TODO: store the try catch block information.
    }

    /**
     * Method c_aaload.
     */
    public void c_aaload() {
        dec();
        bos.write(0x32);
    }

    /**
     * Method c_aastore.
     */
    public void c_aastore() {
        dec3();
        bos.write(0x53);
    }

    /**
     * push null.
     */
    public void c_aconst_null() {
        inc();
        bos.write(0x1);
    }

    /**
     * load reference from variables.
     * 
     * @param n
     */
    public void c_aload(int n) {
        if (n > maxLocal)
            maxLocal = n;

        inc();
        switch (n) {
        case 0:
        case 1:
        case 2:
        case 3:
            bos.write(0x2a + n);
            break;
        default:
            if (n > 255)
                bos.write(0xc4); // wide
            bos.write(0x19); // aload
            if (n > 255)
                bos.write(n >>> 8); // high byte
            bos.write(n);
        }
    }

    /**
     * Method c_anewarray.
     * 
     * @param cName
     */
    public void c_anewarray(String cName) {
        if (cName.length() == 1) {
            int i = "ZCFDBSIJ".indexOf(cName.charAt(0));
            if (i >= 0) {
                bos.write(0xbc);
                bos.write(4 + i);
                return;
            }
        }

        int n = cp.addClass(cName);
        bos.write(0xbd);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * Method c_areturn.
     */
    public void c_areturn() {
        dec();
        bos.write(0xb0);
    }

    /**
     * get length from array.
     * 
     */
    public void c_arraylength() {
        bos.write(0xbe);
    }

    /**
     * Method c_astore.
     * 
     * @param i
     */
    public void c_astore(int i) {
        if (i > maxLocal)
            maxLocal = i;
        dec();
        if (i >= 0 && i <= 3) {
            bos.write(i + 0x75);
        } else if (i < 256) {
            bos.write(0x3a);
            bos.write(i);
        } else {
            bos.write(0xc4); // wide
            bos.write(0x3a);
            bos.write(i >>> 8);
            bos.write(i);
        }
    }

    /**
     * Method c_athrow.
     */
    public void c_athrow() {
        dec();
        bos.write(0xbf);
    }

    /**
     * load byte or boolean from array,index
     */
    public void c_baload() {
        dec();
        bos.write(0x33);
    }

    /**
     * store byte or boolean into array,index,value
     */
    public void c_bastore() {
        dec2();
        bos.write(0x54);
    }

    /**
     * load char from array,index
     */
    public void c_caload() {
        dec();
        bos.write(0x34);
    }

    /**
     * store char into array,index,value
     */
    public void c_castore() {
        dec2();
        bos.write(0x55);
    }

    /**
     * Method c_checkcast.
     * 
     * @param cName
     */
    public void c_checkcast(String cName) {
        int n = cp.addClass(cName);
        bos.write(0xc0);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * convert double to float.
     */
    public void c_d2f() {
        dec();
        bos.write(0x90);
    }

    /**
     * convert double to int.
     */
    public void c_d2i() {
        dec();
        bos.write(0x8e);
    }

    /**
     * convert double to long.
     */
    public void c_d2l() {
        bos.write(0x8f);
    }

    /**
     * add double + double.
     */
    public void c_dadd() {
        dec2();
        bos.write(0x63);
    }

    /**
     * load double from array.
     */
    public void c_daload() {
        bos.write(0x31);
    }

    /**
     * store double into array.
     */
    public void c_dastore() {
        dec4();
        bos.write(0x52);
    }

    /**
     * compare 2 double - push int
     */
    public void c_dcmpg() {
        dec3();
        bos.write(0x98);
    }

    /**
     * compare 2 double - push int
     */
    public void c_dcmpl() {
        dec3();
        bos.write(0x97);
    }

    /**
     * push a double const
     * 
     * @param d
     */
    public void c_dconst(double d) {
        inc2();
        if (d == 0d) {
            bos.write(0x0e);
            return;
        }
        if (d == 1d) {
            bos.write(0x0f);
            return;
        }
        int index = cp.addDouble(d);
        bos.write(0x14);
        bos.write(index >> 8);
        bos.write(index);
    }

    /**
     * double division.
     */
    public void c_ddiv() {
        dec2();
        bos.write(0x6f);
    }

    /**
     * Method c_dload.
     * 
     * @param pno
     */
    public void c_dload(int pno) {
        inc2();
        if (pno <= 3)
            bos.write(0x26 + pno);
        else {
            if (pno > 255)
                bos.write(0xc4); // wide
            bos.write(0x18);
            if (pno > 255)
                bos.write(pno >>> 8);
            bos.write(pno);
        }
    }

    /**
     * double mul.
     */
    public void c_dmul() {
        dec2();
        bos.write(0x6b);
    }

    /**
     * double negate.
     */
    public void c_dneg() {
        bos.write(0x77);
    }

    /**
     * double division remainder.
     */
    public void c_drem() {
        dec2();
        bos.write(0x73);
    }

    /**
     * Method c_lreturn.
     */
    public void c_dreturn() {
        dec2();
        bos.write(0xaf);
    }

    /**
     * Method c_dstore - store a double into var and var+1.
     * 
     * @param i
     */
    public void c_dstore(int i) {
        if (i + 1 > maxLocal)
            maxLocal = i + 1;
        dec2();
        if (i >= 0 && i <= 3) {
            bos.write(i + 0x47);
        } else if (i < 256) {
            bos.write(0x39);
            bos.write(i);
        } else {
            bos.write(0xc4); // wide
            bos.write(0x39);
            bos.write(i >>> 8);
            bos.write(i);
        }
    }

    /**
     * sub double - double.
     */
    public void c_dsub() {
        dec2();
        bos.write(0x67);
    }

    /**
     * Method dup.
     */
    public void c_dup() {
        inc();
        bos.write(0x59);
    }

    /**
     * Method c_dup_x1.
     */
    public void c_dup_x1() {
        inc();
        bos.write(0x5a);
    }

    /**
     * Method c_dup_x2.
     */
    public void c_dup_x2() {
        inc();
        bos.write(0x5b);
    }

    /**
     * Method dup2.
     */
    public void c_dup2() {
        inc2();
        bos.write(0x5c);
    }

    /**
     * Method c_dup_x1.
     */
    public void c_dup2_x1() {
        inc2();
        bos.write(0x5d);
    }

    /**
     * Method c_dup_x2.
     */
    public void c_dup2_x2() {
        inc2();
        bos.write(0x5e);
    }

    /**
     * convert float to double.
     */
    public void c_f2d() {
        inc();
        bos.write(0x8d);
    }

    /**
     * convert float to int.
     */
    public void c_f2i() {
        bos.write(0x8b);
    }

    /**
     * convert float to long.
     */
    public void c_f2l() {
        inc();
        bos.write(0x8c);
    }

    /**
     * add float + float.
     */
    public void c_fadd() {
        dec();
        bos.write(0x62);
    }

    /**
     * load float from array.
     */
    public void c_faload() {
        dec();
        bos.write(0x30);
    }

    /**
     * store float into array.
     */
    public void c_fastore() {
        dec3();
        bos.write(0x51);
    }

    /**
     * compare 2 float - push int
     */
    public void c_fcmpg() {
        dec();
        bos.write(0x96);
    }

    /**
     * compare 2 float - push int
     */
    public void c_fcmpl() {
        dec();
        bos.write(0x95);
    }

    /**
     * push a float value.
     * 
     * @param f
     */
    public void c_fconst(float f) {
        inc();
        if (f == 0.0) {
            bos.write(11);
            return;
        }
        if (f == 1.0) {
            bos.write(12);
            return;
        }
        if (f == 2.0) {
            bos.write(13);
            return;
        }

        int index = cp.addFloat(f);
        if (index < 256) {
            bos.write(0x12);
            bos.write(index);
            return;
        }
        bos.write(0xc4); // wide
        bos.write(12);
        bos.write(index >> 8);
        bos.write(index);
    }

    /**
     * float division.
     */
    public void c_fdiv() {
        dec();
        bos.write(0x6e);
    }

    /**
     * Method c_fload.
     * 
     * @param i
     */
    public void c_fload(int i) {
        inc();
        if (i <= 3)
            bos.write(0x34 + i);
        else {
            if (i > 255)
                bos.write(0xc4); // wide
            bos.write(0x17);
            if (i > 255)
                bos.write(i >>> 8);
            bos.write(i);
        }
    }

    /**
     * float mul.
     */
    public void c_fmul() {
        dec();
        bos.write(0x6a);
    }

    /**
     * float negate.
     */
    public void c_fneg() {
        bos.write(0x76);
    }

    /**
     * float division remainder.
     */
    public void c_frem() {
        dec();
        bos.write(0x72);
    }

    /**
     * Method c_freturn.
     */
    public void c_freturn() {
        dec();
        bos.write(0xae);
    }

    /**
     * Method c_fstore - store a float into variable.
     * 
     * @param i
     */
    public void c_fstore(int i) {
        if (i > maxLocal)
            maxLocal = i;
        dec();
        if (i >= 0 && i <= 3) {
            bos.write(i + 0x43);
        } else if (i < 256) {
            bos.write(0x38);
            bos.write(i);
        } else {
            bos.write(0xc4); // wide
            bos.write(0x38);
            bos.write(i >>> 8);
            bos.write(i);
        }
    }

    /**
     * sub float - float.
     */
    public void c_fsub() {
        dec();
        bos.write(0x66);
    }

    /**
     * Method c_getfield.
     * 
     * @param cName
     *            class Name
     * @param fName
     *            field Name
     * @param signature
     *            signature
     */
    public void c_getfield(String cName, String fName, String signature) {
        int l = signature.indexOf('L');
        if (l >= 0) {
            int k = signature.indexOf(';');
            cp.addClass(signature.substring(l + 1, k));
        }

        int n = cp.addField(cName, fName, signature);
        bos.write(0xb4);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * Method c_getstatic.
     * 
     * @param cName
     *            class Name
     * @param fName
     *            field Name
     * @param signature
     *            signature
     */
    public void c_getstatic(String cName, String fName, String signature) {
        inc();

        int l = signature.indexOf('L');
        if (l >= 0) {
            int k = signature.indexOf(';');
            cp.addClass(signature.substring(l + 1, k));
        }

        int n = cp.addField(cName, fName, signature);
        bos.write(0xb2);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * Add a goto to label.
     * 
     * @param label
     */
    public void c_goto(String label) {
        gotos.add(new Goto(true, true, label, bos.size(), curStack));
        int offset = getOffset(label);
        bos.write(0xa7);

        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * convert int to boolean.
     */
    public void c_i2b() {
        bos.write(0x91);
    }

    /**
     * convert int to char.
     */
    public void c_i2c() {
        bos.write(0x92);
    }

    /**
     * convert int to double.
     */
    public void c_i2d() {
        inc();
        bos.write(0x87);
    }

    /**
     * convert int to float.
     */
    public void c_i2f() {
        bos.write(0x86);
    }

    /**
     * convert int to long.
     */
    public void c_i2l() {
        inc();
        bos.write(0x85);
    }

    /**
     * convert int to short.
     */
    public void c_i2s() {
        bos.write(0x93);
    }

    /**
     * add int + int.
     */
    public void c_iadd() {
        dec();
        bos.write(0x60);
    }

    /**
     * load int from array.
     */
    public void c_iaload() {
        dec();
        bos.write(0x2e);
    }

    /**
     * int & int.
     */
    public void c_iand() {
        dec();
        bos.write(0x7e);
    }

    /**
     * store int into array.
     */
    public void c_iastore() {
        dec3();
        bos.write(0x4f);
    }

    /**
     * Methods c_bconst c_iconst ldc ldc_w.
     * 
     * @param i
     */
    public void c_iconst(int i) {
        inc();
        if (-1 <= i && i <= 5) {
            bos.write(0x3 + i);
            return;
        }

        if (-128 <= i && i <= 127) {
            bos.write(0x10);
            bos.write(i);
            return;
        }

        if (-32768 <= i && i <= 32767) {
            bos.write(0x11);
            bos.write(i >> 8);
            bos.write(i);
            return;
        }

        int index = cp.addInteger(i);
        if (index < 256) {
            bos.write(0x12);
            bos.write(index);
            return;
        }
        bos.write(0x13);
        bos.write(index >> 8);
        bos.write(index);
    }

    /**
     * int division.
     */
    public void c_idiv() {
        dec();
        bos.write(0x6c);
    }

    /**
     * @param label
     */
    public void c_if_acmpeq(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0xa5);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_if_acmpne(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0xa6);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_if_icmpeq(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0x9f);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * int with int and jump on condition.
     * 
     * @param label
     */
    public void c_if_icmpge(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0xa2);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * int with int and jump on condition.
     * 
     * @param label
     */
    public void c_if_icmpgt(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0xa3);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * int with int and jump on condition.
     * 
     * @param label
     */
    public void c_if_icmple(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0xa4);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * int with int and jump on condition.
     * 
     * @param label
     */
    public void c_if_icmplt(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0xa1);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * int with int and jump on condition.
     * 
     * @param label
     */
    public void c_if_icmpne(String label) {
        dec2();
        int offset = addGoto(label);
        bos.write(0xa0);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_ifeq(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0x99);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_ifge(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0x9c);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_ifgt(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0x9d);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_ifle(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0x9e);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_iflt(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0x9b);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_ifne(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0x9a);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_ifnonnull(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0xc7);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * @param label
     */
    public void c_ifnull(String label) {
        dec();
        int offset = addGoto(label);
        bos.write(0xc6);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * increment int variable with value
     * 
     * @param i
     * @param val
     */
    public void c_iinc(int i, int val) {
        if (i > 255 || val < -128 || val > 127) {
            bos.write(0xc4); // wide
            bos.write(0x84);
            bos.write(i >> 8);
            bos.write(i);
            bos.write(val >> 8);
            bos.write(val);
            return;
        }
        bos.write(0x84);
        bos.write(i);
        bos.write(val);
    }

    /**
     * Method c_iload.
     * 
     * @param i
     */
    public void c_iload(int i) {
        inc();
        if (i <= 3)
            bos.write(0x1a + i);
        else {
            if (i > 255)
                bos.write(0xc4); // wide
            bos.write(0x15);
            if (i > 255)
                bos.write(i >>> 8);
            bos.write(i);
        }
    }

    /**
     * int mul.
     */
    public void c_imul() {
        dec();
        bos.write(0x68);
    }

    /**
     * int negate.
     */
    public void c_ineg() {
        bos.write(0x74);
    }

    /**
     * Method c_instanceof.
     * 
     * @param cName
     */
    public void c_instanceof(String cName) {
        int n = cp.addClass(cName);
        bos.write(0xc1);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * Method c_invokeinterface.
     * 
     * @param cName
     *            class name
     * @param mName
     *            method name
     * @param signature
     *            method signature
     */
    public void c_invokeinterface(String cName, String mName, String signature) {
        int count = Util.countStackParams(signature) + 1;
        dec();
        curStack -= count - 1;
        if (!signature.endsWith("V"))
            inc();
        if (signature.endsWith("J"))
            inc();
        else if (signature.endsWith("D"))
            inc();

        int n = cp.addIfaceMethod(cName, mName, signature);
        bos.write(0xb9);
        bos.write(n >>> 8); // high byte
        bos.write(n);
        bos.write(count);
        bos.write(0);
    }

    /**
     * Method c_invokespecial
     * 
     * @param cName
     *            class name
     * @param mName
     *            method name
     * @param signature
     *            method signature
     */
    public void c_invokespecial(String cName, String mName, String signature) {
        dec();
        curStack -= Util.countStackParams(signature);
        if (!signature.endsWith("V"))
            inc();
        if (signature.endsWith("J"))
            inc();
        else if (signature.endsWith("D"))
            inc();

        int n = cp.addMethod(cName, mName, signature);
        bos.write(0xb7);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * Method c_invokestatic
     * 
     * @param cName
     *            class name
     * @param mName
     *            method name
     * @param signature
     *            method signature
     */
    public void c_invokestatic(String cName, String mName, String signature) {
        curStack -= Util.countStackParams(signature);
        if (!signature.endsWith("V"))
            inc();
        if (signature.endsWith("J"))
            inc();
        else if (signature.endsWith("D"))
            inc();

        int n = cp.addMethod(cName, mName, signature);
        bos.write(0xb8);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * Method c_invokevirtual.
     * 
     * @param cName
     *            class name
     * @param mName
     *            method name
     * @param signature
     *            method signature
     */
    public void c_invokevirtual(String cName, String mName, String signature) {
        dec();
        curStack -= Util.countStackParams(signature);
        if (!signature.endsWith("V"))
            inc();
        if (signature.endsWith("J"))
            inc();
        else if (signature.endsWith("D"))
            inc();

        int n = cp.addMethod(cName, mName, signature);
        bos.write(0xb6);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * int | int.
     */
    public void c_ior() {
        dec();
        bos.write(0x80);
    }

    /**
     * int division remainder.
     */
    public void c_irem() {
        dec();
        bos.write(0x70);
    }

    /**
     * Method c_ireturn.
     */
    public void c_ireturn() {
        dec();
        bos.write(0xac);
    }

    /**
     * shift left of int.
     */
    public void c_ishl() {
        dec();
        bos.write(0x78);
    }

    /**
     * signed shift right of int.
     */
    public void c_ishr() {
        dec();
        bos.write(0x7a);
    }

    /**
     * Method c_istore.
     * 
     * @param i
     */
    public void c_istore(int i) {
        if (i > maxLocal)
            maxLocal = i;
        dec();
        if (i >= 0 && i <= 3) {
            bos.write(i + 0x3b);
        } else if (i < 256) {
            bos.write(0x36);
            bos.write(i);
        } else {
            bos.write(0xc4); // wide
            bos.write(0x36);
            bos.write(i >>> 8);
            bos.write(i);
        }
    }

    /**
     * sub int - int.
     */
    public void c_isub() {
        dec();
        bos.write(0x64);
    }

    /**
     * unsigned shift right of int.
     */
    public void c_iushr() {
        dec();
        bos.write(0x7c);
    }

    /**
     * int ^ int.
     */
    public void c_ixor() {
        dec();
        bos.write(0x82);
    }

    /**
     * push return address on stack and jmp to label
     * 
     * @param label
     */
    public void c_jsr(String label) {
        inc();
        int offset = addGoto(label);
        bos.write(0xa8);
        bos.write(offset >> 8);
        bos.write(offset);
    }

    /**
     * convert long to double.
     */
    public void c_l2d() {
        bos.write(0x8a);
    }

    /**
     * convert long to float.
     */
    public void c_l2f() {
        dec();
        bos.write(0x89);
    }

    /**
     * convert long to int.
     */
    public void c_l2i() {
        dec();
        bos.write(0x88);
    }

    /**
     * add long + long.
     */
    public void c_ladd() {
        dec2();
        bos.write(0x61);
    }

    /**
     * load long from array.
     */
    public void c_laload() {
        bos.write(0x2f);
    }

    /**
     * long & long.
     */
    public void c_land() {
        dec2();
        bos.write(0x7f);
    }

    /**
     * store long into array.
     */
    public void c_lastore() {
        dec4();
        bos.write(0x50);
    }

    /**
     * compare long with long.
     */
    public void c_lcmp() {
        dec3();
        bos.write(0x94);
    }

    /**
     * push a long constant.
     * 
     * @param l
     */
    public void c_lconst(long l) {
        inc2();
        if (l == 0L) {
            bos.write(0x09);
            return;
        }
        if (l == 1L) {
            bos.write(0x0a);
            return;
        }

        int index = cp.addLong(l);
        bos.write(0x14);
        bos.write(index >> 8);
        bos.write(index);
    }

    /**
     * Method load a String constant.
     * 
     * @param name
     */
    public void c_ldc(String name) {
        inc();
        int n = cp.addString(name);
        if (n < 256) {
            bos.write(0x12);
            bos.write(n);
            return;
        }
        bos.write(0x13);
        bos.write(n >> 8);
        bos.write(n);
    }

    /**
     * long disivion.
     */
    public void c_ldiv() {
        dec2();
        bos.write(0x6d);
    }

    /**
     * load along variable.
     * 
     * @param vno
     */
    public void c_lload(int vno) {
        if (vno + 1 > maxLocal)
            maxLocal = vno + 1;

        inc2();
        if (vno <= 3)
            bos.write(0x1e + vno);
        else {
            if (vno > 255)
                bos.write(0xc4); // wide
            bos.write(0x16);
            if (vno > 255)
                bos.write(vno >>> 8);
            bos.write(vno);
        }
    }

    /**
     * long mul.
     */
    public void c_lmul() {
        dec2();
        bos.write(0x69);
    }

    /**
     * long negate.
     */
    public void c_lneg() {
        bos.write(0x75);
    }

    /**
     * long | long.
     */
    public void c_lor() {
        dec2();
        bos.write(0x81);
    }

    /**
     * long disivion remainder.
     */
    public void c_lrem() {
        dec2();
        bos.write(0x71);
    }

    /**
     * return a long.
     */
    public void c_lreturn() {
        dec2();
        bos.write(0xad);
    }

    /**
     * shift left of long.
     */
    public void c_lshl() {
        dec();
        bos.write(0x79);
    }

    /**
     * signed shift right of long.
     */
    public void c_lshr() {
        dec();
        bos.write(0x7b);
    }

    /**
     * Method c_lstore - store a long into var and var+1.
     * 
     * @param i
     */
    public void c_lstore(int i) {
        if (i + 1 > maxLocal)
            maxLocal = i + 1;
        dec2();
        if (i >= 0 && i <= 3) {
            bos.write(i + 0x3f);
        } else if (i < 256) {
            bos.write(0x37);
            bos.write(i);
        } else {
            bos.write(0xc4); // wide
            bos.write(0x37);
            bos.write(i >>> 8);
            bos.write(i);
        }
    }

    /**
     * sub long - long.
     */
    public void c_lsub() {
        dec2();
        bos.write(0x65);
    }

    /**
     * unsigned shift right of long.
     */
    public void c_lushr() {
        dec();
        bos.write(0x7d);
    }

    /**
     * long ^ long.
     */
    public void c_lxor() {
        dec2();
        bos.write(0x83);
    }

    /**
   *
   */
    public void c_monitorenter() {
        bos.write(0xc2);
    }

    /**
   */
    public void c_monitorexit() {
        bos.write(0xc3);
    }

    /**
     * Method c_new object of spcified class.
     * 
     * @param cName
     */
    public void c_new(String cName) {
        inc();
        int n = cp.addClass(cName);
        bos.write(0xbb);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * nop.
     */
    public void c_nop() {
        bos.write(0);
    }

    /**
     * Method c_pop.
     */
    public void c_pop() {
        dec();
        bos.write(0x57);
    }

    /**
     * Method c_pop2.
     */
    public void c_pop2() {
        dec2();
        bos.write(0x58);
    }

    /**
     * Method c_putfield.
     * 
     * @param cName
     *            class Name
     * @param fName
     *            field Name
     * @param signature
     *            signature
     */
    public void c_putfield(String cName, String fName, String signature) {
        dec2();

        int l = signature.indexOf('L');
        if (l >= 0) {
            int k = signature.indexOf(';');
            cp.addClass(signature.substring(l + 1, k));
        }

        int n = cp.addField(cName, fName, signature);
        bos.write(0xb5);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * Method c_getstatic.
     * 
     * @param cName
     *            class Name
     * @param fName
     *            field Name
     * @param signature
     *            signature
     */
    public void c_putstatic(String cName, String fName, String signature) {
        dec();

        int l = signature.indexOf('L');
        if (l >= 0) {
            int k = signature.indexOf(';');
            cp.addClass(signature.substring(l + 1, k));
        }

        int n = cp.addField(cName, fName, signature);
        bos.write(0xb3);
        bos.write(n >>> 8); // high byte
        bos.write(n);
    }

    /**
     * @param index
     */
    public void c_ret(int index) {
        dec();
        if (index < 256) {
            bos.write(0xa9);
            bos.write(index);
            return;
        }
        bos.write(0xc4);
        bos.write(0xa9);
        bos.write(index >> 8);
        bos.write(index);
    }

    /**
     * return.
     */
    public void c_return() {
        bos.write(0xb1);
    }

    /**
     * load short from array.
     */
    public void c_saload() {
        dec();
        bos.write(0x35);
    }

    /**
     * store short into array.
     */
    public void c_sastore() {
        dec3();
        bos.write(0x56);
    }

    /**
     * Method c_swap.
     */
    public void c_swap() {
        bos.write(0x5f);
    }

    /**
     * Method c_tableswitch.
     * 
     * @param n
     * @param cases
     * @param defCode
     * @throws IOException
     */
    public void c_tableswitch(int n, ArrayList<Code> cases, Code defCode) throws IOException {
        dec();
        bos.write(0xaa);

        // pad bytes
        int pad = 4 - (bos.size() & 3) & 3;
        for (int i = 0; i < pad; ++i)
            bos.write(0);

        // where first tableswitch starts
        int start = pad + 12 + 4 * cases.size() + 1;

        // size of code
        int sz = start;
        for (Iterator<Code> e = cases.iterator(); e.hasNext();) {
            Code c = e.next();
            sz += c.bos.size();
        }

        bos.write(sz >>> 24);
        bos.write(sz >>> 16);
        bos.write(sz >>> 8);
        bos.write(sz);

        bos.write(n >>> 24);
        bos.write(n >>> 16);
        bos.write(n >>> 8);
        bos.write(n);

        n += cases.size() - 1;

        bos.write(n >>> 24);
        bos.write(n >>> 16);
        bos.write(n >>> 8);
        bos.write(n);

        // write offsets
        for (Iterator<Code> e = cases.iterator(); e.hasNext();) {
            Code c = e.next();

            bos.write(start >>> 24);
            bos.write(start >>> 16);
            bos.write(start >>> 8);
            bos.write(start);

            start += c.bos.size();
        }
        // write code
        for (Iterator<Code> e = cases.iterator(); e.hasNext();) {
            Code c = e.next();
            bos.write(c.bos.toByteArray());
            if (c.maxStack + curStack > maxStack)
                maxStack = c.maxStack + curStack;
        }

        if (defCode != null) {
            bos.write(defCode.bos.toByteArray());
        }
    }

    void dec() {
        --curStack;
        if (curStack < 0)
            curStack = 0;
    }

    void dec2() {
        curStack -= 2;
    }

    void dec3() {
        curStack -= 3;
    }

    void dec4() {
        curStack -= 4;
    }

    /**
     * @param i
     */
    public void emit(int i) {
        bos.write(i);
    }

    /**
     * @param g
     * @return
     * @throws IOException
     */
    private boolean fixStack(Goto g) throws IOException {
        // unconditional gotos -> force stack at other label
        Goto lg = labels.get(g.label);
        if (lg == null) {
            throw new IOException("unresolved Label: " + g.label);
        }
        if (lg.stack == g.stack)
            return false;

        int fix = lg.stack - g.stack;
        boolean hit = false;
        for (Iterator<Goto> i = gotos.iterator(); i.hasNext();) {
            Goto fg = i.next();
            if (fg.touched)
                continue;

            if (!hit) {
                if (fg != lg)
                    continue;
                hit = true;
            }

            fg.stack -= fix;
            fg.touched = true;
            if (fg.isGoto && fg.isUnconditional)
                break;
        }
        return hit;
    }

    private int getOffset(String label) {
        Goto g = labels.get(label);
        if (g == null)
            return -1;
        return g.offset - bos.size();
    }

    void inc() {
        ++curStack;
        if (curStack > maxStack)
            maxStack = curStack;
    }

    void inc2() {
        curStack += 2;
        if (curStack > maxStack)
            maxStack = curStack;
    }

    /**
     * Method setParams.
     * 
     * @param signature
     * @param b
     */
    void setParams(String signature, String thisType) {
        maxLocal += Util.countStackParams(signature);

        String type = Util.signature2Type(signature);
        int bra = type.indexOf('(');
        if (bra >= 0) {
            int ce = type.indexOf(')');
            int n = 0;
            if (thisType != null) {
                localsMap.put(n++, Pair.makePair("this", thisType));
            }
            for (StringTokenizer st = new StringTokenizer(type.substring(bra + 1, ce), ", "); st.hasMoreElements();) {
                String t = st.nextToken();
                localsMap.put(n, Pair.makePair("p" + n, t));
                if (t.equals("long") || t.equals("double")) {
                    ++n;
                }
                ++n;
            }
        }
    }

    public String toString() {
        // return DisAsm.disassemble(bos.toByteArray(), cp);
        StringBuffer sb = new StringBuffer();

        // add local variables
        int n = 0;
        for (Pair<String, String> p : localsMap.values()) {
            if (p.getFirst().equals("this"))
                continue;
            if (localCount + n++ < localsMap.size())
                continue;
            sb.append("\t").append(p.getSecond()).append(" ").append(p.getFirst()).append(";\r\n");
        }
        if (sb.length() > 0)
            sb.append("\r\n");

        if (dc != null) {
            sb.append(dc.toString());
        }
        // else
        {
            for (Iterator<Instruction> i = instructions.iterator(); i.hasNext();) {
                Instruction ins = i.next();
                sb.append(ins.toString()).append("\r\n");
            }
        }

        return sb.toString();
    }

    /**
     * Method writeTo.
     * 
     * @param dos
     * @throws IOException
     */
    void writeCode(DataOutputStream dos) throws IOException {
        Goto last = new Goto(false, false, "", bos.size(), curStack);
        gotos.add(last);

        byte code[] = bos.toByteArray();

        // fix stack markers
        // HashSet limiter = new HashSet();
        for (Iterator<Goto> i = gotos.iterator(); i.hasNext();) {
            Goto g = i.next();
            if (!g.isGoto)
                continue;

            if (fixStack(g)) {
                /*
                 * if (limiter.contains(g.label)) break; limiter.add(g.label);
                 */
                i = gotos.iterator();
            }
        }

        if (last.stack != 0)
            System.out.println("method stack not 0");

        // resolve gotos
        for (Iterator<Goto> i = gotos.iterator(); i.hasNext();) {
            Goto g = i.next();
            if (!g.isGoto)
                continue;

            Goto lg = labels.get(g.label);
            if (lg == null) {
                throw new IOException("unresolved Label: " + g.label);
            }
            if (lg.stack != g.stack) {
                System.out.println("stack mismatch goto Label: " + g.label + ": " + g.stack + "!=" + lg.stack);
            }

            int offset = g.offset;
            int diff = lg.offset - offset++;
            code[offset++] = (byte) (diff >> 8);
            code[offset] = (byte) diff;
        }

        dos.writeShort(cp.addUTF8("Code"));
        dos.writeInt(8 + code.length + 2 + 8 * exceptions.size() + 2);
        dos.writeShort(maxStack);
        dos.writeShort(maxLocal);
        dos.writeInt(code.length);
        dos.write(code);
        dos.writeShort(exceptions.size());
        for (Ex exc : exceptions) {
            exc.writeTo(dos);
        }
        dos.writeShort(0); // no attributes yet

        bos.reset();
        bos.write(code);
    }

    /**
     * @param dos
     * @throws IOException
     */
    void writeStackMap(DataOutputStream dos) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream sm = new DataOutputStream(b);
        sm.writeShort(1); // 1 entry
        sm.writeShort(0); // from line 0
        sm.writeShort(maxLocal); // max count of local vars
        for (int i = 0; i < maxLocal; ++i)
            sm.write(1); // fake int entries
        sm.writeShort(0); // dunno
        sm.flush();

        dos.writeShort(cp.addUTF8("StackMap"));
        dos.writeInt(b.size());
        dos.write(b.toByteArray());

    }

    /**
     * assign a line number table.
     * 
     * @param lineNumberTable
     */
    void setLineNumberTable(int[][] lineNumberTable) {
        this.lineNumberTable = lineNumberTable;
    }

    void setLocalVariableTable(int[][] lvt) {
        for (int i = 0; i < lvt.length; ++i) {
            int[] lv = lvt[i];
            localsMap.put(lv[4], Pair.makePair(cp.getConstant(lv[2]), Util.signature2Type(cp.getConstant(lv[3]))));
        }
    }

    public ArrayList<String> getLocals() {
        ArrayList<String> r = new ArrayList<String>();
        for (Pair<String, String> p : localsMap.values()) {
            if (p.getFirst().equals("this"))
                continue;
            r.add(p.getFirst());
        }
        return r;
    }

    void setClassDefinition(ClassDefinition classDefinition) {
        cd = classDefinition;
    }

    /**
     * Convert the instructions into Java like instructions.
     */
    public void decompile() {
        dc = new DC(cp, localsMap, cd.getClassName());
        dc.decompile(instructions);
    }

    void downgradeLambda() {
        final ArrayList<Instruction> patched = new ArrayList<Instruction>();
        for (final Instruction ins : instructions) {
            if (ins.opcode == 0xba) {
                final Entry constant1 = cp.getEntry(((CallInstruction) ins).getIndex());
                final String ctLambdaName = "ct$lambda$" + constant1.iVal1;
                final Entry constant2 = cp.getEntry(constant1.iVal2);
                final String signature = cp.getConstant(constant2.iVal2);

                // define the ct#lambda helper function if not present
                if (cd.getMethod(ctLambdaName, signature) == null) {
                    cd.defineCtLambdaMethod(ctLambdaName, signature);
                }

                int index = cp.addMethod(cd.getClassName(), ctLambdaName, signature);

                // invoke_static
                patched.add(new ConstantInstruction(0xB8, index, cp));
                continue;
            }
            patched.add(ins);
        }
        instructions = patched;
    }

    /**
     * Add the stack types to each instruction.
     */
    public void buildStack() {
        // start with empty stack
        final ArrayList<String> stack = new ArrayList<String>();
        for (final Instruction ins : instructions) {
            ins.setStack((ArrayList<String>) stack.clone());
            
            // pop stack
            int count = ins.getStackUseCount();
            if (count == 1) {
                final String n = ins.getName();
                if (n != null && n.indexOf("store") >= 0) {
                    final int space = n.lastIndexOf(' ');
                    final Integer index = Integer.parseInt(n.substring(space + 1));
                    if (!localsMap.containsKey(index)) {
                        localsMap.put(index, Pair.makePair("v" + index, stack.get(stack.size() - 1)));
                    }
                }
            }
            while (count > 0) {
                --count;
                if (stack.isEmpty())
                    throw new RuntimeException("emtpy stack");
                stack.remove(stack.size() - 1);
            }
            
            final String push = ins.getStackPush(this);
            if (push != null) {
                if ("dup".equals(push)) {
                    stack.add(stack.get(stack.size() - 1));
                } else {
                    stack.add(push);
                }
            }
        }
    }
}