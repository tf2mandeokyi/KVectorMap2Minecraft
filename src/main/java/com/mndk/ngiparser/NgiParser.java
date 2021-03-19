package com.mndk.ngiparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.mndk.ngiparser.nda.NdaDataColumn;
import com.mndk.ngiparser.ngi.NgiHeader;
import com.mndk.ngiparser.ngi.NgiLayer;
import com.mndk.ngiparser.ngi.NgiParseException;
import com.mndk.ngiparser.ngi.NgiParseResult;
import com.mndk.ngiparser.ngi.element.NgiElement;
import com.mndk.ngiparser.ngi.element.NgiLineElement;
import com.mndk.ngiparser.ngi.element.NgiPointElement;
import com.mndk.ngiparser.ngi.element.NgiPolygonElement;
import com.mndk.ngiparser.ngi.element.NgiTextElement;

public class NgiParser {

	
	
    private BufferedReader ngiReader, ndaReader;
    private NgiParseResult result;

    
    
    /**
     * Parses ngi file, and if available, it also parses same-filename-having nda file.
     * @param ngiFilePath The path of the .ngi file
     * */
    public static NgiParseResult parse(String ngiFilePath, String encoding) throws IOException {
    	if(!FilenameUtils.getExtension(ngiFilePath).equals("ngi"))
    		throw new NgiParseException();
    	String ndaFilePath = replaceLast(ngiFilePath, ".ngi", ".nda");
    	if(!new File(ndaFilePath).exists()) ndaFilePath = null;
        return parse(ngiFilePath, ndaFilePath, encoding);
    }

    

    /**
     * Parses both ngi file and nda file, and then combines them.
     * @param ngiFilePath The path of the .ngi file
     * @param ndaFilePath The path of the .nda file
     * */
    public static NgiParseResult parse(String ngiFilePath, String ndaFilePath, String encoding) throws IOException {
    	return new NgiParser().parse(
        		new BufferedReader(new InputStreamReader(new FileInputStream(ngiFilePath), encoding)),
        		ndaFilePath == null ? null : new BufferedReader(new InputStreamReader(new FileInputStream(ndaFilePath), encoding))
        );
    }
    
    
    
    /**
     * Parses both ngi file and nda file, and then combines them.
     * @param ngiFilePath The path of the .ngi file
     * @param ndaFilePath The path of the .nda file
     * */
    public static NgiParseResult parse(InputStream ngiStream, InputStream ndaStream) throws IOException {
    	return new NgiParser().parse(
        		new BufferedReader(new InputStreamReader(ngiStream)),
        		ndaStream == null ? null : new BufferedReader(new InputStreamReader(ndaStream))
        );
    }
    
    

    private NgiParseResult parse(BufferedReader ngiReader, BufferedReader ndaReader) throws IOException {
        this.ngiReader = ngiReader;
    	this.ndaReader = ndaReader;
    	result = new NgiParseResult(new HashMap<>(), new HashMap<>());
    	
    	String line;
    	
        while(ngiReader.ready()) {
            line = ngiReader.readLine();
            if(line.equals("<LAYER_START>")) {
                readNgiLayer();
            }
        }
        
        if(this.ndaReader != null) {
        	while(ndaReader.ready()) {
                line = ndaReader.readLine();
                if(line.equals("<LAYER_START>")) {
                    readNdaLayer();
                }
            }
        }
        
        return result;
    }



	private void readNgiLayer() throws IOException {
        NgiLayer layer = new NgiLayer();
        int id = 0;
        layerLoop: while(ngiReader.ready()) {
            String line = ngiReader.readLine();
            switch(line) {
                case "$LAYER_ID":
                    id = readIntegerVariable(ngiReader);
                    break;
                case "$LAYER_NAME":
                    layer.name = readStringVariable(ngiReader);
                    break;
                case "<HEADER>":
                    layer.header = this.readNgiHeader();
                    break;
                case "<DATA>":
                	this.readNgiData(layer);
                    break;
                case "<LAYER_END>":
                    break layerLoop;
                default:
                    throw new NgiParseException();
            }
        }
        result.addLayer(id, layer);
    }
    
    

