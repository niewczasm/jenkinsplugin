package io.jenkins.plugins.hostmonitor;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Set;

/**
 * Pipeline step to update host status
 * Usage: updateHostStatus hostname: 'server1', status: 'ONLINE', message: 'All services running'
 */
public class UpdateHostStatusStep extends Step {
    
    private final String hostname;
    private String status;
    private String message;
    
    @DataBoundConstructor
    public UpdateHostStatusStep(String hostname) {
        this.hostname = hostname;
        this.status = "UNKNOWN";
        this.message = "";
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public String getStatus() {
        return status;
    }
    
    @DataBoundSetter
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    @DataBoundSetter
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(hostname, status, message, context);
    }
    
    private static class Execution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        
        private final String hostname;
        private final String status;
        private final String message;
        
        Execution(String hostname, String status, String message, StepContext context) {
            super(context);
            this.hostname = hostname;
            this.status = status;
            this.message = message;
        }
        
        @Override
        protected Void run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            
            HostMonitorManager manager = HostMonitorManager.getInstance();
            manager.updateHost(hostname, status, message);
            
            if (listener != null) {
                listener.getLogger().println("Updated host status: " + hostname + " -> " + status);
            }
            
            return null;
        }
    }
    
    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
        
        @Override
        public String getFunctionName() {
            return "updateHostStatus";
        }
        
        @NonNull
        @Override
        public String getDisplayName() {
            return "Update Host Monitor Status";
        }
    }
}
