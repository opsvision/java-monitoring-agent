package com.opsvision.monitoring.utils;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

public class SnmpWalk {
	final static Logger logger = Logger.getLogger(SnmpWalk.class);

	/**
	 * Once you have a List of TreeEvents, you can process them
	 * using the following sample code.
	 *
	 * for (TreeEvent event : events) {
	 *  if (event != null) {
	 *    if (event.isError()) { continue; }
	 *      VariableBinding[] varBindings = event.getVariableBindings();
	 *      if (varBindings == null || varBindings.length == 0) { continue; }
	 *
	 *      for (VariableBinding varBinding : varBindings) {
	 *        System.out.println(
	 *          varBinding.getOid() + " : " +
	 *          varBinding.getVariable().getSyntaxString() + " : " +
	 *          varBinding.getVariable()
	 *        );
	 *      }
	 *    }
	 *  }
	 *
	 * @param host
	 * @param port
	 * @param communityString
	 * @param baseOID
	 */
	public static List<TreeEvent> doWalk(String host, String port, String communityString, String baseOID) {
		Address targetAddress = GenericAddress.parse("udp:" + host + "/" + port);

		TransportMapping transport = null;
		Snmp snmp = null;
		OID oid = null;

		try {
			transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			transport.listen();

			oid = new OID(baseOID);

		} catch (NullPointerException | IOException e) {
			// Uh oh - log the error and return a null (hopefully we can catch it later)
			logger.error(e.getMessage());
			return null;
		}

		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(communityString));
		target.setAddress(targetAddress);
		target.setRetries(3);
		target.setTimeout(3000); // 3 seconds
		target.setVersion(SnmpConstants.version1);

		// Do an SNMP walk
		TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());

		// Extract the events
		@SuppressWarnings("unchecked")
		List<TreeEvent> events = treeUtils.getSubtree(target, oid);

		try {
			snmp.close();
		} catch (Exception e) {}

		return events;
	}
}