    private void readNdaLayer() throws IOException {
    	int layerId = 0;
    	
        layerLoop: while(ndaReader.ready()) {
            String line = ndaReader.readLine();
            switch(line) {
                case "$LAYER_ID":
                    layerId = readIntegerVariable(ndaReader);
                    break;
                case "$LAYER_NAME":
                	readStringVariable(ndaReader);
                    // I mean, the name should be already declared in .ngi file,
                	// so why do you need to read this again?
                    break;
                case "<HEADER>":
                    this.readNdaHeader(layerId);
                    break;
                case "<DATA>":
                	this.readNdaData(layerId);
                    break;
                case "<LAYER_END>":
                    break layerLoop;
                default:
                    throw new NgiParseException();
            }
        }
	}

	
	
	private NgiHeader readNgiHeader() throws IOException {
        NgiHeader result = new NgiHeader();
        headerLoop: while(ngiReader.ready()) {
            String line = ngiReader.readLine();
            switch(line) {
                case "$VERSION":
                    result.version = readIntegerVariable(ngiReader);
                    break;
                case "$GEOMETRIC_METADATA":
                    this.readNgiGeometricMetadata(result);
                    break;
                case "$POINT_REPRESENT":
                    result.pointAttributes = this.readNgiHeaderAttributes(new NgiPointElement.Attr());
                    break;
                case "$LINE_REPRESENT":
                    result.lineAttributes = this.readNgiHeaderAttributes(new NgiLineElement.Attr());
                    break;
                case "$REGION_REPRESENT":
                    result.polygonAttributes = this.readNgiHeaderAttributes(new NgiPolygonElement.Attr());
                    break;
                case "$TEXT_REPRESENT":
                	result.textAttributes = this.readNgiHeaderAttributes(new NgiTextElement.Attr());
                    break;
                case "<END>":
                    break headerLoop;
            }
        }
        return result;
    }
	
	

    private <T extends NgiElement.Attr> Map<Integer, T> readNgiHeaderAttributes(T newAttrInstance) throws IOException {
        Map<Integer, T> attributes = new HashMap<>();
        while(ngiReader.ready()) {
            String line = ngiReader.readLine();
            if(line.equals("$END")) break;
            
            int firstSpace = line.indexOf(' ');
            int index = Integer.parseInt(line.substring(0, firstSpace));
            String attr = line.substring(firstSpace + 1);
            
            String[] parameters = attr.substring(attr.indexOf('('), attr.lastIndexOf(')')).split(",\\s*");
            
            newAttrInstance.from(parameters);
            
            attributes.put(index, newAttrInstance);
        }
        return attributes;
    }

    
    
    private void readNgiGeometricMetadata(NgiHeader header) throws IOException {
        while(ngiReader.ready()) {
            String line = ngiReader.readLine();
            if(line.startsWith("MASK(") && line.endsWith(")")) {
                // TODO figure out how to deal with this
            	// or not, I mean why would this be necessary smh
            }
            else if(line.startsWith("DIM(") && line.endsWith(")")) {
                header.dimensions = Integer.parseInt(line.substring(4, line.length() - 1));
            }
            else if(line.startsWith("BOUND(") && line.endsWith(")")) {
                String[] args = line.substring(6, line.length() - 1).split(",");
                double[] min = new double[header.dimensions], max = new double[header.dimensions];
                for(int i=0;i<header.dimensions;i++) {
                    min[i] = Double.parseDouble(args[i]);
                    max[i] = Double.parseDouble(args[header.dimensions + i]);
                }
                header.bound = new NgiHeader.Boundary(min, max);
            }
            if(line.equals("$END")) break;
        }
    }

	
	
	private void readNdaHeader(int layerId) throws IOException {
        NdaDataColumn[] columnList = new NdaDataColumn[0];
        headerLoop: while(ndaReader.ready()) {
            String line = ndaReader.readLine();
            switch(line) {
                case "$VERSION":
                    // Skip version declaration
                    break;
                case "$ASPATIAL_FIELD_DEF":
                	columnList = this.readNdaHeaderColumns();
                    break;
                case "<END>":
                    break headerLoop;
            }
        }
        result.getLayer(layerId).header.columns = columnList;
    }

	
	
