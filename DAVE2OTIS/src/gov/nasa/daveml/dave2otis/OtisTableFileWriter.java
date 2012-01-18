/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.daveml.dave2otis;

import gov.nasa.daveml.dave.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author ebjackso
 */
class OtisTableFileWriter extends FileWriter {
    
    int tableNumber, tableRefNumber;
    Model  ourModel;
    String indent; // indent for table values
    int    lineWrapLength; /** max chars per line in tables */

    public OtisTableFileWriter(Model theModel, String tableFileName) throws IOException {
        super( tableFileName );
        ourModel = theModel;
        indent = "         ";
        lineWrapLength = 72;
    }
    
    /**
     *
     * Adds newline to the end of each write.
     *
     * @param cbuf String containing text to write
     *
     */

    private void writeln( String cbuf ) throws IOException
    {
            super.write( cbuf + "\n" );
    }
    
    /**
     * Adds just a newline
     */
    
    private void writeln() throws IOException
    {
        this.writeln("");
    }

    
    void generateTableDescription(BlockFuncTable bft) {
        String outVarID = bft.getOutputVarID();
        FuncTable ft    = bft.getFunctionTableDef();
        int[] dims      = ft.getDimensions();
        int numDims     = dims.length;
        String gtID     = ft.getGTID();
        
        try {               
            this.writeTable(bft);
        } catch (IOException ex) {
            Logger.getLogger(OtisTableFileWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeTable(BlockFuncTable bft) throws IOException {
        FuncTable ft    = bft.getFunctionTableDef();
        String outVarID = bft.getOutputVarID();
        int[] dims      = ft.getDimensions();
        int numDims     = dims.length;
        
        // write header
        
        writeln(indent + outVarID);
        writeln(indent + outVarID);
        writeln(indent + "1.0");
        writeln(indent + "* " + "no description");
        writeln(indent + "1");
        writeln(indent + "NCOEF 1");
        write(  indent + "* this term is a function of ");
        if (numDims == 1)
            write("one thing: ");
        else
            write(numDims + " things: ");
        for (int dim = numDims; dim >= 1; dim-- ) {
            write( bft.getVarID(dim) );
            if (dim > 2)
                write( ", " );
            if (dim == 2)
                write( " and ");
        }
        writeln();
        writeln(indent + numDims);
        
        // WRITE BREAKPOINTS (INDEPENDENT VALUES)
        
        // note that we work backwards in writing independent values
        // as OTIS's convention is first breakpoint varies fastest
        // whereas DAVE-ML the last dimension varies fastest
        
        for (int dim = numDims; dim >= 1; dim-- ) {
       
            String inVarID  = bft.getVarID(dim); // convert to port (1-based) number
            String bpID     = ft.getBPID(dim);
            ArrayList<Double> bps  = ourModel.getBPSetByID(bpID).values();
            Iterator<Double> bpIt  = bps.iterator();
            
            writeln(indent + inVarID);
            writeln(indent + "* number of " + inVarID + "s");
            writeln(indent + bps.size());
            writeln(indent + "* " + inVarID + " values");
            String origIndent = indent;
            indent = indent + "      "; // add six spaces
            
            String buffer = indent;
            
            while (bpIt.hasNext()) {
                double breakpointVal = bpIt.next();
                String testBuffer = buffer + breakpointVal + "  ";
                if (testBuffer.length() > lineWrapLength) {
                    writeln(buffer);
                    buffer = indent + breakpointVal + "  ";
                } else {
                    buffer = testBuffer;
                }
            }
            writeln(buffer);
            indent = origIndent;
        }
        
        // WRITE TABLE VALUES (DEPENDENT VALUES)
        
        // these are in the stored order, since we wrote breakpoints 
        // in reverse order
        ArrayList<Double> pts = ft.getValues();
        Iterator<Double> ptIt = pts.iterator();

        // duplicate the dims array (vector of integers giving table dimension)
        int[] coords = dims.clone();
        
        // zero out the array
        for (int i = 0; i < coords.length; i++) {
            coords[i] = 0;
        }
        
        // write top-level header
        writeln(  indent + "* " + outVarID + " values for various " +
                bft.getVarID(numDims));
        
        // recursively write comment header and last bit of table
        writeIndepValHdr(bft, ptIt, coords, 1);

        writeln();
        writeln();
    }

    private void writeIndepValHdr(BlockFuncTable bft, Iterator<Double> ptIt, 
            int[] coords, int dim ) throws IOException {
        FuncTable ft    = bft.getFunctionTableDef();
        int[] dims      = ft.getDimensions();
        int numDims     = dims.length;
        if (dim < numDims) {
            String bpID = ft.getBPID(dim);
            ArrayList<Double> bps = ourModel.getBPSetByID(bpID).values();
            for (int i = 0; i < bps.size(); i++ ) {
                coords[dim] = i; // increment along this breakpoint dimension
                double bpVal = bps.get(coords[dim]);
                writeln(indent + "* for " + bft.getVarID(dim) + " = " + bpVal + " ");
                writeIndepValHdr(bft, ptIt, coords, dim+1);
            }
            
        } else { // last dimension shows final-dimension vector at given coordinates

            String lastBpID = ft.getBPID(numDims);
            int numLastBps = ourModel.getBPSetByID(lastBpID).values().size();
            
            String buffer = indent + "  ";
            for (int i = 0; i < numLastBps; i++) {
                double val = ptIt.next();
                String testBuffer = buffer + val + "  ";
                if (testBuffer.length() > lineWrapLength) {
                    writeln(buffer);
                    buffer = indent + "  " + val + "  ";
                } else {
                    buffer = testBuffer;
                }
            }
            writeln(buffer);
        }
    }
}