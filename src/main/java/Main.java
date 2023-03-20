import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static BufferedReader br;

    public static void main(String[] args) throws IOException {
        br = new BufferedReader(new InputStreamReader(System.in));
        System.setErr(System.out);
        if (args.length < 1) {
            try {
                throw new IllegalArgumentException("错误: 未检测到输入文件");
            } catch (Exception e) {
                System.err.println(e.getMessage());
                
            }
            Pause();
            return;
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            try {
                throw new IllegalArgumentException("错误: 输入文件不存在");
            } catch (Exception e) {
                System.err.println(e.getMessage());
                
            }
            Pause();
            return;
        }
        String line = "";
        int lineNum = 0;
        List<Integer> ids = new LinkedList<>();
        List<SimpleNote> parents = new LinkedList<>();
        int scNum = 0, sciNum = 0, bpmNum = 0, bpmsNum = 0;
        int scNumFile = -1, bpmNumFile = -1;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            while ((line = bufferedReader.readLine()) != null) {
                lineNum++;
                if (line.startsWith(new String(BOM, StandardCharsets.UTF_8)))
                    throw new IllegalArgumentException("错误: 文件有BOM头");
                if (line.startsWith("#")) {
                    if (!line.endsWith(";")) throw new CommandFormatException(lineNum, "#" + line, "指令不以分号结尾");
                    line = line.substring(1);
                    if (line.contains("=")) {
                        int i = line.indexOf("=");
                        if (line.lastIndexOf("=") != i)
                            throw new CommandFormatException(lineNum, "#" + line, "指令中含有多个等号");
                        String command = line.substring(0, i);
                        String value = line.substring(i + 1);
                        value = value.substring(0, value.length() - 1);
                        if (command.equals("SC [" + scNum + "]")) {
                            try {
                                Float.parseFloat(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "SC值不合法");
                            }
                            scNum++;
                        } else if (command.equals("SCI[" + sciNum + "]")) {
                            try {
                                Float.parseFloat(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "SC变化位置值不合法");
                            }
                            sciNum++;
                        } else if (command.equals("BPM [" + bpmNum + "]")) {
                            try {
                                Float.parseFloat(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "BPM值不合法");
                            }
                            bpmNum++;
                        } else if (command.equals("BPMS[" + bpmsNum + "]")) {
                            try {
                                Float.parseFloat(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "BPM变化位置值不合法");
                            }
                            bpmsNum++;
                        } else if (command.equals("BPM_NUMBER")) {
                            if (bpmNumFile > 0) throw new CommandFormatException(lineNum, "#" + line, "多个BPM总数存在");
                            try {
                                bpmNumFile = Integer.parseInt(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "BPM总数不合法");
                            }
                        } else if (command.equals("SCN")) {
                            if (scNumFile > 0) throw new CommandFormatException(lineNum, "#" + line, "多个SC总数存在");
                            try {
                                scNumFile = Integer.parseInt(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "SC总数不合法");
                            }
                        } else if (command.equals("OFFSET")) {
                            try {
                                Float.parseFloat(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "OFFSET不合法");
                            }
                        } else if (command.equals("BEAT")) {
                            try {
                                Float.parseFloat(value);
                            } catch (NumberFormatException ignored) {
                                throw new CommandFormatException(lineNum, "#" + line, "BEAT不合法");
                            }
                        } else if (command.equals("NDNAME")) {
                            // ignore
                        } else throw new CommandFormatException(lineNum, "#" + line, "未知指令");
                    } else {
                        line = line.substring(0, line.length() - 1);
                        switch (line) {
                            case "NOPOS":
                                break;
                            default:
                                throw new CommandFormatException(lineNum, "#" + line + ";", "未知指令");
                        }
                    }
                } else {
                    if (!line.startsWith("<") || !line.endsWith(">"))
                        throw new NoteFormatException(lineNum, line, "Note格式不正确");
                    line = line.substring(1, line.length() - 1);
                    String[] items = line.split("><");
                    for (String item : items) {
                        if (item.contains(">") || item.contains("<"))
                            throw new NoteFormatException(lineNum, line, "Note格式不正确");
                    }
                    // 检查id
                    try {
                        int id = Integer.parseInt(items[0]);
                        if (id < 0) throw new NoteFormatException(lineNum, line, "Note id小于0");
                        ids.add(id);
                    } catch (NumberFormatException e) {
                        throw new NoteFormatException(lineNum, line, "Note id不是数字");
                    }
                    // 检查parent
                    try {
                        int id = Integer.parseInt(items[6]);
                        if (id < 0) throw new NoteFormatException(lineNum, line, "Note parent小于0");
                        parents.add(new SimpleNote(lineNum, line, id));
                    } catch (NumberFormatException e) {
                        throw new NoteFormatException(lineNum, line, "Note parent不是数字");
                    }
                }
            }
            if (scNum < sciNum) throw new IllegalArgumentException("错误: SC语句少于SCI语句");
            if (scNum > sciNum) throw new IllegalArgumentException("错误: SC语句多于SCI语句");
            if (bpmNum < bpmsNum) throw new IllegalArgumentException("错误: BPM语句少于BPMS语句");
            if (bpmNum > bpmsNum) throw new IllegalArgumentException("错误: BPM语句多于BPMS语句");
            if (scNum < 1) throw new IllegalArgumentException("错误: 不存在任意SC语句");
            if (bpmNum < 1) throw new IllegalArgumentException("错误: 不存在任意BPM语句");
            if (scNumFile < 0) throw new IllegalArgumentException("错误: 文件中不存在SC总数");
            if (bpmNumFile < 0) throw new IllegalArgumentException("错误: 文件中不存在BPM总数");
            if (scNum < scNumFile) throw new IllegalArgumentException("错误: SC语句少于总数");
            if (scNum > scNumFile) throw new IllegalArgumentException("错误: SC语句多于总数");
            if (bpmNum < bpmNumFile) throw new IllegalArgumentException("错误: BPM语句少于总数");
            if (bpmNum > bpmNumFile) throw new IllegalArgumentException("错误: BPM语句多于总数");
            for (SimpleNote i :
                    parents) {
                if (!ids.contains(i.parent)) {
                    System.err.println("警告：编号为" + i + "的Note不存在，但是有Note的parent指向该编号\n行数: " + i.line + ", 内容: " + i.content);
                }
            }
            System.out.println("没发现错误");
            
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        Pause();
    }

    private static void Pause() throws IOException {
        System.out.println();
        System.out.println("按回车退出...");
        br.readLine();
    }
}

class CommandFormatException extends RuntimeException {
    public CommandFormatException(int lineNum, String line, String message) {
        super("错误行: " + lineNum + ", 内容: " + line + (message.isEmpty() ? "" : "\n") + message);
    }
}

class NoteFormatException extends RuntimeException {
    public NoteFormatException(int lineNum, String line, String message) {
        super("错误行: " + lineNum + ", 内容: " + line + "\n" + message);
    }
}

class SimpleNote {
    public int line;
    public String content;
    public int parent;

    public SimpleNote(int line, String content, int parent) {
        this.line = line;
        this.content = content;
        this.parent = parent;
    }
}