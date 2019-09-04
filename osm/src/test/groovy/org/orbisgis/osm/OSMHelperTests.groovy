package org.orbisgis.osm


import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.orbisgis.datamanager.JdbcDataSource
import org.orbisgis.datamanager.h2gis.H2GIS
import org.orbisgis.processmanagerapi.IProcess
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.junit.jupiter.api.Assertions.*

class OSMHelperTests {

    private static final Logger logger = LoggerFactory.getLogger(OSMHelperTests.class)

    @Test
    void extractTest() {
        def extract = OSMHelper.Loader.extract()
        assertTrue extract.execute(overpassQuery: "(node(48.780889172043,-3.0626213550568,48.783356423929,-3.0579113960266); " +
                "way(48.780889172043,-3.0626213550568,48.783356423929,-3.0579113960266); " +
                "relation(48.780889172043,-3.0626213550568,48.783356423929,-3.0579113960266); );\n" +
                "out;\n" +
                ">;");
        assertTrue new File(extract.results.outputFilePath).exists()
        assertTrue new File(extract.results.outputFilePath).length() > 0
    }

    @Test
    void extractTestWrongQuery() {
        def extract = OSMHelper.Loader.extract()
        assertFalse extract.execute(overpassQuery: "building =yes");
    }

    @Test
    void loadTest() {
        def h2GIS = H2GIS.open('./target/osmhelper')
        def load = OSMHelper.Loader.load()
        def prefix = "OSM_FILE"
        assertTrue load.execute(datasource : h2GIS, osmTablesPrefix : prefix, osmFilePath : new File(this.class.getResource("osmFileForTest.osm").toURI()).getAbsolutePath())

        //Count nodes
        h2GIS.eachRow "SELECT count(*) as nb FROM OSMHELPER.PUBLIC.${prefix}_NODE",{ row ->
            assertEquals 1176, row.nb }

        //Count ways
        h2GIS.eachRow "SELECT count(*) as nb FROM OSMHELPER.PUBLIC.${prefix}_WAY",{ row ->
            assertEquals 171, row.nb }

        //Count relations
        h2GIS.eachRow "SELECT count(*) as nb FROM OSMHELPER.PUBLIC.${prefix}_RELATION",{ row ->
            assertEquals 0, row.nb }

        //Check specific tags
        h2GIS.eachRow "SELECT count(*) as nb FROM OSMHELPER.PUBLIC.${prefix}_WAY_TAG WHERE ID_WAY=305633653",{ row ->
            assertEquals 2, row.nb }
    }

    @Test
    void loadTransformPolygonsTest() {
        def h2GIS = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        def load = OSMHelper.Loader.load()
        def prefix = "OSM_FILE"
        assertTrue load.execute(datasource : h2GIS, osmTablesPrefix : prefix,
                osmFilePath : new File(this.class.getResource("osmFileForTest.osm").toURI()).getAbsolutePath())
        def transform = OSMHelper.Transform.toPolygons()
        transform.execute( datasource:h2GIS, osmTablesPrefix:prefix, epsgCode :2154)
        assertEquals 126, h2GIS.getTable(transform.results.outputTableName).rowCount
    }

    @Test
    void loadTransformPolygonsFilteredTest() {
        def h2GIS = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        def load = OSMHelper.Loader.load()
        def prefix = "OSM_FILE"
        assertTrue load.execute(datasource : h2GIS, osmTablesPrefix : prefix,
                osmFilePath : new File(this.class.getResource("osmFileForTest.osm").toURI()).getAbsolutePath())

        def transform = OSMHelper.Transform.toPolygons()
        transform.execute( datasource:h2GIS, osmTablesPrefix:prefix, epsgCode :2154, tagKeys:["building"])
        assertEquals 125, h2GIS.getTable(transform.results.outputTableName).rowCount
    }

    @Test
    void loadTransformLinesTest() {
        def h2GIS = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        def load = OSMHelper.Loader.load()
        def prefix = "OSM_FILE"
        assertTrue load.execute(datasource : h2GIS, osmTablesPrefix : prefix,
                osmFilePath : new File(this.class.getResource("osmFileForTest.osm").toURI()).getAbsolutePath())
        def transform = OSMHelper.Transform.toLines()
        transform.execute( datasource:h2GIS, osmTablesPrefix:prefix, epsgCode :2154)
        h2GIS.save(transform.results.outputTableName, "/tmp/osm.shp")

        assertEquals 167, h2GIS.getTable(transform.results.outputTableName).rowCount
    }

    @Test
    void loadTransformPointsFilteredTest() {
        def h2GIS = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        def load = OSMHelper.Loader.load()
        def prefix = "OSM_FILE"
        assertTrue load.execute(datasource : h2GIS, osmTablesPrefix : prefix,
                osmFilePath : new File(this.class.getResource("osmFileForTest.osm").toURI()).getAbsolutePath())

        def transform = OSMHelper.Transform.toPoints()
        transform.execute( datasource:h2GIS, osmTablesPrefix:prefix, epsgCode :2154, tagKeys:["place"])
        assertEquals 3, h2GIS.getTable(transform.results.outputTableName).rowCount
    }


    @Test
    void extractLandCoverTest() {
        JdbcDataSource dataSource = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        assertNotNull(dataSource)
        IProcess process = OSMHelper.OSMTemplate.LANDCOVER()
        process.execute(filterArea: new Envelope(-3.1153294444084167 , -3.112971782684326, 48.73787306084071 , 48.73910952775334),datasource:dataSource)
        assertNotNull(process.results.datasource.getTable(process.results.outputPolygonsTableName))
        assertEquals 3, dataSource.getTable(process.results.outputPolygonsTableName).rowCount
    }

