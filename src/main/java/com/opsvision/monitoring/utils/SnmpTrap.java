package com.opsvision.monitoring.utils;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpTrap {
	final static Logger logger = Logger.getLogger(SnmpTrap.class);

	public static void sendTrap(int specificTrap, VariableBinding[] varbinds) {
		PropertyHandler props = PropertyHandler.getInstance();
		String host = props.getValue("monitor.main.trap.destination",
				"10.1.3.69");
		String port = props.getValue("monitor.main.trap.port", "162");

		Address targetAddress = GenericAddress
				.parse("udp:" + host + "/" + port);

		IpAddress agentAddress = null;
		TransportMapping transport = null;
		Snmp snmp = null;

		try {
			agentAddress = new IpAddress(InetAddress.getLocalHost());
			transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

		} catch (NullPointerException | IOException e) {
			logger.error(e.getMessage());
			return;
		}

		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("public"));
		target.setAddress(targetAddress);
		target.setRetries(3);
		target.setTimeout(3000); // 3 seconds
		target.setVersion(SnmpConstants.version1);

		PDUv1 pdu = new PDUv1();
		pdu.setType(PDU.V1TRAP);
		pdu.setEnterprise(new OID("1.3.6.1.4.1.44132.4.3"));
		pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
		pdu.setSpecificTrap(specificTrap);
		pdu.setAgentAddress(agentAddress);
		for (VariableBinding variableBinding : varbinds) {
			pdu.add(variableBinding);
		}

		try {
			snmp.send(pdu, target);

		} catch (IOException e) {
			logger.error("Error sending trap: " + e.getMessage());

		} finally {
			try {
				snmp.close();

			} catch (Exception e) {
			}
		}
	}

}
