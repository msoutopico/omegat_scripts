/* :name = TA - Translatability Assessment :description =Runs VF4TA on the project and adds TA notes in the translation field.
 *
 *  @version: 1.2.0
 *  @author: Manuel Souto Pico
 *  @date: 2020.08.30
 *  @properties: https://cat.capstan.be/OmegaT/project-assets.json
 */

// user options
include_suggestions = true
// save_as_tmx2source = true

/*
 * OPTIONS: 	The parametter `include_suggestions` above determines whether suggestions are included also (as
 * 			well as comments). It is enabled by default but can be disabled by the user:
 * 			for that, simply replace "true" with "false".
 *
 * QUESTIONS:	manuel.souto@capstan.be
 *
 */










































/*
 * @changes: 1.2.0: added notes do tmx2source
*/

// first check: is the project being opened?
import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE
title = "Translatability Assessment"
// final def title = "Translatability Assessment"


// manual:
// first check: is a project open?
if ( !project.projectProperties ) {
	noprj_prompt = "No project is open, a project needs to be open to run this script."
	console.println(noprj_prompt)
	noprj_prompt.alert()
	return
}

// modules
import groovy.util.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.omegat.util.StaticUtils
import java.security.MessageDigest
import static groovy.io.FileType.FILES


import org.omegat.core.events.IApplicationEventListener

import groovy.swing.SwingBuilder
import groovy.util.XmlSlurper
//import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.FlowLayout
import java.awt.GridBagConstraints as GBC
import java.awt.GridBagLayout as GBL
import java.nio.file.Paths
import java.util.zip.ZipFile
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JProgressBar
import javax.swing.WindowConstants
import org.apache.commons.io.FileUtils
import org.omegat.CLIParameters
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

@Grab('com.xlson.groovycsv:groovycsv:1.3')
import static com.xlson.groovycsv.CsvParser.parseCsv
import org.omegat.util.TMXWriter
import org.omegat.util.OConsts
import org.omegat.gui.main.ProjectUICommands;

// unused
utils = (StringUtil.getMethods().toString().findAll("makeValidXML")) ? StringUtil : StaticUtils

String.metaClass.confirm = { ->
    showConfirmDialog null, delegate, title, YES_NO_OPTION
}

String.metaClass.alert = { ->
    showMessageDialog null, delegate, title, INFORMATION_MESSAGE
}


// the script starts here if a project is open
console.println("="*40 + "\n" + " "*5 + "Translatability Assessment\n" + "="*40)

// constants
digest = MessageDigest.getInstance("MD5") // not using def gives the variable global scope
// def prop = project.projectProperties
prop = project.getProjectProperties()
proj_name = prop.projectName
// The container is the first entity in the project name (before the first underscore)
container = (proj_name =~ /^[^_]+/)[0]
// tgtlang_iso_match = (proj_name =~ /(?<=_)[a-z]{3}-[A-Z]{3}(?=_)/) // fragile -> get code from settings


projectRoot = prop.projectRootDir
// projectRoot = prop.getProjectRoot()
omegat_dir = prop.getProjectInternal()
glossary_dir = prop.getGlossaryRoot()
tmdir_fs = prop.getTMRoot() // fs = forward slash
tmdir = new File(tmdir_fs)
reftm_dir_str = tmdir_fs + "ref"
// reftm_dir = new File(reftm_dir_str) // the File object includes backslashes on Windows


timestamp = new Date().format("YYYYMMddHHmm")
config_dir = StaticUtils.getConfigDir()
config_file = new File(config_dir + "containers_config.properties")

tgt_code = 			project.projectProperties.targetLanguage
src_code = 			project.projectProperties.sourceLanguage
tgt_lang_subtag = 	project.projectProperties.targetLanguage.languageCode
// def src_lang_subtag = 	project.projectProperties.sourceLanguage.languageCode
// def tgt_region_subtag = project.projectProperties.targetLanguage.countryCode
// def src_region_subtag = project.projectProperties.sourceLanguage.countryCode

// functions
log_echo = { msg ->
    if (log_file.exists()) {
		// todo: if it exists and is not empty, do nothing
        log_file.append(msg + "\n", "UTF-8")
        console.println(msg)
    } else {
		// otherwise, document when the asset was downloaded (just once, at the end of the script)
        log_file.write(msg + "\n", "UTF-8")
        console.println(msg)
    }
}

// https://www.baeldung.com/groovy-file-read
String read_file_string(String path_to_file) {
    File file = new File(path_to_file)
    String file_content = file.text
    return file_content
}

