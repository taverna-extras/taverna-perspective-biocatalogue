package io.github.tavernaextras.biocatalogue.integration.health_check;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.biocatalogue.x2009.xml.rest.MonitoringStatusLabel;
import org.biocatalogue.x2009.xml.rest.Service;
import org.biocatalogue.x2009.xml.rest.ServiceTest;
import org.biocatalogue.x2009.xml.rest.TestScript;

import io.github.tavernaextras.biocatalogue.model.SoapOperationIdentity;
import io.github.tavernaextras.biocatalogue.model.Util;
import io.github.tavernaextras.biocatalogue.model.connectivity.BioCatalogueClient;
import org.apache.taverna.activities.wsdl.WSDLActivity;
import org.apache.taverna.activities.wsdl.WSDLActivityConfigurationBean;
import io.github.tavernaextras.biocatalogue.MainComponentFactory;
import org.apache.taverna.visit.VisitReport;
import org.apache.taverna.visit.VisitReport.Status;
import org.apache.taverna.workflowmodel.health.HealthChecker;


/**
 * A {@link HealthChecker} for a {@link WSDLActivity}.
 *
 * @author Sergejs Aleksejevs
 */
public class BioCatalogueWSDLActivityHealthChecker implements HealthChecker<WSDLActivity>
{
  private static final int MILLIS_IN_THE_PAST_FOR_OLDEST_MONITORING_DATA = 48 * 60 * 60 * 1000;  // 48hrs
  
  
  private Logger logger;
  
