/**
 * Loss of control calculator for calculating exceedences
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;

import java.util.Map;
import java.util.Optional;

import java.nio.file.Path;

import java.lang.Math;

import static org.ngafid.flights.CalculationParameters.*;

public class LossOfControlCalculation extends Calculation {
    private File file;
    private Optional<PrintWriter> pw;

    /**
     * Constructor 
     *
     * @param flight the flight to calculate for
     * @param cachedParameters a set parameters that includes the already-calculated AOA Simple 
     * and stall probability
     */
    public LossOfControlCalculation(Flight flight, Map<String, DoubleTimeSeries> cachedParameters) { 
        super(flight, lociParamStrings, cachedParameters);

        cachedParameters.put(PRO_SPIN_FORCE, new DoubleTimeSeries(PRO_SPIN_FORCE, "double"));
        cachedParameters.put(YAW_RATE, new DoubleTimeSeries(YAW_RATE, "double"));
        cachedParameters.put(HDG + LAG_SUFFIX + YAW_RATE_LAG, cachedParameters.get(HDG).lag(YAW_RATE_LAG));

        this.pw = Optional.empty();
    }

    /**
     * Constructor 
     *
     * @param flight the flight to calculate for
     * @param cachedParameters a set parameters that includes the already-calculated AOA Simple 
     * @param path the filepath ROOT directory to print logfiles too
     * */
    public LossOfControlCalculation(Flight flight, Map<String, DoubleTimeSeries> cachedParameters, Path path) { 
        this(flight, cachedParameters);
        //try to create a file output 
        this.createFileOut(path);
    }


    /**
     * Creates an output filestream
     *
     * @param path the path of the file to write
     */
    private void createFileOut(Path path) {
        String filename = "/flight_"+ this.flight.getId() +".out";

        file = new File(path.toString()+filename);
        System.out.println("LOCI_CALCULATOR: printing to file " + file.toString() + " for flight #" + this.flight.getId());

        try {
            this.pw = Optional.of(new PrintWriter(file));
        } catch(FileNotFoundException e) {
            System.err.println("File not writable!");
            System.exit(1);
        }
    }

    /**
     * Gets the lagged difference of two points in a {@link DoubleTimeSeries}
     * The difference is between the current index and the one prior
     * 
     * @param series the {@link DoubleTimeSeries} to lag
     * @param index the start index
     */
    //private double lag(DoubleTimeSeries series, int index){
        //double currIndex = series.get(index);
        //if(index >= 1) {
            //return currIndex - series.get(index - 1);
        //}
        //return currIndex;
    //}

    /**
     * Calculates the yaw rate at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getYawRate(int index){
        DoubleTimeSeries hdg = this.parameters.get(HDG); 
        DoubleTimeSeries yawRate = this.parameters.get(YAW_RATE);
        DoubleTimeSeries hdgLagged = this.parameters.get(HDG + LAG_SUFFIX + YAW_RATE_LAG);

        double laggedValue = hdgLagged.get(index);
        double value = Double.isNaN(laggedValue) ? 0 : 
            180 - Math.abs(180 - Math.abs(hdg.get(index) - laggedValue) % 360);

        if (yawRate.size() == index) {
            yawRate.add(value);
        }

        return value;
    }

    /**
     * Gets the roll comp at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getRollComp(int index){
        DoubleTimeSeries roll = this.parameters.get(ROLL);
        return roll.get(index) * COMP_CONV;
    }

    /**
     * Gets the yaw comp at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getYawComp(int index){
        return this.getYawRate(index) * COMP_CONV;
    }
    
    /**
     * Gets the VR comp at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getVRComp(int index){
        return ((this.parameters.get(TAS_FTMIN).get(index) / 60) * this.getYawComp(index));
    }

    /**
     * Gets the CT comp at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getCTComp(int index){
        return Math.sin(this.getRollComp(index)) * 32.2;
    }

    /**
     * Gets the cord comp at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getCordComp(int index){
          return Math.abs(this.getCTComp(index) - this.getVRComp(index)) * 100;
    }

    /**
     * Gets the pro spin force at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getProSpin(int index){
        DoubleTimeSeries psf = this.parameters.get(PRO_SPIN_FORCE);
        
        double value = Math.min((this.getCordComp(index) / PROSPIN_LIM), 100);

        if (psf.size() == index) {
            psf.add(value);
        }

        return value;
    }

    /**
     * Calculates the LOC-I probability at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index as a percentage
     */
    private double calculateProbability(int index) {
        double prob = (this.parameters.get(STALL_PROB).get(index) * this.getProSpin(index));
        return prob;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDatabase(){
        this.parameters.get(LOCI).updateDatabase(connection, super.flight.getId());
        this.parameters.get(PRO_SPIN_FORCE).updateDatabase(connection, super.flight.getId());
        this.parameters.get(YAW_RATE).updateDatabase(connection, super.flight.getId());
    }

    /**
     * Calculates the loss of control probability
     *
     * @return a floating-point percentage of the probability of loss of control
     */
    public void calculate() {
        this.printDetails();
        
        DoubleTimeSeries loci;
        this.parameters.put(LOCI, 
                (loci = new DoubleTimeSeries(LOCI, "string")));
        DoubleTimeSeries altAGL = this.parameters.get(ALT_AGL);

        for(int i = 0; i < altAGL.size(); i++) {
            loci.add(this.calculateProbability(i) / 100);
        }

        if(this.pw.isPresent()) {
            this.writeFile(loci, this.parameters.get(STALL_PROB));
        }
    }

    /**
     * Writes the data to a file for analysis purposes
     *
     * @pre {@link Optional} pw has a wrapped instance of {@link PrintWriter}
     *
     * @param loci the loss of contorl {@link DoubleTimeSeries}
     * @param sProb the stall probability {@link DoubleTimeSeries}
     */
    public void writeFile(DoubleTimeSeries loci, DoubleTimeSeries sProb){
        PrintWriter pw = this.pw.get();
        System.out.println("printing to file");
        try{
            pw.println("Index:\t\t\tStall Probability:\t\t\t\tLOC-I Probability:");
            for(int i = 0; i<loci.size(); i++){
                pw.println(i+"\t\t\t"+sProb.get(i)+"\t\t\t\t"+loci.get(i));
            }
            pw.println("\n\nMaximum Values: ");
            pw.println("Stall Probability: "+sProb.getMax()+" LOC-I: "+loci.getMax());

            pw.println("Average Values: ");
            pw.println("Stall Probability: "+sProb.getAvg()+" LOC-I: "+loci.getAvg());
        } catch (Exception e) { 
            e.printStackTrace();
        } finally {
            pw.close();
        }
    }

    /**
     * Writes details about the calcualtion to standard error
     */
    public void printDetails() {
        System.err.println("\n\n");
        System.err.println("------------ LOCI/Stall Probability CALCULATION INFO ------------");
        System.err.println("flight_id: "+flight.getId());
        System.err.println("logfile: "+(file != null ? file.toString() : "None specified."));
        System.err.println("-----------------------------------------------------------------");
        System.err.println("\n\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCalculationName() {
        return LOCI;
    }
}
