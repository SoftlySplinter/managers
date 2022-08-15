package dev.galasa.zos.internal;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IResourceManagement;

public class ZosPortResourceMonitor implements Runnable {
	
	private final Log logger = LogFactory.getLog(this.getClass());

	private final IFramework 				 framework;
	private final IResourceManagement 		 resourceManagement;
	private final IDynamicStatusStoreService dss;
	
	public ZosPortResourceMonitor(IFramework framework, IResourceManagement resourceManagement, IDynamicStatusStoreService dss, ZosResourceManagement zosResourceManagement, 
										IConfigurationPropertyStoreService cps) {
		this.framework = framework;
		this.resourceManagement = resourceManagement;
		this.dss = dss;
        this.logger.info("zOS port provisioning resource monitor initialised");
    }
	
	@Override
	public void run() {
		logger.info("Monitoring for z/OS ports held up in allocated status");
		
		try {
			
			// Get the list of ports allocated in the DSS
			Map<String, String> zosPortsInDss = dss.getPrefix("zosport");
			Set<String> allActiveRuns = this.framework.getFrameworkRuns().getActiveRunNames();
			
			// Iterate through all z/OS ports stored in the DSS
			for (String prefix : zosPortsInDss.keySet()) {
	
				// Delete DSS port allocation to run not active
				if (!allActiveRuns.contains(zosPortsInDss.get(prefix))) {
					deleteDss(prefix.split(".")[1], zosPortsInDss.get(prefix));
				}
			}
		} catch (Exception e) {
			logger.error("Failure during scanning DSS for z/OS ports");
		}		
		
        this.resourceManagement.resourceManagementRunSuccessful();
        logger.info("Finished cleaning up z/OS port allocation");
	}
	
	public void runFinishedOrDeleted(String runName) {
		
		try {
			
			// Get list of ports allocated in the DSS
			Map<String, String> zosPortsInDss = dss.getPrefix("zosport");	
			
			for (String prefix : zosPortsInDss.keySet()) {
				
				// This port allocation belongs to the run
				if (zosPortsInDss.get(prefix).equals(runName)) {
					deleteDss(prefix.split(".")[1], zosPortsInDss.get(prefix));	
				}
			}
		} catch (Exception e) {
			logger.error("Failure cleaning up z/OS ports for finished run " + runName);
		}
	}
	
	private void deleteDss(String port, String run) {
		logger.info("Freeing port " + port + " allocated to run " + run + " which has finished");
		
		try {
			ZosPoolPorts.deleteDss(port, run, dss);
		} catch (Exception e) {
			logger.error("Failure in discarding z/OS port " + port + " allocated to run " + run);
		}
	}
}