  public BioCatalogueWSDLActivityHealthChecker() {
    logger = Logger.getLogger(BioCatalogueWSDLActivityHealthChecker.class);
  }
  
  
  public boolean canVisit(Object subject) {
    return (subject instanceof WSDLActivity);
  }
  
  
  public VisitReport visit(WSDLActivity activity, List<Object> ancestors)
  {
    WSDLActivityConfigurationBean configBean = activity.getConfiguration();
    SoapOperationIdentity soapOpIdentity = new SoapOperationIdentity(configBean.getWsdl(), configBean.getOperation(), null);
    
    try {
      // make BioCatalogue API request to fetch the data
      Service serviceWithMonitoringData = BioCatalogueClient.getInstance().lookupParentServiceMonitoringData(soapOpIdentity);
      MonitoringStatusLabel.Enum serviceStatusLabel = null;
      
      
      VisitReport.Status status = null;
      String visitReportLabel = null;
      String visitReportExplanation = null;
      List<VisitReport> subReports = new ArrayList<VisitReport>();
      
      
      if (serviceWithMonitoringData == null) {
        // BioCatalogue doesn't "know" about this service - it appears not to be registered;
        // --> nothing to report to Taverna
        return (null);
      }
      else if (serviceWithMonitoringData.getLatestMonitoringStatus() == null) {
        // BioCatalogue "knows" this service, but for some reason there was no monitoring data available;
        // possibly an API change? either way --> nothing to report to Taverna
        return (null);
      }
      else
      {
        Calendar lastCheckedAt = serviceWithMonitoringData.getLatestMonitoringStatus().getLastChecked();
        String agoString = Util.getAgoString(lastCheckedAt, Calendar.getInstance(), MILLIS_IN_THE_PAST_FOR_OLDEST_MONITORING_DATA);
        if (agoString == null) {
          return (null);
        }
        
        serviceStatusLabel = serviceWithMonitoringData.getLatestMonitoringStatus().getLabel();
        switch (serviceStatusLabel.intValue()) {
          case MonitoringStatusLabel.INT_PASSED:
            visitReportLabel = "Service Catalogue: all tests passed " + agoString;
            visitReportExplanation = "The Service Catalogue reports that all available tests for this WSDL service have " +
            		                     "been successful. They have been last executed " + agoString;
            status = Status.OK;
            break;
                  
          case MonitoringStatusLabel.INT_WARNING:
          case MonitoringStatusLabel.INT_FAILED:
            visitReportLabel = "Service Catalogue: some tests failed " + agoString;
            visitReportExplanation = "Some test scripts for this WSDL service have failed";
            
            // only extract data about failing test scripts
            subReports = createTestScriptSubReportsForFailingService(activity, serviceWithMonitoringData);
            if (subReports.size() == 0) {
              // failing tests must have been for endpoint / WSDL location - but not for scripts;
              // Taverna doesn't need to know about the former, as it replicates internal checks
              return (null);
            }
            else {
              // determine the worst status and report as the one of the collection of subreports
              status = VisitReport.getWorstStatus(subReports);
            }
            break;
          
          case MonitoringStatusLabel.INT_UNCHECKED:
            // monitoring record states that the status of this service was not (yet) checked;
            // possibly monitoring on BioCatalogue was switched off before this service was registered;
            // --> nothing to report to Taverna
            return (null);
                  
          default:
            visitReportLabel = "Service Catalogue: unknown monitoring status received - \"" + serviceStatusLabel.toString() + "\"";
            visitReportExplanation = "The Service Catalogue has returned a new monitoring status for this service: \"" +
                                     serviceStatusLabel.toString() + "\"\n\n" +
                                     "It has never been used before and probably indicates a change in the Service Catalogue API. " +
                                     "Please report this issue to the Service Catalogue developers.";
            status = Status.WARNING;
            break;
        }
      }
      
      // wrap determined values into a single VisitReport object; then attach data to identify
      // this service in associated VisitExplainer
      VisitReport report = new VisitReport(BioCatalogueWSDLActivityHealthCheck.getInstance(), activity, 
                                           visitReportLabel, BioCatalogueWSDLActivityHealthCheck.MESSAGE_IN_VISIT_REPORT, status, subReports);
      report.setProperty(BioCatalogueWSDLActivityHealthCheck.WSDL_LOCATION_PROPERTY, soapOpIdentity.getWsdlLocation());
      report.setProperty(BioCatalogueWSDLActivityHealthCheck.OPERATION_NAME_PROPERTY, soapOpIdentity.getOperationName());
      report.setProperty(BioCatalogueWSDLActivityHealthCheck.EXPLANATION_MSG_PROPERTY, visitReportExplanation);
      
      return (report);
    }
    catch (Exception e) {
      // not sure what could have happened - it will be visible in the logs
      logger.error("Unexpected error while performing health check for " + 
                   soapOpIdentity.getWsdlLocation() + " service.", e);
      return (null);
    }
  }
  
  
  private List<VisitReport> createTestScriptSubReportsForFailingService(WSDLActivity activity, Service serviceWithMonitoringData)
  {
    List<VisitReport> subReports = new ArrayList<VisitReport>();
    
    try {
      List<ServiceTest> serviceTests = serviceWithMonitoringData.getMonitoring().getTests().getServiceTestList();
      for (ServiceTest test : serviceTests)
      {
        if (test.getTestType().getTestScript() != null &&
            test.getTestType().getTestScript()instanceof TestScript)
        {
          TestScript testScript = test.getTestType().getTestScript();
          
          String agoString = Util.getAgoString(test.getLatestStatus().getLastChecked(), Calendar.getInstance(), 
                                               MILLIS_IN_THE_PAST_FOR_OLDEST_MONITORING_DATA);
          
          // only proceed if this test wasn't run too long ago
          if (agoString != null) {
            String label = "Service Catalogue: \"" + testScript.getName() + "\" test script " + test.getLatestStatus().getLabel();
            VisitReport report = new VisitReport(BioCatalogueWSDLActivityHealthCheck.getInstance(), activity, 
                label, BioCatalogueWSDLActivityHealthCheck.MESSAGE_IN_VISIT_REPORT,
                ServiceMonitoringStatusInterpreter.translateBioCatalogueStatusForTaverna(test.getLatestStatus().getLabel()));
            report.setProperty(BioCatalogueWSDLActivityHealthCheck.WSDL_LOCATION_PROPERTY, activity.getConfiguration().getWsdl());
            report.setProperty(BioCatalogueWSDLActivityHealthCheck.OPERATION_NAME_PROPERTY, activity.getConfiguration().getOperation());
            report.setProperty(BioCatalogueWSDLActivityHealthCheck.EXPLANATION_MSG_PROPERTY,
                               "This test was last executed " + agoString + "." +
                               "\n\n" + StringEscapeUtils.escapeHtml(test.getLatestStatus().getMessage()) +
                               "\n\n---- Test script description ----\n" + StringEscapeUtils.escapeHtml(testScript.getDescription()));
            
            subReports.add(report);
          }
        }
      }
    }
    catch (Exception e) {
      // log the error, but do not terminate the method - maybe some sub reports were successfully
      // generated, in which case at least partial result can be returned
      logger.error("Encountered unexpected problem while trying to generate a collection of sub-reports " +
      		         "for a failing service: " + activity.getConfiguration().getWsdl(), e);
    }
    
    return (subReports);
  }
  
  
  /**
   * Health check for the WSDL activities involves fetching
   * the monitoring status of each activity from BioCatalogue - 
   * this *may* be time consuming.
   */
  public boolean isTimeConsuming() {
    return true;
  }

}
