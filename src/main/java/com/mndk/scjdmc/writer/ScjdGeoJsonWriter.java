package com.mndk.scjdmc.writer;

import com.mndk.scjdmc.column.LayerDataType;
import com.mndk.scjdmc.util.ScjdDirectoryParsedMap;
import com.mndk.scjdmc.util.file.DirectoryManager;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ScjdGeoJsonWriter {


    public static void writeAsSingleJsonFile(
            ScjdDirectoryParsedMap<SimpleFeatureCollection> featureMap, File destinationFile
    ) throws IOException {

        DirectoryManager.createParentFolders(destinationFile);
        try (SimpleFeatureJsonWriter writer = SimpleFeatureJsonWriter.newFeatureCollectionWriter(destinationFile)) {
            for(Map.Entry<LayerDataType, List<SimpleFeatureCollection>> entry : featureMap.entrySet()) {
                List<SimpleFeatureCollection> featureCollection = entry.getValue();
                if(featureCollection != null) writeCollectionList(entry.getValue(), writer);
            }
        }
    }


    public static void writeAsFolder(
            ScjdDirectoryParsedMap<SimpleFeatureCollection> featureMap, File destinationFolder
    ) throws IOException {
        for(Map.Entry<LayerDataType, List<SimpleFeatureCollection>> entry : featureMap.entrySet()) {
            String jsonFileName = entry.getKey().getLayerName() + ".json";
            List<SimpleFeatureCollection> featureCollection = entry.getValue();
            if(featureCollection == null) continue;

            File destinationFile = new File(destinationFolder, jsonFileName);
            DirectoryManager.createParentFolders(destinationFile);

            try (SimpleFeatureJsonWriter writer = SimpleFeatureJsonWriter.newFeatureCollectionWriter(destinationFile)) {
                writeCollectionList(entry.getValue(), writer);
            }
        }
    }


    public static void writeCollectionList(
            List<SimpleFeatureCollection> collections, SimpleFeatureJsonWriter writer
    ) throws IOException {
        for(SimpleFeatureCollection collection : collections) {
            if(collection == null) continue;

            SimpleFeatureIterator featureIterator = collection.features();
            while (featureIterator.hasNext()) {
                writer.write(featureIterator.next());
            }
        }

    }

}