/*
 *  @author: Manuel Souto Pico, with awesome help from Kos Ivantsov
 *  @date: 2020.05.29
 *  @properties: https://cat.capstan.be/OmegaT/project-assets.json
 */

// in the properties file, instead of literal paths, use patterns with placeholders (*)
// in the properties file, add filters (e.g. "en2021" for PISA 2024, etc.)

/* todo:
 * - merge TMs and put them in TM/_Merged or TM/OmegaT
 * - download glossaries
 * - download style guide data
 */

/*
 *  Installation:
 *  - From OmegaT, go to Options > Access Configuration Folder
 *  - Navigate to the "scripts" folder, "project_changed" subfolder
 *  - Put the script (this file) in the "project_changed" folder
 *  e.g. C:\Users\manuel\AppData\Roaming\OmegaT\scripts\project_changed\container_customizer.groovy
 *
 */

// modules
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import org.omegat.util.StaticUtils

// constants
def prop = project.getProjectProperties()
def tmdir = prop.getTMRoot()
def mldir_str = tmdir + "capps" + container
def mldir = new File(mldir_str)
def proj = prop.projectName
def root = prop.getProjectRoot()
// The container is the first entity in the project name (before the first underscore)
def container = (proj =~ /^[^_]+(?=_)/)[0]

timestamp = new Date().format("YYYYMMddHHmm")
confDir = StaticUtils.getConfigDir()

containers_config = new File(confDir.toString() + File.separator + "containers_config.json")

logFile = new File(confDir.toString() + File.separator + "logs" + File.separator + "container_assets_${container}.log")

// functions
logEcho = { msg ->
    if (logFile.exists()) {
		// todo: if it exists and is not empty, do nothing
        logFile.append(msg + "\n", "UTF-8")
        console.println(msg)
    } else {
		// otherwise, document when the asset was downloaded (just once, at the end of the script)
        logFile.write(msg + "\n", "UTF-8")
        console.println(msg)
    }
}

/*
	prop.getProjectRoot()
	prop.getProjectInternal()
	prop.getSourceRoot()
	prop.getTargetRoot()
	prop.getTMRoot()
	prop.getGlossaryRoot()
	StaticUtils.getConfigDir()
 */

def jsonSlurper = new JsonSlurper()
def containers_config_obj = jsonSlurper.parseFile(containers_config)

console.println "${containers_config_obj}"
return




 // todo:
 // check if the container is in container.properties json file
 //
 // if it is, try to download language _regular_expression_operators
logEcho("="*40 + "\n" + " "*5 + "Container assets management\n" + "="*40)



// if (new File(mldir_str).exists())
// def local_file = new File(path_str + File.separator + "file.zip")

// if mldir folder exists, it means assets have already been downloaded
if (mldir.exists()) {
    console.println "Assets had previously been downloaded. No action required."
    // @Kos: it should also check that this folder contains tmx files (and the same tmx files available for download)
    return
    // System.exit(0) // where 0 is a counter in seconds?
}
// else continue...

/* @Kos: it should check whether a working internet connexion exists:
If none, warns the user:
“You need to be connected to the internet to download langauge assets for this project.
Close this project or close OmegaT, connect to the internet and open the project again.”
If Internet connection is found, continue to the next step.
*/

console.println "Downloading assets to folder /tm/capps..."

def tgt_code = project.projectProperties.targetLanguage.languageCode
def src_code = project.projectProperties.sourceLanguage.languageCode

console.println "Project: '$proj'"
console.println "Path to project: $root"
console.println "Container: '$container'"
console.println "Target language: '$tgt_code'"
console.println "Source language: '$src_code'"

// todo:
// download list
// parse list
// filter list and turn it to array
// loop through array and download entries to mldir
mldir.mkdirs()

def domain = "https://capps.capstan.be/TM/_Merged/"
// list_url = "https://cat.capstan.be/OmegaT/index.php"
def list_url = domain + "list.php"

// new File(mldir + File.separator + "file.txt") << new URL (sourceUrl).getText()
// console.println(mldir + File.separator + "file.txt")

try {
	list_url.toURL().openStream()
	//console.println()
} catch (IOException e) {
    e.printStackTrace()
    return
}

def tm_list_array = list_url.toURL().text.readLines()
//console.println() // to prevent the previous line printing to the console


// todo:
// create /tm/capps only if the array.len > 0

tm_list_array.each { line ->
	// line has class java.lang.String
	// console.println line

	// def pattern = ~/^${domain}${container}_(?:.*?)${tgt_code}/
	// console.println "${domain}${container}"
	def pattern = ~/${domain}${container}_(?:.*?)${tgt_code}_(?:.*?)${src_code}/
	// def pattern = ~/s[oa]m[ie]/
	def m = line =~ pattern
	// assert m instanceof Matcher
	if (m) {
		console.println "match: '" + line + "'"
		// download the file and put it in /tm/capps
		// @Kos: here
	} else {
		// console.println "NO match: " + line
		// throw new RuntimeException("Oops, text not found!")
	}
}

logEcho("-"*40)
// todo: add ${container} to msg
return

// https://www.baeldung.com/groovy-pattern-matching
// http://docs.groovy-lang.org/latest/html/documentation/#_regular_expression_operators
// http://docs.groovy-lang.org/latest/html/documentation/#_match_operator
