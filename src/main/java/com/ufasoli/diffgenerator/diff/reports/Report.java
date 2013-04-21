package com.ufasoli.diffgenerator.diff.reports;

import difflib.DiffRow;

/**
 * Created with IntelliJ IDEA.
 * User: ufasoli
 * Date: 21/04/13
 * Time: 19:17
 * To change this template use File | Settings | File Templates.
 */
public interface Report {

    public void generateReport(String targetFolder, List<DiffRow> diffs);
    public void generateAggregatedReport(String targetFolder, List<DiffRow> diffs, String reportName);
}