    @Test
    void extractBuildingTest() {
        JdbcDataSource dataSource = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        assertNotNull(dataSource)
        IProcess process = OSMHelper.OSMTemplate.BUILDING()
        process.execute(filterArea: new Envelope(26.87346652150154 , 26.874645352363586, -31.899314836854924 , -31.898518974220607), datasource: dataSource)
        assertNotNull(process.results.datasource.getTable(process.results.outputPolygonsTableName))
        assertEquals 1, dataSource.getTable(process.results.outputPolygonsTableName).rowCount
    }

    @Test
    void extractWaterTest() {
        JdbcDataSource dataSource = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        assertNotNull(dataSource)
        IProcess process = OSMHelper.OSMTemplate.WATER()
        process.execute(filterArea: new Envelope(-3.143248558044433 , -3.124387264251709, 48.25956997946164 , 48.269554636080265), datasource: dataSource)
        assertNotNull(dataSource.getTable(process.results.outputPolygonsTableName))
        assertEquals 1, dataSource.getTable(process.results.outputPolygonsTableName).rowCount
    }

    @Test
    void extractPlace() {
        assertNotNull OSMHelper.Utilities.getAreaFromPlace("vannes");
        assertNotNull OSMHelper.Utilities.getAreaFromPlace("lyon");
        Geometry geom = OSMHelper.Utilities.getAreaFromPlace("Baarle-Nassau");
        assertEquals(9,geom.getNumGeometries())
        geom = OSMHelper.Utilities.getAreaFromPlace("Baerle-Duc");
        assertEquals(24,geom.getNumGeometries())
        geom = OSMHelper.Utilities.getAreaFromPlace("séné");
        assertEquals(6,geom.getNumGeometries())
    }

    @Test
    void buildOSMQuery() {
        WKTReader  wktReader = new WKTReader()
        Geometry p = wktReader.read("POLYGON ((4 2, 10 20, 30 20, 30 0, 4 2))")
        assertEquals "[bbox:0.0,4.0,20.0, 30.0];\n" +
                "(\n" +
                "\tnode[\"water\"];\n" +
                "\tnode[\"building\"];\n" +
                "\trelation[\"water\"];\n" +
                "\trelation[\"building\"];\n" +
                "\tway[\"water\"];\n" +
                "\tway[\"building\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out;" ,
                OSMHelper.Utilities.buildOSMQuery(p.getEnvelopeInternal(), ["WATER", "BUILDING"], OSMElement.NODE, OSMElement.RELATION,OSMElement.WAY)
        assertEquals "[bbox:0.0,4.0,20.0, 30.0];\n" +
                "(\n" +
                "\tnode[\"water\"](poly:\"2.0 4.0 20.0 10.0 20.0 30.0 0.0 30.0\");\n" +
                "\trelation[\"water\"](poly:\"2.0 4.0 20.0 10.0 20.0 30.0 0.0 30.0\");\n" +
                ");\n" +
                "(._;>;);\n" +
                "out;", OSMHelper.Utilities.buildOSMQuery(p,["WATER"], OSMElement.NODE, OSMElement.RELATION)
    }

    @Test
    void buildOSMQueryEmptyKeys() {
        WKTReader  wktReader = new WKTReader()
        Geometry p = wktReader.read("POLYGON ((4 2, 10 20, 30 20, 30 0, 4 2))")
        assertEquals "[bbox:0.0,4.0,20.0, 30.0];\n" +
                "(\n" +
                "\tnode;\n" +
                "\trelation;\n" +
                ");\n" +
                "(._;>;);\n" +
                "out;", OSMHelper.Utilities.buildOSMQuery(p.getEnvelopeInternal(),[], OSMElement.NODE, OSMElement.RELATION)
    }

    @Test
    void extractPlaceLoadTransformPolygonsFilteredBboxTest() {
        JdbcDataSource dataSource = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        assertNotNull(dataSource)
        Geometry geom = OSMHelper.Utilities.getAreaFromPlace("Cliscouët, vannes");
        IProcess process = OSMHelper.OSMTemplate.BUILDING()
        process.execute(filterArea: geom.getEnvelopeInternal(),datasource:dataSource)
        File output = new File("./target/osm_building_bbox_from_place.shp")
        if(output.exists()){
            output.delete()
        }
        assertTrue process.results.datasource.save(process.results.outputPolygonsTableName, './target/osm_building_bbox_from_place.shp')
    }


    //@Test doesn't work here... POST method must be used
    void extractPlaceLoadTransformPolygonsFilteredPolyTest() {
        JdbcDataSource dataSource = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        assertNotNull(dataSource)
        Geometry geom = OSMHelper.Utilities.getAreaFromPlace("Cliscouët, vannes")
        IProcess process = OSMHelper.OSMTemplate.BUILDING()
        process.execute(filterArea: geom,datasource:dataSource)
        File output = new File("./target/osm_building_poly_from_place.shp")
        if(output.exists()){
            output.delete()
        }
        assertTrue process.results.datasource.save(process.results.outputPolygonsTableName, './target/osm_building_poly_from_place.shp')
    }

    @Test
    void extractLoadTransformRelationTest() {
        def h2GIS = H2GIS.open('./target/osmhelper;AUTO_SERVER=TRUE')
        def load = OSMHelper.Loader.load()
        def prefix = "OSM_FILE"
        assertTrue load.execute(datasource : h2GIS, osmTablesPrefix : prefix,
                osmFilePath : new File(this.class.getResource("osm_one_relation.osm").toURI()).getAbsolutePath())
        def transform = OSMHelper.Transform.extractRelationsAsLines(h2GIS,prefix, 2154, "relations_tests", ["building"])

        assertEquals 1, h2GIS.getTable("relations_tests").rowCount
    }
}