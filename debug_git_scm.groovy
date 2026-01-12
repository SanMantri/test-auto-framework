import hudson.plugins.git.GitSCM
try {
    println "=== GitSCM Constructors ==="
    GitSCM.class.getConstructors().each { output ->
        println output
    }
    println "==========================="
} catch (Exception e) {
    e.printStackTrace()
}