	private NdaDataColumn[] readNdaHeaderColumns() throws IOException {
        List<NdaDataColumn> result = new ArrayList<>();
        while(ndaReader.ready()) {
            String line = ndaReader.readLine();
            if(line.equals("$END")) break;
            else if(line.startsWith("ATTRIB")) {
            	String columnData = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
            	result.add(new NdaDataColumn(parseDataParametersToObject(columnData)));
            }
        }
        return result.toArray(new NdaDataColumn[0]);
    }
    
    

    private void readNgiData(NgiLayer layer) throws IOException {
    	Map<Integer, NgiElement<?>> result = new HashMap<>();
    	while(ngiReader.ready()) {
            String line = ngiReader.readLine();
            if(line.startsWith("$RECORD")) {
            	int recordIndex = Integer.parseInt(line.substring(line.indexOf(" ") + 1));
            	NgiElement<?> element = readNgiRecord(layer);
            	result.put(recordIndex, element);
            	this.result.addElement(recordIndex, element);
            }
            else if(line.equals("<END>")) break;
        }
    	layer.data = result;
	}
    
    

    private void readNdaData(int layerId) throws IOException {
    	while(ndaReader.ready()) {
            String line = ndaReader.readLine();
            if(line.startsWith("$RECORD")) {
            	int recordIndex = Integer.parseInt(line.substring(line.indexOf(" ") + 1));
            	NgiElement<?> element = result.getElement(recordIndex);
            	String nextLine = ndaReader.readLine();
            	Object[] row = parseDataParametersToObject(nextLine);
            	element.rowData = row;
            }
            else if(line.equals("<END>")) break;
        }
	}

    

