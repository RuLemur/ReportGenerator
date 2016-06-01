import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sun.text.normalizer.UTF16;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Inflater;

public class Generator {
    public static void main(String[] args) {
        File settingsFile = new File(args[0]);  //input data
        String inputDataFile = args[1];
        File outData = new File(args[2]);

        List<LinkedList<String>> settings = parseSettingsXML(settingsFile);
        List<String[]> inputData = new LinkedList<>();

        StringBuilder report = new StringBuilder();
        inputData = readDataFile(inputDataFile);

        int screenSize = Integer.valueOf(settings.get(0).get(0)) * Integer.valueOf(settings.get(0).get(1));
        report.append(createHeader(settings)).append("\n");
        report.append(createLine(Integer.valueOf(settings.get(0).get(0)))).append("\n");
        for (int i = 0; i < inputData.size(); i++) {

            if (report.length() < screenSize) {
                report.append(newNote(settings, inputData.get(i)));
                report.append(createLine(Integer.valueOf(settings.get(0).get(0)))).append("\n");
            } else {
                report.append("~\n");
                report.append(createHeader(settings)).append("\n");
                report.append(createLine(Integer.valueOf(settings.get(0).get(0)))).append("\n");
                screenSize *= 2;
                i--;
            }
        }
        outToFile(report.toString(),outData);
        System.out.print(report);


    }

    private static void outToFile(String s,File file) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF16"))){
            bw.write(s);
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*rintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
        writer.println("The first line"); writer.println("The second line"); writer.close();
        Создание двоичного файла (также перезапись файла):*/
    }

    public static List<LinkedList<String>> parseSettingsXML(File settings) {
        //Map<String, String> result = new LinkedHashMap<>();
        List<LinkedList<String>> result = new LinkedList<>();
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();

            DefaultHandler defaultHandler = new DefaultHandler() {
                String block, element;
                String pwidth, pheight, ctitle, cwidth;


                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if ("page".equalsIgnoreCase(qName) || "column".equalsIgnoreCase(qName)) {
                        block = qName;
                    }
                    element = qName;

                }

                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if ("page".equalsIgnoreCase(qName)) {

                        result.add(new LinkedList<String>(Arrays.asList(pwidth, pheight)));
                        //element = "";
                    }
                    if ("column".equalsIgnoreCase(qName)) {
                        result.add(new LinkedList<String>(Arrays.asList(ctitle, cwidth)));

                    }
                    element = "";

                }

                public void characters(char ch[], int start, int length) throws SAXException {
                    String str = String.valueOf(ch, start, length);
                    if ("page".equalsIgnoreCase(block)) {
                        if ("width".equalsIgnoreCase(element)) {
                            pwidth = str;
                        }
                        if ("height".equalsIgnoreCase(element)) {
                            pheight = str;
                        }
                    }
                    if ("column".equalsIgnoreCase(block)) {
                        if ("title".equalsIgnoreCase(element)) {
                            ctitle = str;
                        }
                        if ("width".equalsIgnoreCase(element)) {
                            cwidth = str;
                        }
                    }
                }

            };

            saxParser.parse(settings, defaultHandler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String createHeader(List<LinkedList<String>> settingList) {
        StringBuilder header = new StringBuilder();
        int summcol = 1;
        for (int i = 1; i < settingList.size(); i++) {
            summcol += Integer.valueOf(settingList.get(i).get(1)) + 3;
        }

        if (Integer.valueOf(settingList.get(0).get(0)) == summcol) {
            header.append("|");
            for (int i = 1; i < settingList.size(); i++) {
                header.append(" ");
                header.append(settingList.get(i).get(0));
                header.append(addSpaces(Integer.valueOf(settingList.get(i).get(1)) - settingList.get(i).get(0).length()));
                header.append(" |");
            }
        }
        //header.append("\n");
        return header.toString();

    }

    public static List<String[]> readDataFile(String data) {
        List<String> fileLines = new LinkedList<>();
        List<String[]> outList = new LinkedList<>();
        try (Stream<String> stream = Files.lines(Paths.get(data), Charset.forName("UTF16"))) {
            fileLines = stream.collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String node : fileLines) {
            outList.add(node.split("\t"));
        }
        return outList;
    }

    public static String newNote(List<LinkedList<String>> settings, String[] data) {
        StringBuilder note = new StringBuilder();
        StringBuilder[] dataRemain = new StringBuilder[data.length];
        int maxiter = 1;
        for (int i = 0; i < data.length; i++) {
            dataRemain[i] = new StringBuilder(data[i]);
            if (dataRemain[i].length() > Integer.valueOf(settings.get(i + 1).get(1))) {
                int t = dataRemain[i].length() / Integer.valueOf(settings.get(i + 1).get(1)) + 1;
                if (t > maxiter)
                    maxiter = t;
            }
        }
        for (int k = 0; k < maxiter; k++) {
            for (int i = 0; i < data.length; i++) {
                note.append("| ");
                if (dataRemain[i].length() <= Integer.valueOf(settings.get(i + 1).get(1))) {
                    note.append(dataRemain[i]);
                    note.append(addSpaces(Integer.valueOf(settings.get(i + 1).get(1)) - dataRemain[i].length()));
                    note.append(" ");
                    dataRemain[i].delete(0, dataRemain[i].length());
                    data[i] = dataRemain[i].toString();
                } else {
                    note.append(dataRemain[i].substring(0, 7));
                    //note.append(addSpaces(Integer.valueOf(settings.get(i + 1).get(1)) - dataRemain[i].length()));
                    note.append(" ");
                    try {
                        dataRemain[i].delete(0, Integer.valueOf(settings.get(i + 1).get(1)));
                    } catch (Exception e) {
                    }
                }


            }
            note.append("|\n");

        }


        return note.toString();
    }

    public static String addSpaces(int count) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < count; i++) {
            str.append(" ");
        }

        return str.toString();
    }

    public static String createLine(int width) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < width; i++) {
            str.append("-");
        }
        //str.append("\n");
        return str.toString();
    }

}

