/* :name =TA - Create TA Notes TM :description =
 *
 *  @version: 1.2.0
 *  @author: Manuel Souto Pico
 *  @date: 2020.08.30
 *  @properties: https://cat.capstan.be/OmegaT/project-assets.json
 */

// user options
delete_working_tm = true



















































/*
 * @changes: 1.2.0: added notes do tmx2source
*/

// first check: is the project being opened?
import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE
title = "TA notes"
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
console.println("="*40 + "\n" + " "*5 + "TA Notes\n" + "="*40)

// constants
def prop = project.projectProperties
tgt_code = 		prop.targetLanguage
src_code = 		prop.sourceLanguage
tgt_lang_subtag = 	tgt_code.languageCode
// def src_lang_subtag = 	project.projectProperties.sourceLanguage.languageCode
// def tgt_region_subtag = project.projectProperties.targetLanguage.countryCode
// def src_region_subtag = project.projectProperties.sourceLanguage.countryCode

// Keep track of the directories and files we need before closing the project.
def projectRoot = prop.projectRootDir;
def projectSave = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)
def timestamp = new Date().format("yyyyMMddHHmmss", TimeZone.getTimeZone('UTC'));
def backupFile = new File(prop.projectInternal, OConsts.STATUS_EXTENSION + timestamp + ".ta.bak");


def tmx2source_dir = new File(prop.getTMRoot(), "tmx2source")
tmx2source_dir.mkdir()
def ta_notes_tmx = new File(tmx2source_dir, tgt_lang_subtag.toUpperCase() + "_TA.tmx")
	



def old_stuff_func() {
	
	console.print("Closing project...\n")
	// Closing project, to be safe.
	ProjectUICommands.projectClose()

	console.println(prop.projectInternal)
	
	def project_save = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)
	
	def tmx2source_dir = new File(prop.getTMRoot(), "tmx2source")
	tmx2source_dir.mkdirs()
	def ta_notes_tmx = new File(tmx2source_dir, tgt_lang_subtag.toUpperCase() + "_TA.tmx")
	
	// Files.copy(project_save.toPath(), ta_notes_tmx.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
	// Files.move(project_save.toPath(), ta_notes_tmx.toPath(), REPLACE_EXISTING)
	console.println("Line 120")
	// ta_notes_tmx.delete()
	
	if ( ta_notes_tmx.exists() ){
	   ta_notes_tmx.delete()
	}
	project_save.renameTo(ta_notes_tmx)
	// Files.copy(project_save.toPath(), ta_notes_tmx.toPath())
	console.println("Line 130")
	
	// sleep(5000)
	while ( project_save.exists() ) {
		project_save.delete()	
	}

	console.println("Line 140")
}

	
if (!prop) {
	final def msg   = 'No project opened.'
	JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
	return
}


int choice = JOptionPane.showOptionDialog(null, "The current project will be closed. Continue?",
	title,
	JOptionPane.YES_NO_OPTION,
	JOptionPane.QUESTION_MESSAGE,
	null, null, null);

if (choice == JOptionPane.YES_OPTION)
{
	console.print("\nClosing project...");
	// Closing project, to be safe.
	ProjectUICommands.projectClose();
}

// Make a backup of the original file, just in case.
console.println("\nBacking up file to '${backupFile}'");
Files.copy(projectSave.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)

// move to tmx2source
// Files.copy(projectSave.toPath(), ta_notes_tmx.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)

def write_tmx_file(xml_content, xml_file) {	
	// https://stackoverflow.com/a/22221811 to avoid encoding issues
	// The resulting XML file has no DOCTYPE, but it doesn't prevent it
	// to being loaded correctly.
	
	xml_file.withWriter("UTF-8") { w ->
		new XmlNodePrinter( new PrintWriter( w ) ).with { p ->
			preserveWhitespace = true;
			p.print( xml_content );
		}
	}
}

try {
	// Setup the XML parser and load the project_save.tmx
	def xmlParser = new XmlParser();
	xmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
	xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	def projectTmx = xmlParser.parse(projectSave);
	def tmxBody = projectTmx.body;

	def tusToRemove = [];
	def nodes_with_ta_notes = [];
	projectTmx.body.tu.findAll { tu ->
	
		// mark entry to remove
		tusToRemove.add(tu);
	
		// add entry to the TA notes 
		def target_text = tu.tuv[1].seg.text()
		def ta_note = target_text =~ /^COMMENT#*: /
		if (ta_note) {
			nodes_with_ta_notes.add(tu);
		}
	}

	// Removes all the found <tu> from the XML document
	tusToRemove.each { tu ->
		// remove every node from main working TM
		def f = tu.parent().remove(tu);
	}

	// flush working TM
	write_tmx_file(projectTmx, projectSave)
	
	// add the TA notes found to the TA notes list (to be written to the TA notes TM)
	if ( ta_notes_tmx.exists() )  ta_notes_tmx.delete() 
	def newBody = new Node(projectTmx, 'body', nodes_with_ta_notes)

	// (re)create TA notes TM
	write_tmx_file(projectTmx, ta_notes_tmx)
}
catch(IOException e) {
	console.println(e)
}

// sleep(5000)
// projectSave.delete()
	
// Phew, all is good. Reopen the project.
console.print("Reopening project...\n")
JOptionPane.showMessageDialog(null, "TA notes file created.\nThe project will be re-loaded now.", title, JOptionPane.INFORMATION_MESSAGE);
ProjectUICommands.projectOpen(projectRoot, true)

// console.println("eventType: " + eventType)
// console.println("Collecting garbage...")
System.gc()
// console.println("All garbage collected. Ready for next run.")
return