	private NgiElement<?> readNgiRecord(NgiLayer layer) throws IOException {
    	
        String type = ngiReader.readLine(), line;
        int dimensions = layer.header.dimensions;
        
        int vertexCount, attributeIndex;
        
        if(type.startsWith("TEXT")) {
    		NgiTextElement textElement = new NgiTextElement(layer);
        	textElement.text = type.substring(6, type.length() - 2);
        	textElement.position = new double[dimensions];

        	String[] pointStringArr = ngiReader.readLine().split(" ");
    		for(int d = 0; d < dimensions; ++d) {
    			textElement.position[d] = Double.parseDouble(pointStringArr[d]);
        	}
            
            line = ngiReader.readLine();
            if(!line.startsWith("TEXTGATTR")) throw new NgiParseException();
            attributeIndex = Integer.parseInt(line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")));
            textElement.attribute = layer.header.textAttributes.get(attributeIndex);
        	return textElement;
        }
        
        switch(type) {
        	case "POINT":
        		NgiPointElement pointElement = new NgiPointElement(layer);
        		pointElement.position = new double[dimensions];

            	String[] pointStringArr = ngiReader.readLine().split(" ");
        		for(int d = 0; d < dimensions; ++d) {
        			pointElement.position[d] = Double.parseDouble(pointStringArr[d]);
            	}
                
                line = ngiReader.readLine();
                if(!line.startsWith("SYMBOLGATTR")) throw new NgiParseException();
                attributeIndex = Integer.parseInt(line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")));
                pointElement.attribute = layer.header.pointAttributes.get(attributeIndex);
        		
        		return pointElement;
        		
        	case "LINESTRING":
        		NgiLineElement lineElement = new NgiLineElement(layer);
                vertexCount = Integer.parseInt(ngiReader.readLine());
                lineElement.lineData = new double[vertexCount][];
                
                for(int v = 0; v < vertexCount; ++v) {
                	lineElement.lineData[v] = new double[dimensions];
                	String[] vertexStringArr = ngiReader.readLine().split(" ");
                	
                	for(int d = 0; d < dimensions; ++d) {
                		lineElement.lineData[v][d] = Double.parseDouble(vertexStringArr[d]);
                	}
                }
                
                line = ngiReader.readLine();
                if(!line.startsWith("LINEGATTR")) throw new NgiParseException();
                attributeIndex = Integer.parseInt(line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")));
                lineElement.attribute = layer.header.lineAttributes.get(attributeIndex);
                
        		return lineElement;
        		
        	case "POLYGON":
        		NgiPolygonElement polygonElement = new NgiPolygonElement(layer);
        		
        		line = ngiReader.readLine();
        		if(!line.startsWith("NUMPARTS")) throw new NgiParseException();
                int numParts = Integer.parseInt(line.substring(9));
                polygonElement.vertexData = new double[numParts][][];
                
                for(int p = 0; p < numParts; ++p) {
                	vertexCount = Integer.parseInt(ngiReader.readLine());
                	polygonElement.vertexData[p] = new double[vertexCount][];
                	
                	for(int v = 0; v < vertexCount; ++v) {
                		polygonElement.vertexData[p][v] = new double[dimensions];
                    	String[] vertexStringArr = ngiReader.readLine().split(" ");
                    	
                    	for(int d = 0; d < dimensions; ++d) {
                    		polygonElement.vertexData[p][v][d] = Double.parseDouble(vertexStringArr[d]);
                    	}
                    }
                }
                
                line = ngiReader.readLine();
                if(!line.startsWith("REGIONGATTR")) throw new NgiParseException();
                attributeIndex = Integer.parseInt(line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")));
                polygonElement.attribute = layer.header.polygonAttributes.get(attributeIndex);
                
        		return polygonElement;
        		
        	default:
        		throw new NgiParseException();
        }
        
    }

    
    
    private static int readIntegerVariable(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        int result = Integer.parseInt(line);
        line = reader.readLine();
        if(!line.equals("$END")) throw new NgiParseException();
        return result;
    }

    
    
    private static String readStringVariable(BufferedReader reader) throws IOException {
        String result = reader.readLine();
        if(!result.startsWith("\"") || !result.endsWith("\""))
            throw new NgiParseException();
        result = result.substring(1, result.length() - 1);
        String line = reader.readLine();
        if(!line.equals("$END")) throw new NgiParseException();
        return result;
    }
    
    
    
    private static Object[] parseDataParametersToObject(String parametersString) {
    	return parseDataParametersToObject(parametersString, "\\s*,\\s*");
    }
    
    
    
    private static Object[] parseDataParametersToObject(String parametersString, String splitRegex) {
    	String[] parameters = parametersString.split(splitRegex);
    	Object[] result = new Object[parameters.length];
    	for(int i = 0; i < parameters.length; ++i) {
    		String temp = parameters[i];
    		if(temp.startsWith("\"") && temp.endsWith("\"")) {
    			result[i] = temp.substring(1, temp.length() - 1);
    		}
    		else if(temp.toLowerCase().equals("false") || temp.toLowerCase().equals("true")) {
    			result[i] = Boolean.parseBoolean(temp);
    		}
    		else if(temp.matches("-?(\\d+)")) { // check integer
    			result[i] = Integer.parseInt(temp);
    		}
    		else if(temp.matches("-?(\\d+\\.?|\\d*.\\d+)")) { // check float
    			result[i] = Double.parseDouble(temp);
    		}
    		else {
    			result[i] = temp;
    		}
    	}
    	return result;
    }

    
    
    private static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
    }
    
    
    
    public static void main(String[] args) throws IOException {
		Map<Integer, NgiLayer> layers = NgiParser.parse("(B010)수치지도_376081986_2018_00000816502746.ngi", "MS949").getLayers();
		System.out.println(layers.size());
		for(Map.Entry<Integer, NgiLayer> entry : layers.entrySet()) {
			NgiLayer layer = entry.getValue();
			if(!layer.name.startsWith("F001")) continue;
			System.out.println(layer.name);
			for(Map.Entry<Integer, NgiElement<?>> entry1 : layer.data.entrySet()) {
				NgiElement<?> value = entry1.getValue();
				System.out.println(entry1.getKey() + ": " + value.getRowData("등고수치"));
			}
		}
	}
}
