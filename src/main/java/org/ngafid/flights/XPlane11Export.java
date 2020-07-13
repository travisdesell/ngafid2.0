package org.ngafid.flights;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.HashMap;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import org.ngafid.WebServer;
import static org.ngafid.flights.XPlaneParameters.*;

/**
 * Create exports for X-Plane 11
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class XPlane11Export extends XPlaneExport{

	/**
	 * {inheritDoc}
	 */
	public XPlane11Export(int flightId){
		super(flightId);
	}

	/**
	 * {inheritDoc}
	 * */
	@Override
	public StringWriter export(){
		HashMap<String, Object> scopes = new HashMap<String, Object>();

		scopes.put(ENDL, POSIX_ENDL);
		scopes.put(ACFT, ACFT.toUpperCase()+","+xplaneNames.get(flight.getAirframeType())+",");
		scopes.put(TAIL, TAIL.toUpperCase()+","+flight.getTailNumber()+",");

		StringBuffer sb = new StringBuffer();

		int length = parameters.get(ALT_MSL).size();

		for (int i = 0; i < length; i++) {
			//make sure we dont log where the GPS wasn't recording coordinates as this will 
			//cause X-Plane to crash
			if(!Double.isNaN(parameters.get(LONGITUDE).get(i))
			&& !Double.isNaN(parameters.get(LATITUDE).get(i))){
				sb.append("DATA, " + i + "," + NULL_DATA + parameters.get(LONGITUDE).get(i) + "," +
					parameters.get(LATITUDE).get(i) +	"," + parameters.get(ALT_MSL).get(i) + "," +
					getZeros(4) + parameters.get(PITCH).get(i) + "," + parameters.get(ROLL).get(i) + "," +
					parameters.get(HEADING).get(i) + "," + parameters.get(IAS).get(i) + getZeros(70) +
					parameters.get(E1_EGT).get(i) + getZeros(19) + "\n");
			}
		}

		String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "template.fdr";

		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache mustache = mf.compile(templateFile);

		scopes.put(DATA, sb.toString());
		scopes.put(COMM, COMM.toUpperCase()+",Flight " + flight.getId() +",");

		StringWriter stringOut = new StringWriter();

		try{
			mustache.execute(new PrintWriter(stringOut), scopes).flush();
		}catch(IOException e){
			stringOut.write(e.toString());
		}

		return stringOut;

	}
}

