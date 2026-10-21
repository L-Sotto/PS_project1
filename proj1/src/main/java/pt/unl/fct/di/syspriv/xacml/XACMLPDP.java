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

import pt.unl.fct.di.syspriv.storage.Hibernate;

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

    
    private void addGdprState(Map<String, String> resourceAttrs, Object resource) {
        if (resource instanceof Patient p) {
            resourceAttrs.put("erasure-requested",Boolean.toString(Boolean.TRUE.equals(p.getErasureRequested())));
            resourceAttrs.put("retention-expired",Boolean.toString(p.getRetentionUntil() != null&& p.getRetentionUntil().isBefore(java.time.LocalDateTime.now())));

        } else if (resource instanceof Employee e) {
            resourceAttrs.put("erasure-requested",Boolean.toString(Boolean.TRUE.equals(e.getErasureRequested())));

            resourceAttrs.put("retention-expired",Boolean.toString( e.getRetentionUntil() != null&& e.getRetentionUntil().isBefore(java.time.LocalDateTime.now())));
        }
        
    }
    


    public boolean checkAccess(Object requester, Object resource, String context) throws Exception {
        return checkAccess(requester, resource, context, "read");
    }
    
    public boolean checkAccess(Object requester, Object resource, String context, String action) throws Exception {
        Map<String, String> subjectAttrs = new HashMap<>();
        Map<String, String> resourceAttrs = new HashMap<>();
        Map<String, String> actionAttrs = new HashMap<>();
        Map<String, String> envAttrs = new HashMap<>();

        
       
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

        addGdprState(resourceAttrs, resource);
        addConsentAttributes(resourceAttrs, resource);
        envAttrs.put("context", context);

        //actionAttrs.put("action-id", "read");


        actionAttrs.put("action-id", action);

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
    

    // private void addConsentAttributes(Map<String, String> resourceAttrs,Object resource) {

    //     List<ConsentRecord> records = null;

    //     if (resource instanceof Patient p) {
    //         records = p.getConsentRecords();
    //     } else if (resource instanceof Employee e) {
    //         records = e.getConsentRecords();
    //     }

    //     boolean marketingGranted = false;
    //     boolean marketingWithdrawn = false;
    //     boolean researchGranted = false;

    //     if (records != null) {
    //         for (ConsentRecord record : records) {
    //             if (record.getPurpose() == null) {
    //                 continue;
    //             }

    //             String purpose = record.getPurpose().trim().toLowerCase(Locale.ROOT);
    //             boolean granted = record.isGranted();
    //             boolean withdrawn = record.getWithdrawnAt() != null;

    //             if ("marketing".equals(purpose)) {
                    
    //                 if (granted && !withdrawn) {
    //                     marketingGranted = true;
    //                 }
    //                 if (withdrawn) {
    //                     marketingWithdrawn = true;
    //                 }
    //             } else if ("research".equals(purpose)) {
    //                 if (granted && !withdrawn) {
    //                     researchGranted = true;
    //                 }
    //             }
    //         }
    //     }
    //     resourceAttrs.put( "consent-marketing", Boolean.toString(marketingGranted && !marketingWithdrawn));
    //     resourceAttrs.put("consent-withdrawn", Boolean.toString(marketingWithdrawn));
    //     resourceAttrs.put("consent-research", Boolean.toString(researchGranted));
    // }


    private List<ConsentRecord> loadConsent(String type, Long subjectId) {
        if (subjectId == null) return List.of();
        String field = "PATIENT".equals(type) ? "patientSubjectId" : "employeeSubjectId";
        return Hibernate.getInstance().jpql(
            "FROM ConsentRecord c WHERE c." + field + " = " + subjectId, ConsentRecord.class);
    }

    private void addConsentAttributes(Map<String, String> resourceAttrs, Object resource) {
        List<ConsentRecord> records = null;

        if (resource instanceof Patient p) {
            records = loadConsent("PATIENT", p.getId());
        } else if (resource instanceof Employee e) {
            records = loadConsent("EMPLOYEE", e.getId());
        } else if (resource instanceof Appointment a && a.getPatient() != null) {
            records = loadConsent("PATIENT", a.getPatient().getId());
        } else if (resource instanceof Payment p && p.getPatient() != null) {
            records = loadConsent("PATIENT", p.getPatient().getId());
        }
        ConsentRecord latestMarketing = null;
        ConsentRecord latestResearch = null;

        if (records != null) {
            for (ConsentRecord record : records) {
                if (record == null || record.getPurpose() == null) {
                    continue;
                }

                String purpose = record.getPurpose().trim().toLowerCase(Locale.ROOT);
                if ("marketing".equals(purpose)) {
                    if (latestMarketing == null
                            || isLaterConsent(record, latestMarketing)) {
                        latestMarketing = record;
                    }
                } else if ("research".equals(purpose)) {
                    if (latestResearch == null
                            || isLaterConsent(record, latestResearch)) {
                        latestResearch = record;
                    }
                }
            }
        }

        boolean marketingGranted = isCurrentlyGranted(latestMarketing);
        boolean marketingWithdrawn = latestMarketing != null&& latestMarketing.getWithdrawnAt() != null;
        boolean researchGranted = isCurrentlyGranted(latestResearch);

        resourceAttrs.put("consent-marketing", Boolean.toString(marketingGranted));
        resourceAttrs.put("consent-withdrawn",Boolean.toString(marketingWithdrawn));
        resourceAttrs.put( "consent-research",Boolean.toString(researchGranted));
    }

    private boolean isCurrentlyGranted(ConsentRecord record) {
        return record != null&& record.isGranted() && record.getWithdrawnAt() == null;
    }

    private boolean isLaterConsent(ConsentRecord candidate, ConsentRecord current) {
        java.time.LocalDateTime candidateTime = candidate.getWithdrawnAt() != null
                        ? candidate.getWithdrawnAt()
                        : candidate.getGrantedAt();

        java.time.LocalDateTime currentTime =current.getWithdrawnAt() != null
                        ? current.getWithdrawnAt()
                        : current.getGrantedAt();
        if (candidateTime == null) {
            return false;
        }

        if (currentTime == null) {
            return true;
        }

        return candidateTime.isAfter(currentTime);
    }



}
