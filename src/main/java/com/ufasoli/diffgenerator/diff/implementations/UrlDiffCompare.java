package com.ufasoli.diffgenerator.diff.implementations;

import difflib.Chunk;
import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffRow;
import difflib.DiffRow.Tag;
import difflib.DiffRowGenerator;
import difflib.DiffRowGenerator.Builder;
import difflib.DiffUtils;
import difflib.Patch;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

public class UrlDiffCompare
{
    private String url1;
    private String url2;
    private String reportName;
    private String outputFolder;
    private List<String> url1Lines;
    private List<String> url2Lines;

    public UrlDiffCompare(String url1, String url2, String reportName, String outputFolder)
    {
        this.url1 = url1;
        this.url2 = url2;
        this.reportName = reportName;
        this.outputFolder = outputFolder;
    }

    public void compare() {
        this.url1Lines = urlToLines(this.url1);
        this.url2Lines = urlToLines(this.url2);

        DiffRowGenerator.Builder builder = new DiffRowGenerator.Builder();
        builder.showInlineDiffs(false);
        builder.columnWidth(120);
        DiffRowGenerator drg = builder.build();

        List rows = drg.generateDiffRows(this.url1Lines, this.url2Lines);
        try
        {
            FileWriter fWriter = new FileWriter(this.outputFolder + this.reportName + ".html");
            StringBuilder htmlHead = new StringBuilder(
                    "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body><div id='comparaison'><pre style=' white-space: pre-wrap; word-wrap: break-word;'> ");

            htmlHead.append(
                    String.format("<h3>Legend</h3><ul><li>changes : <span style='background-color: %s; border: thin solid black;'>&nbsp;</span></li><li> deleted  : <span style='background-color: %s; border: thin solid black;'>&nbsp;</span></li><li>Equal : <span style='background-color: %s; border: thin solid black;'>&nbsp;</span></li><li>inserted : <span style='background-color: %s; border: thin solid black;'>&nbsp;</span></li></ul>", new Object[] {
                            "#FF9933", "#FCD8D9", "#E0FCD0", "#0099CC" }));

            StringBuilder htmlDiffbody = new StringBuilder(
                    "<table style='border-collapse: separate; border-spacing: 2px; border-color: gray;' >");
            htmlDiffbody.append(String.format(
                    "<tr><th>Line</th><th>%s</th><th>Line</th><th>%s</th></tr>", new Object[] { this.url1, this.url2 }));
            htmlDiffbody.append("<tr>");
            int line = 1;
            int changes = 0;
            int deleted = 0;
            int inserted = 0;
            int equal = 0;

            for (DiffRow dr : rows)
            {
                htmlDiffbody.append("<tr>");
                String leftCss = "gray";
                String rightCss = "gray";

                if (dr.getTag() == DiffRow.Tag.CHANGE) {
                    leftCss = " border : 1px solid #FF6633; background-color: #FF9933";
                    rightCss = leftCss;
                    changes++;
                }
                else if (dr.getTag() == DiffRow.Tag.DELETE)
                {
                    leftCss = "border: 1px solid #9A2328; background-color: #FCD8D9";
                    rightCss = leftCss;
                    deleted++;
                } else if (dr.getTag() == DiffRow.Tag.EQUAL)
                {
                    leftCss = "border: 1px solid #1A981F; background-color: #E0FCD0";
                    rightCss = leftCss;
                    equal++;
                }
                else if (dr.getTag() == DiffRow.Tag.INSERT) {
                    leftCss = "border : 1px solid #8E8E8E; background-color: #DEDEDE";
                    rightCss = "border : 1px solid #0033CC; background-color: #CFFEF0";
                    inserted++;
                }

                htmlDiffbody
                        .append("<td style='background-color: #cccccc; font-family: monospace; font-weight: bold;'>" +
                                line + "</td>");
                htmlDiffbody
                        .append("<td style='" + leftCss +
                                " ; color: black; font-family: monospace; '>" + dr.getOldLine() +
                                "</td>");
                htmlDiffbody
                        .append("<td style='background-color: #cccccc; font-family: monospace; font-weight: bold;'>" +
                                line + "</td>");
                htmlDiffbody.append("<td style='" + rightCss +
                        "; color: black; font-family: monospace'>" + dr.getNewLine() + "</td>");

                line++;
            }

            htmlHead.append(
                    String.format("<ul><li>Inserted Lines : %s</li><li>Deleted Lines : %s</li><li>Modified Lines : %s</li><li>Non Modified lines :%s </li></ul> <hr />", new Object[] {
                            Integer.valueOf(inserted), Integer.valueOf(deleted), Integer.valueOf(changes), Integer.valueOf(equal) }));

            htmlDiffbody.append("</table></pre></div></body></html>");
            fWriter.write(htmlHead.toString());
            fWriter.write(htmlDiffbody.toString());
            fWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Chunk> getChangesFromOriginal()
            throws IOException
    {
        return getChunksByType(Delta.TYPE.CHANGE);
    }

    public List<Chunk> getInsertsFromOriginal() throws IOException {
        return getChunksByType(Delta.TYPE.INSERT);
    }

    public List<Chunk> getDeletesFromOriginal() throws IOException {
        return getChunksByType(Delta.TYPE.DELETE);
    }

    private List<Chunk> getChunksByType(Delta.TYPE type) throws IOException {
        List listOfChanges = new ArrayList();
        List deltas = getDeltas();
        for (Delta delta : deltas) {
            if (delta.getType() == type) {
                listOfChanges.add(delta.getRevised());
            }
        }
        return listOfChanges;
    }

    private List<Delta> getDeltas() throws IOException
    {
        Patch patch = DiffUtils.diff(this.url1Lines, this.url2Lines);

        return patch.getDeltas();
    }

    private List<String> urlToLines(String stringUrl)
    {
        List urlLines = new ArrayList();
        try
        {
            URL url = new URL(stringUrl);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                String inputLine;
                urlLines.add(inputLine);
            }

            in.close();

            if (urlLines.size() == 1)
            {
                FileWriter tmpFile = new FileWriter(this.outputFolder + "tmp.json");
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = (JsonNode)mapper.readValue((String)urlLines.get(0), JsonNode.class);

                ObjectWriter writer = mapper.defaultPrettyPrintingWriter();
                String formattedObject = writer.writeValueAsString(rootNode);

                tmpFile.write(formattedObject);

                tmpFile.close();
                urlLines.clear();
                urlLines.addAll(fileToLines(new File(this.outputFolder + "tmp.json")));
            }

            in.close();
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return urlLines;
    }

    private List<String> fileToLines(File file)
    {
        List lines = new ArrayList();
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null)
            {
                String line;
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

    public String getUrl1() {
        return this.url1;
    }

    public void setUrl1(String url1) {
        this.url1 = url1;
    }

    public String getUrl2() {
        return this.url2;
    }

    public void setUrl2(String url2) {
        this.url2 = url2;
    }

    public String getReportName() {
        return this.reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public List<String> getUrl1Lines() {
        return this.url1Lines;
    }

    public void setUrl1Lines(List<String> url1Lines) {
        this.url1Lines = url1Lines;
    }

    public List<String> getUrl2Lines() {
        return this.url2Lines;
    }

    public void setUrl2Lines(List<String> url2Lines) {
        this.url2Lines = url2Lines;
    }

    public String getOutputFolder() {
        return this.outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }
}