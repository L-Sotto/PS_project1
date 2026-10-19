package pt.unl.fct.di.syspriv.xacml;

import pt.unl.fct.di.syspriv.util.ConfigLoader;
import pt.unl.fct.di.syspriv.entities.*;

import org.ow2.authzforce.core.pdp.api.*;
import org.ow2.authzforce.core.pdp.api.value.*;
import org.ow2.authzforce.core.pdp.impl.BasePdpEngine;
import org.ow2.authzforce.core.pdp.impl.PdpEngineConfiguration;
import org.ow2.authzforce.xacml.identifiers.XacmlStatusCode;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.DecisionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.Level;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.io.IOUtils;

public class XACMLPDP {

    // Standard XACML category URNs
    private static final String SUBJECT_CATEGORY = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    private static final String RESOURCE_CATEGORY = "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";
    private static final String ACTION_CATEGORY = "urn:oasis:names:tc:xacml:3.0:attribute-category:action";
    private static final String ENV_CATEGORY = "urn:oasis:names:tc:xacml:3.0:attribute-category:environment";

    private final BasePdpEngine pdp;
    private Logger logger;

    public XACMLPDP() throws Exception {
        PdpEngineConfiguration config = PdpEngineConfiguration.getInstance(XACMLPDP.class.getClassLoader().getResource("pdp.xml").getPath());
        this.pdp = new BasePdpEngine(config);
        this.logger = LoggerFactory.getLogger(BasePdpEngine.class);
    }

    private String loadPolicyXml(String policyFile) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(policyFile);
        if (is != null) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        
        if (Files.exists(Paths.get(policyFile))) {
            return new String(Files.readAllBytes(Paths.get(policyFile)), StandardCharsets.UTF_8);
        }
        
        throw new IllegalStateException("Policy file not found: " + policyFile);
    }

    private String idOf(Object o) {
        if (o == null) return null;
        if (o instanceof Employee e) return String.valueOf(e.getId());
        if (o instanceof Patient p) return String.valueOf(p.getId());
        return String.valueOf(o);
    }
    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) map.put(key, value);
    }

    public boolean checkAccess(Object requester, Object resource, String context) throws Exception {
        Map<String, String> subjectAttrs = new HashMap<>();
        Map<String, String> resourceAttrs = new HashMap<>();
        Map<String, String> actionAttrs = new HashMap<>();
        Map<String, String> envAttrs = new HashMap<>();

        // TODO: Place the attributes from the XML into the appropriate HashMap.
       
        // subjectAttrs.put("id", String.valueOf(requester.getClass()));
        if (requester instanceof Patient p) {
            subjectAttrs.put("role", "Patient");
            subjectAttrs.put("id", String.valueOf(p.getId()));
        } else if (requester instanceof Employee e) {
            subjectAttrs.put("role", e.getRole());          
            subjectAttrs.put("id", String.valueOf(e.getId()));
        }

        if (resource instanceof Patient p) {
            resourceAttrs.put("id", String.valueOf(p.getId()));
            resourceAttrs.put("resource-type", "PatientData");
            putIfNotNull(resourceAttrs, "primary-doctor", idOf(p.getPrimaryDoctor()));
        } else if (resource instanceof Employee e) {
            resourceAttrs.put("id", String.valueOf(e.getId()));
            resourceAttrs.put("resource-type", "EmployeeData");
        } else if (resource instanceof Appointment a) {
            resourceAttrs.put("id", String.valueOf(a.getId()));
            resourceAttrs.put("resource-type", "AppointmentData");
            putIfNotNull(resourceAttrs, "employee-id", idOf(a.getEmployee()));
            putIfNotNull(resourceAttrs, "patient-id", idOf(a.getPatient()));
        } else if (resource instanceof Payment p) {
            resourceAttrs.put("id", String.valueOf(p.getId()));
            resourceAttrs.put("resource-type", "PaymentData");
            putIfNotNull(resourceAttrs, "patient-id", idOf(p.getPatient()));
        }
        envAttrs.put("context", context);

        //actionAttrs.put("action-id", "read");


        actionAttrs.put("action-id", "read");

        DecisionRequestBuilder<?> requestBuilder = pdp.newRequestBuilder(-1, -1);

        addAttributes(requestBuilder, SUBJECT_CATEGORY, subjectAttrs);
        addAttributes(requestBuilder, RESOURCE_CATEGORY, resourceAttrs);
        addAttributes(requestBuilder, ACTION_CATEGORY, actionAttrs);
        addAttributes(requestBuilder, ENV_CATEGORY, envAttrs);

        var request = requestBuilder.build(true);
        logger.debug(request.toString());
        DecisionResult result = pdp.evaluate(request);
        
        logger.debug(result.toString());
        return result.getDecision() == DecisionType.PERMIT;
    }

    private void addAttributes(DecisionRequestBuilder<?> requestBuilder, String category, Map<String, String> attrs) {
        for (var entry : attrs.entrySet()) {
            AttributeFqn fqn = AttributeFqns.newInstance(category, Optional.empty(), entry.getKey());
            AttributeBag<?> bag = Bags.singletonAttributeBag(
                StandardDatatypes.STRING,
                new StringValue(entry.getValue())
            );
            requestBuilder.putNamedAttributeIfAbsent(fqn, bag);
        }
    }
}
