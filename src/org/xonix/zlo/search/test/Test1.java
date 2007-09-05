package org.xonix.zlo.search.test;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.Query;

/**
 * Author: Vovan
 * Date: 06.09.2007
 * Time: 0:48:24
 */
public class Test1 {
    public static void main(String[] args) {
        QueryParser qp = new QueryParser("field1", new SimpleAnalyzer());
        try {
            Query q = qp.parse("[1/1/04 TO 5/30/05]");
            System.out.println(q.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
