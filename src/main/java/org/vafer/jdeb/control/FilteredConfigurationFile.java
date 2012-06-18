package org.vafer.jdeb.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

public class FilteredConfigurationFile {

    private static String openToken = "[[";
    private static String closeToken = "]]";
    private List<String> lines = new ArrayList<String>();
    private String name;

    public FilteredConfigurationFile(String name, InputStream pInputStream, VariableResolver pResolver) throws IOException, ParseException {
        this.name = name;
        parse(pInputStream, pResolver);
    }

    public static void setOpenToken( final String pToken ) {
        openToken = pToken;
    }

    public static void setCloseToken( final String pToken ) {
        closeToken = pToken;
    }

    private void parse(InputStream pInputStream, VariableResolver pResolver) throws IOException, ParseException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(pInputStream));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(Utils.replaceVariables(pResolver, line, openToken, closeToken));
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }

    public String getName() {
        return name;
    }

}
