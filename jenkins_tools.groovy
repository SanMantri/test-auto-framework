import hudson.model.*
import hudson.tools.*
import hudson.tasks.Maven.MavenInstallation
import hudson.tasks.Maven.MavenInstaller
import jenkins.model.Jenkins

// Configure Maven
def mavenDescriptor = Jenkins.instance.getDescriptorByType(hudson.tasks.Maven.DescriptorImpl.class)
// Assuming Maven is installed via Homebrew at standard location
def mavenInst = new MavenInstallation("Maven3", "/opt/homebrew/bin/mvn", Jenkins.instance)
mavenDescriptor.setInstallations(mavenInst)
mavenDescriptor.save()

// Configure JDK
def jdkDescriptor = Jenkins.instance.getDescriptorByType(hudson.model.JDK.DescriptorImpl.class)
// Assuming JDK 17 is installed via Homebrew at standard location
def jdkInst = new hudson.model.JDK("JDK17", "/opt/homebrew/opt/openjdk") 
jdkDescriptor.setInstallations(jdkInst)
jdkDescriptor.save()

println "Success: Configured Maven3 and JDK17"
