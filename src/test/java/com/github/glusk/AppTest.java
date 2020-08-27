package com.github.glusk;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

public class AppTest {
    private static XML xml;
    @BeforeClass
    public static void initResource() throws IOException {
        xml = new XMLDocument(
            Thread.currentThread()
                  .getContextClassLoader()
                  .getResource("dtecbs-l.xml")
            ).registerNs("bsi", "http://www.bsi.si");
    }
    @Test
    public void testFindRateByDateAndCurrency() {
        assertEquals(
            xml.xpath("//bsi:tecajnica[@datum=\"2018-07-16\"]/bsi:tecaj[@oznaka=\"USD\"]/text()").get(0),
            "1.1720"
        );
    }
    @Test
    public void testCountRatesByDate() {
        assertEquals(
            xml.nodes("//bsi:tecajnica[@datum=\"2018-07-16\"]/bsi:tecaj").size(),
            32
        );
    }
    @Test
    public void testCompareDates() {
        assertEquals(
            xml.nodes("//bsi:tecajnica[number(translate(@datum, '-', '')) > 20180712]").size(),
            2
        );
    }
    @Test
    public void testNestedQuery() {
        List<XML> nodes = xml.nodes("//bsi:tecajnica[number(translate(@datum, '-', '')) = 20180712]");
        for (XML node : nodes) {
            // library bug - none of these work:
            // node.xpath("bsi:tecajnica/@datum").get(0));
            // node.xpath("/bsi:tecajnica/@datum").get(0));
            // node.xpath("//bsi:tecajnica/@datum").get(0));
            // Work-around:
            XML freshNode = new XMLDocument(node.toString()).registerNs("bsi", "http://www.bsi.si");
            assertEquals(
                freshNode.xpath("/bsi:tecajnica/@datum").get(0),
                "2018-07-12"
            );
        }
    }
}
