package com.opsvision.monitoring.utils;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpGet {
	final static Logger logger = Logger.getLogger(SnmpGet.class);

	public static ResponseEvent doGet(String host, String port, String communityString, String oidValue) {
		Address targetAddress = GenericAddress.parse("udp:" + host + "/" + port);

		TransportMapping transport = null;
		Snmp snmp = null;

		try {
			transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			transport.listen();

		} catch (NullPointerException | IOException e) {
			// Uh oh - log the error and return a null (hopefully we can catch it later)
			logger.error("Error with transport mapping: " + e.getMessage());
			return null;
		}

		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(communityString));
		target.setAddress(targetAddress);
		target.setRetries(3);
		target.setTimeout(3000); // 3 seconds
		target.setVersion(SnmpConstants.version1);

		// Create the PDU object
		PDU pdu = new PDU();
		pdu.add(new VariableBinding(new OID(oidValue)));
		pdu.setType(PDU.GET);
		pdu.setRequestID(new Integer32(1));

		// Perform the Get
		//PDU responsePDU = null;
		ResponseEvent response = null;
		try {
			response = snmp.get(pdu, target);

		} catch (Exception e) {
			logger.error(e.getMessage());

		} finally {
			try {
				snmp.close();

			} catch (Exception e) {}
		}

		return response;
	}
}