def download_asset(url_to_remote_asset, assets_dir) {
	try {
		def dest_file = new File(assets_dir.toString() + File.separator + remote_asset_name)
		dest_file.withOutputStream { output_stream  ->
			def url = new URL(url_to_remote_asset).openConnection()
			/*
			byte[] buffer = new byte[contentLength];
			int bytesRead = url.inputStream.read(buffer);
			String page = new String(buffer, 0, bytesRead, "UTF-8");
			*/
			output_stream << url.inputStream
			// FileUtils.copyInputStreamToFile(url_to_remote_asset.toURL().openStream(), dest_file)
		}
	} catch (IOException e) {
		// unable to download asset
		console.println("Unable to download asset: " + e.message)
		return
	}
}

def get_file_content(url_to_file) {
	try {
		// def local_path = new File(config_dir.toString() + File.separator + "asset_hashlist.txt")
		def url = new URL(url_to_file).openConnection()
		file_content = url.inputStream.readLines()
	}  catch (IOException e) {
		// last_modif will stay an empty array
		console.println("Unable to download file list: " + e.message)
		return // stop script if list of hashes not available?? or just download everything found?
	}
	return file_content // arrayList
}

def filter_ruleset_list(list) {
	// quit if no rulesets list found
	if (!list) {
		console.println("No ruleset list found, unable to continue.")
		return
	} else {
		// filtered by container
		return list.findAll { it.contains("RuleSet_Translatability_${container}") } //  || it.contains("RuleSet_Translatability_Generic")}
		// get list of containers through API to check whether $container is in them, if it's not then use Generic
		// only for English; if more source languages need to be used, then filter by source language
	}
}



def build_ruleset(ruleset_list) {
	def ruleset = []
	// def ruleset_without_filename = [:]
	ruleset_list.each { remote_ruleset_name ->

		def url_to_remote_ruleset = domain + "Rules/" + remote_ruleset_name
		remote_ruleset_content = get_file_content(url_to_remote_ruleset).unique()

		def headers = remote_ruleset_content.remove(0).split(/\t/) // tokenize('\t')
		remote_ruleset_content.each { line ->

			def rule = line.split(/\t/) as List // as List is necessary, otherewise it outputs a string[]
			// def rule = parseCsv(it, separator: '\t')

			if (rule[0] != "#") {
				def rule_map = [:]
				headers.eachWithIndex { field, i ->
					rule_map[field] = ( rule[i] ? rule[i] : "" )
					// ruleset.add(rule)
				}

				/* to avoid duplicates coming from different rulesets (container vs generic)
				if ( ruleset_without_filename.containsKey(rule_map.toString()) ) {
					console.println("this rule already has been added")
				} else {
					ruleset_without_filename[rule_map.toString()] = true
				}
				*/
				rule_map["file"] = remote_ruleset_name
				// console.println(rule_map)
				ruleset << rule_map // ruleset.add(Arrays.asList(rule_map));
			}
		}
		console.println("\n")
	}
	return ruleset.unique().sort { it.Source }
}

def create_ta_notes_tmx() {

	def tmx2source_dir = new File(prop.getTMRoot(), "tmx2source")
	tmx2source_dir.mkdirs()
	def project_save = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)
	def ta_notes_tmx = new File(tmx2source_dir, tgt_lang_subtag.toUpperCase() + "_TA.tmx")

}


domain = "https://capps.capstan.be/"
file_list = "file_list.txt"
url_to_file_list = domain + "Rules/" + file_list

ruleset_list = get_file_content(url_to_file_list)
ruleset_list = filter_ruleset_list(ruleset_list)
ruleset = build_ruleset(ruleset_list)

// make a copy of current working TM

def gui(projectRoot){

	project.allEntries.each { segment ->
		editor.gotoEntry(segment.entryNum())
		def source_text = segment.getSrcText();

		ta_note = []
		i = 1
		ruleset.each { rule ->

			def pattern = ~"${rule.Source}"
			assert pattern.class == Pattern
			issue_found = source_text =~ pattern
			assert issue_found instanceof java.util.regex.Matcher
			if ( issue_found ) {
				ta_note.add("#${i}# <" + rule.Target + ">: " + rule.Comments + "// SUGGESTION: " + rule.Suggestion)
				i++
				// @ ask PM what format they want
			}
		}
		editor.replaceEditText(ta_note.join('\n' + '-'*60 + '\n'))
		// add entry to tmx2source ?
	}
	// create_ta_notes_tmx()
}

// commit/register translations to project_save.tmx?
// org.omegat.core.Core.getProject().saveProject(true)
// reload to properly save and load translations // @Briac: not sure this works
org.omegat.gui.main.ProjectUICommands.projectReload()

// console.println("eventType: " + eventType)
// console.println("Collecting garbage...")
System.gc()
// console.println("All garbage collected. Ready for next run.")
return
