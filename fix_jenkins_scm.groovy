import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import hudson.plugins.git.BranchSpec

try {
    def jobName = "Automation-Framework"
    def job = Jenkins.instance.getItem(jobName)

    if (job != null) {
        println "Found job: $jobName"
        
        // Define Remote Config
        // UserRemoteConfig(String url, String name, String refspec, String credentialsId)
        def remoteConfig = new UserRemoteConfig("git@github.com:SanMantri/test-auto-framework.git", null, null, null)
        
        // Define SCM
        // GitSCM(List<UserRemoteConfig> remoteConfigs, List<BranchSpec> branches, boolean doGenerateSubmoduleConfigurations, 
        //        List<SubmoduleConfig> submoduleCfg, GitRepositoryBrowser browser, String gitTool, List<GitSCMExtension> extensions)
        def scm = new GitSCM(
            Collections.singletonList(remoteConfig),
            Collections.singletonList(new BranchSpec("*/main")),
            false,
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList()
        )
        
        // Define Flow Definition
        def flowDef = new CpsScmFlowDefinition(scm, "Jenkinsfile")
        flowDef.setLightweight(false)
        
        // Update Job
        job.setDefinition(flowDef)
        job.save()
        println "SUCCESS: Job SCM updated to use GitHub."
    } else {
        println "ERROR: Job '$jobName' not found."
    }
} catch (Exception e) {
    println "ERROR: Failed to update job. " + e.getMessage()
    e.printStackTrace()
}
