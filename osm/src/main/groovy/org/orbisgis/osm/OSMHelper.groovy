package org.orbisgis.osm

import org.orbisgis.processmanager.ProcessManager
import org.orbisgis.processmanagerapi.IProcessFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Main script to access to all processes used to extract, transform and save OSM data as GIS layers
 * @author Erwan Bocher CNRS LAB-STICC
 * @author Elisabeth Lesaux UBS LAB-STICC
 */
abstract class OSMHelper extends Script {

    public static IProcessFactory processFactory = ProcessManager.getProcessManager().factory("osm")

    public static Logger logger = LoggerFactory.getLogger(OSMHelper.class)

    public static  Loader = new Loader()

    public static  Transform = new Transform()

    static def uuid(){UUID.randomUUID().toString().replaceAll("-", "_")}

}