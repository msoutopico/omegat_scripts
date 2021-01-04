/* :name = Remove Redundant IDs in TMs :description=
 *
 * @author	Manuel Souto Pico
 * @creation	2020.10.23
 * @last		2020.01.04
 * @version	0.0.4
*/

// path to the folder inside the TM folder
path_to_tmx_dir = "enforce/"

/**
 * @versioning
 * 0.0.3	2020-12-23: Added function `enforced_match_lurking` to avoid removing 
 * 		props from the alternative translation when there's an enforced match 
 * 		with a different translation.
 * 0.0.4	2020-01-04: Added log to `scripts_output` folder indicating how many
 * 		ICE matches have been removed, who ran the script and when.
*/


// import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE;
// if (eventType != PROJECT_CHANGE_TYPE.CLOSE) {
//      return;
// }

// PROBLEMS:
// 1. when prompting to continue, saying No does not work


// code starts here, do not modify
import javax.swing.JOptionPane;
import org.omegat.util.OConsts;
import org.omegat.gui.main.ProjectUICommands;
import java.nio.file.Files;
import java.nio.file.Path
import java.nio.file.Paths
import groovy.util.*
import java.io.File
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.omegat.util.StaticUtils
import java.nio.file.Path
import org.omegat.util.Preferences


import java.text.SimpleDateFormat
def timestamp = new Date()
def readableSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
def filenameSdf = new SimpleDateFormat("yyyyMMdd_HHmmss")
logDate = readableSdf.format(timestamp)

// def confDir = StaticUtils.getConfigDir()
userId = Preferences.getPreferenceDefault(Preferences.TEAM_AUTHOR, System.getProperty("user.name"))
def prop = project.projectProperties
def projectRoot = prop.projectRootDir
// console.println(projectRoot) // path with OS's file separator, without final /
// console.println(prop.projectRoot) // path with /, with final /

def logFileName = "scripts_log.txt"
String scriptOutputPath = projectRoot.toString() + File.separator + 'script_output'
def scriptOutputDir = new File(scriptOutputPath);
scriptOutputDir.mkdirs(); // if file already exists will do nothing

String logFilePath = scriptOutputPath + File.separator + logFileName
console.println(logFilePath)
logFile = new File(logFilePath);
logFile.createNewFile(); // if file already exists will do nothing


def get_all_tu_nodes_from_tm_dir(tmx_files_in_dir) {

	def all_tu_nodes = []

	tmx_files_in_dir.each { tmx_file ->
		//@console.println(tmx_file)
		def tmx_content = read_tmx_file(tmx_file)
		def tu_nodes = tmx_content.body.tu.findAll { it }
		all_tu_nodes += tu_nodes
	}

	return all_tu_nodes
}


def enforced_match_lurking(all_tu_nodes, entry_src_txt, entry_tgt_txt) {

	def matches_found = all_tu_nodes.findAll { node ->
		node.tuv[0].seg.find { s -> s.text() == entry_src_txt }
	}

	def match_found_with_equal_xlat = matches_found.findAll { node ->

		node.tuv[0].seg.find { s -> s.text() == entry_src_txt } &&
		node.tuv[1].seg.find { s -> s.text() == entry_tgt_txt }
	}

	return ( matches_found.isEmpty() || match_found_with_equal_xlat ? false : true )
}


def cluster_segm_pairs_by_alt_vs_default(entries) {

	sources_with_alt = []
	segm_pairs_with_alt = [:]
	sources_with_default = []
	segm_pairs_with_default = [:]

	entries.each {


	    info = project.getTranslationInfo(it)

		// editor.gotoEntry(tu.entryNum())
		String source_text = it.getSrcText() // java.lang.String
		// console.println(source_text + " has an default translation")
		String target_text = info.translation ? info.translation : null;
		// console.println(source_text)

		// @MS_20201223: break all other
		// if (source_text != "None of the classes") { return true }
		// if (source_text != "Climate change and global warming") { return true }
		// if (source_text != "Most students at my school are imaginative.") { return true }



		// console.println("info.defaultTranslation")
		// console.println(info.defaultTranslation)
		//
		// console.println("it.getDuplicate()")
		// console.println(it.getDuplicate())

	    // assert isDup.getClass() == class org.omegat.core.data.SourceTextEntry$DUPLICATE
	    def isDup = it.getDuplicate() // it can be FIRST, NEXT or NONE
	    def isAlt = info.defaultTranslation ? "default" : "alternative" // this is the translation the entry/segment has in the project, there might be multiple or orphan segments in the project_save.tmx that are not used here or nowhere else in the project
	    // console.println("${it.entryNum()} Alt: $isAlt \nDup: $isDup")


		// @MS_20201223: grab unique entries and their translation (alt, or def), then check in project_save whether there's other translations and delete them

		if ( isAlt.toString() == "alternative" ) {

			// create one to many mapping if it doesn't exist
			if (!segm_pairs_with_alt[source_text]) ( segm_pairs_with_alt[source_text] = [] )
			// add translation to the list that has the source text as key
			// to avoid repetitions
			// // if (!segm_pairs_with_alt[source_text].contains(target_text)) segm_pairs_with_alt[source_text].add(target_text)
			// to get real frequency of each
			segm_pairs_with_alt[source_text].add(target_text)

		}

		if ( isAlt.toString() == "default" ) {

			if (!segm_pairs_with_default[source_text]) ( segm_pairs_with_default[source_text] = target_text )
		}
	}

	return [segm_pairs_with_alt, segm_pairs_with_default]
}

// this is in fact checking whether there's another identical segment which has default translation
def has_default_xlat(source_text) {
	return sources_with_default.contains(source_text)
}

def get_files_in_dir(dir, ext_re) {

	dir.traverse(maxDepth: 0) { // removed: `type: FILES,`
		// create object of Path
		Path path = Paths.get(it.path)
		// call getFileName() and get FileName as a string
		String asset_name = path.getFileName()
		def is_tm = asset_name =~ ext_re
		isDirectory = Files.isDirectory(path)
		// assert m instanceof Matcher
		if (is_tm) {
			files_in_dir.add(it)

		} else if(isDirectory) {
			get_files_in_dir(it, ext_re)
		}
	}
	return files_in_dir
}

def read_tmx_file(tmx_file) {

	def xmlParser = new XmlParser();
	// def root = new XmlSlurper(false,false).parseText(source)
	xmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
	xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	xmlParser.setFeature("http://xml.org/sax/features/namespaces", false)
    xmlParser.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
    // ref: https://stackoverflow.com/a/36538456/2095577
    // doc = xmlParser.parseText(tmx_file)
    // return XmlUtil.serialize(doc)
	return xmlParser.parse(tmx_file);
}

def get_list_of_repetitions(entries) {

	def repetitions = []

	entries.each {

	    info = project.getTranslationInfo(it)
	    // assert isDup.getClass() == class org.omegat.core.data.SourceTextEntry$DUPLICATE
	    def isDup = it.getDuplicate() // it can be FIRST, NEXT or NONE
	    def isAlt = info.defaultTranslation ? "default" : "alternative"
	    // console.println("${it.entryNum()} Alt: $isAlt \nDup: $isDup")

		// editor.gotoEntry(tu.entryNum())
		String source_text = it.getSrcText() // java.lang.String
		// console.println(source_text)

		if ( isDup.toString() == "FIRST" ) {
			repetitions.add(source_text)
		}

	}

	return repetitions
}

def get_list_of_default_xlats_reg_in_projsave(projectSave) {

	def project_tmx = read_tmx_file(projectSave)

	default_xlats_in_proj = [:]

	project_tmx.body.tu.findAll { node ->

		def prop_file    = node?.prop.find  {  p -> p.@type    == 'file' }?.text()
		def prop_id      = node?.prop.find  {  p -> p.@type    == 'id'   }?.text()

		if (!prop_file && !prop_id) {
			def source_text = node.tuv[0].seg.text()
			def target_text = node.tuv[1].seg.text()
			default_xlats_in_proj[source_text] = target_text
		}
	}
	return default_xlats_in_proj
}

def get_list_of_default_translations(entries) {  // project.allEntries

	def default_xlats_in_proj = [:]

	entries.each {

	    info = project.getTranslationInfo(it)
	    // assert isDup.getClass() == class org.omegat.core.data.SourceTextEntry$DUPLICATE
	    def isDup = it.getDuplicate() // it can be FIRST, NEXT or NONE
	    def isAlt = info.defaultTranslation ? "default" : "alternative"
	    // console.println("${it.entryNum()} Alt: $isAlt \nDup: $isDup")

		// editor.gotoEntry(tu.entryNum())
		String source_text = it.getSrcText() // java.lang.String
		String target_text = info.translation ? info.translation : null;
		// console.println(source_text)

		if ( isAlt.toString() == "default" ) {
			default_xlats_in_proj[source_text] = target_text
		}

	}

	return default_xlats_in_proj
}

def get_default_xlats_in_tms(tu_nodes) {

	default_xlats_in_tms = [:]

	tu_nodes.findAll { node ->

		def prop_file    = node?.prop.find  {  p -> p.@type    == 'file' }?.text()
		def prop_id      = node?.prop.find  {  p -> p.@type    == 'id'   }?.text()
		// def propMissing = node?.prop.find {  p -> p.@missing == 'xxx'  }?.text()
		// console.println("> " +  prop_file  + "\n> " + prop_id + "\n");
		if (!prop_file && !prop_id) {
			def source_text = node.tuv[0].seg.text()
			def target_text = node.tuv[1].seg.text()
			default_xlats_in_tms[source_text] = target_text
		}
	}
	return default_xlats_in_tms
}

def get_first_and_uniq_in_tms(tu_nodes) {

	// Get all <tu> that have a translation
	def all_sources = []
	def segm_pairs = [:]
	def first_and_uniq_in_tms = [:]

	// get all source texts and create a map where each source
	// text is a key and the list of all its translations is the value
	tu_nodes.findAll { node ->
		def source_text = node.tuv[0].seg.text()
		def target_text = node.tuv[1].seg.text()

		// add source text to list of all source texts
		all_sources.add(source_text)

		// create one to many mapping if it doesn't exist
		if (!segm_pairs[source_text]) ( segm_pairs[source_text] = [] )
		// add translation to the list that has the source text as key
		segm_pairs[source_text].add(target_text)
	}

	// remove repetitions
	def all_sources_set = all_sources.toSet()

	// finds the entries that should not be alternative
	all_sources_set.each { src_txt ->

		def alt_trans_list = [segm_pairs[src_txt]]
		def max_rep = 0
		// get the most frequent translation (to be made the default)
		alt_trans_list.each { it ->
			def freq_tgt = Collections.frequency(segm_pairs[src_txt], it)
			//console.println("Translation '${it}' has been used ${freq_tgt} times")
			if (freq_tgt > max_rep) {
				max_rep = freq_tgt
				first_and_uniq_in_tms[src_txt] = it
			}
		}
	}

	return first_and_uniq_in_tms
}

def remove_props_from_uniq_in_proj(tmx_content, repeated_in_proj_save, first_and_uniq_in_tms) {
	// def tmx_body = tmx_content.body

	tmx_content.body.each { tu ->

		def altsegms_to_default = tu.findAll { it ->

			it.prop.find { p -> p.@type    == 'file' } &&
			it.prop.find { p -> p.@type    == 'id' } &&
			!repeated_in_proj_save.contains(it.tuv[0].seg.text()) // MS: change to !sources_with_alt_in_proj
			// && !multiple_translations(x) in  /tm/enforce/gcm
		}

		altsegms_to_default.each { it ->

			it.remove( it.prop[1] )
			it.remove( it.prop[0] )
		}

	}
	return tmx_content
}

def remove_props_if_default_in_projsave(tmx_content, default_xlats_in_tms) {

	// default_xlats_in_proj = get_list_of_default_translations(project.allEntries)
	default_xlats_in_proj = get_list_of_default_xlats_reg_in_projsave(projectSave)
	//@console.println(default_xlats_in_proj)

	matches_to_debind_counter = 0

	tmx_content.body.each { tu ->

		def matches_to_debind = tu.findAll { it ->

			// has context properties (is alt)
			it.prop.find { p -> p.@type    == 'file' } &&
			it.prop.find { p -> p.@type    == 'id' } &&
			// there isn't already a default xlat for the same source text (whatever the xlat is)
			!default_xlats_in_tms[it.tuv[0].seg.text()] &&
			// there is already a default translation identical to this match (so there's no risk of changing it)
			default_xlats_in_proj[it.tuv[0].seg.text()] == it.tuv[1].seg.text()
		}

		matches_to_debind_counter += matches_to_debind.size()

		matches_to_debind.each { it ->

			//console.println(it)
			it.remove( it.prop[1] )
			it.remove( it.prop[0] )
		}
	}
	return tmx_content
}

def remove_props_from_rep_src_with_same_tgt(tmx_content, first_and_uniq_in_tms) {

	default_xlats_in_proj = get_list_of_default_translations(project.allEntries)

	altsegms_to_default_counter = 0

	tmx_content.body.each { tu ->

		def altsegms_to_default = tu.findAll { it ->

			it.prop.find { p -> p.@type    == 'file' } &&
			it.prop.find { p -> p.@type    == 'id' } &&

			first_and_uniq_in_tms[it.tuv[0].seg.text()] == it.tuv[1].seg.text() &&
			// the source text of the match to be debound is not found in project_save with a different default translation
			(
				default_xlats_in_proj[it.tuv[0].seg.text()]
				&&
				default_xlats_in_proj[it.tuv[0].seg.text()] == it.tuv[1].seg.text()
			)

			// && !multiple_translations(x) in  /tm/enforce/gcm
		}

		console.println(altsegms_to_default)

		altsegms_to_default_counter += altsegms_to_default.size()

		altsegms_to_default.each { it ->

			console.println(it)

			// it.remove( it.prop[1] )
			// it.remove( it.prop[0] )
		}
	}
	return tmx_content
}

def write_tmx_file(tmx_content, tmx_file) {

	tmx_file.withWriter("UTF-8") { w ->
		new XmlNodePrinter( new PrintWriter( w ) ).with { p ->
			preserveWhitespace = true;
			p.print( tmx_content );
		}
	}
}


def gui() {
	final def title = 'Remove redundant IDs in TMs'

	def props = project.projectProperties
	if (!props) {
		final def msg   = 'No project opened.'
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
		return
	}

	// prop = project.getProjectProperties()
	proj_name = props.projectName

	// def root = prop.getProjectRoot()
	omegat_dir = props.getProjectInternal()
	tmdir_fs = props.getTMRoot() // fs = forward slash

	File tmx_dir = new File(tmdir_fs + path_to_tmx_dir)
	def ext_re = ~/tmx$/

	if (!tmx_dir.exists()) {
		console.println("The folder /tm/${path_to_tmx_dir} does not exist")
		return
	}

	files_in_dir = [] // must be init outside of the function, because the func is recursive
	def tmx_files_in_dir = get_files_in_dir(tmx_dir, ext_re)

	def all_tu_nodes = get_all_tu_nodes_from_tm_dir(tmx_files_in_dir)





	// Keep track of the directories and files we need before closing the project.
	projectRoot = props.projectRootDir;
	projectSave = new File(props.projectInternal, OConsts.STATUS_EXTENSION)
	timestamp = new Date().format("yyyyMMddHHmmss", TimeZone.getTimeZone('UTC'));
	backupFile = new File(props.projectInternal, OConsts.STATUS_EXTENSION + "." + timestamp + ".alt.bak");

	int choice = JOptionPane.showOptionDialog(null, "Redundant ICE matches will be removed. Continue?",
		title,
		JOptionPane.YES_NO_OPTION,
		JOptionPane.QUESTION_MESSAGE,
		null, null, null);

	if (choice == JOptionPane.YES_OPTION) {
		console.print("Closing project...\n")

		// to make sure enforced translations are saved to project_save.tmx
		/*
		org.omegat.core.Core.getProject().saveProject(true);
		ProjectUICommands.projectClose()
		ProjectUICommands.projectOpen(projectRoot, true)
		*/
		// ProjectUICommands.projectReload()


		// Closing project, to be safe.
		ProjectUICommands.projectClose();
	}


	// Make a backup of the original file, just in case.
	// console.println("Backing up file to '${backupFile}'");
	console.println("Backing up file to " + backupFile.getName() + "\n");
	Files.copy(projectSave.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)

	// Setup the XML parser and load the project_save.tmx
	def xmlParser = new XmlParser();
	xmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
	xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	def project_tmx = xmlParser.parse(projectSave);
	def tmx_body = project_tmx.body;

	// Get all <tu> that have a translation

	def all_sources = []
	def segm_pairs = [:]
	def segm_pairs_to_remove_props = [:]

	def sorted_entries = cluster_segm_pairs_by_alt_vs_default(project.allEntries)

	// sources_with_alt = sorted_entries[0]
	segm_pairs_with_alt = sorted_entries[0] // map - string to list of strings
	// sources_with_default = sorted_entries[2]
	segm_pairs_with_default = sorted_entries[1] // map - string to string

	sources_with_alt = 		segm_pairs_with_alt.keySet() 		as String[]
	sources_with_default = 	segm_pairs_with_default.keySet() 	as String[]

	// @MS_20201223
	// console.println("sources_with_alt:")
	// console.println(Arrays.toString(sources_with_alt))
	// console.println("sources_with_default:")
	// console.println(Arrays.toString(sources_with_default))

	segm_pairs_without_default = [:]
	segm_pairs_to_remove_props = [:]

	// decision: decide which translations should be made default and which should be deleted
	segm_pairs_with_alt.each { source_text, list_of_alt_xlats -> // key, value
		// console.println(source_text)
		if (has_default_xlat(source_text)) {
			// console.println("++++ There's an identical segment in the project with a defaul xlat")

			list_of_alt_xlats.each { alt_xlat ->

				if (segm_pairs_with_default[source_text] == alt_xlat) {
					// since the alt translation is identical to the default, it can be dumped (=rm props)
					if (!segm_pairs_to_remove_props[source_text]) segm_pairs_to_remove_props[source_text] = []
					segm_pairs_to_remove_props[source_text].add(alt_xlat)
				}
			}

		} else {
			// console.println("---- NO identical segment in the project with a defaul xlat")
			// collect to find the most frequent one and make it default
			// segm_pairs_without_default.put(source_text, list_of_alt_xlats)
			segm_pairs_without_default[source_text] = list_of_alt_xlats
		}
		// if (sources_with_default(source_text)) console.println("Remove context props of " + source_text)
	}


	segms_pairs_with_alt_xlats_to_default = [:]
	// finds the entries that should not be alternative
	segm_pairs_without_default.each { src_txt, list_of_alt_xlats ->

		def alt_trans_set = segm_pairs_without_default[src_txt].toSet()
		def max_rep = 0
		// get the most frequent translation (to be made the default)
		alt_trans_set.each { it ->

			// check whether there's an enforced match with the same translation
			// if there is, the alt xlat can be removed / else it's bypassing the enforcement and should be kept
			def enforced_will_overwrite = enforced_match_lurking(all_tu_nodes, src_txt, alt_trans = it)

			if (!enforced_will_overwrite) {
				def freq_tgt = Collections.frequency(segm_pairs_without_default[src_txt], it)
				//console.println("Translation '${it}' has been used ${freq_tgt} times")
				if (freq_tgt > max_rep) {
					max_rep = freq_tgt
					segms_pairs_with_alt_xlats_to_default[src_txt] = it
				}
			}
		}
	}


	segms_pairs_with_alt_xlats_to_default.each {
		//@console.println("The following xlat will be made default: " + it)
	}


	def props_to_remove_counter = 0
	segms_pairs_with_alt_xlats_to_default.each { src, tgt ->

		// @MS20201223:
		// console.println(src + " => " + tgt)

		// get all prop nodes that have tuv parents stored in segm_pairs_to_remove_props
		def props_to_remove = project_tmx.body.tu.prop.findAll {
			it.parent().tuv[0].seg.find { s -> s.text() == src } &&
			it.parent().tuv[1].seg.find { s -> s.text() == tgt }
		}

		props_to_remove_counter += props_to_remove.size()

		props_to_remove.each { it ->
			def parent = it.parent()
			parent.remove(it)
			// @MS_20201223:
			// console.println("removing " + it)
		}

		/*
		altsegms_to_default.each { it ->
			it.remove( it.prop[1] )
			it.remove( it.prop[0] )
		}*/

	}


	def nodes_to_default_counter = 0
	segm_pairs_to_remove_props.each { src, list_of_alt_xlats -> // "src, tgt" are "key, value"


		//@console.println("The following translations of '" + src + "' will be deleted")
		//@list_of_alt_xlats.each { tgt -> console.println(tgt) }

		list_of_alt_xlats.each { tgt ->

			def tus_tu_remove = project_tmx.body.tu.findAll { tu ->
				(tu.tuv[0].seg.text() == src) &&
				(tu.tuv[1].seg.text() == tgt) &&
				(tu.prop.count{ p -> p.@type == 'id' } > 0)
			}

			nodes_to_default_counter += tus_tu_remove.size()
			// Removes the found <tu> from the XML document
			tus_tu_remove.each { tu ->
				// def f = tu.parent().remove(tu);
			}
		}
	}

	// https://stackoverflow.com/a/22221811 to avoid encoding issues
	// The resulting XML file has no DOCTYPE, but it doesn't prevent it
	// to being loaded correctly.
	projectSave.withWriter("UTF-8") { w ->
		new XmlNodePrinter( new PrintWriter( w ) ).with { p ->
			preserveWhitespace = true;
			p.print( project_tmx );
		}
	}


// --------------------------------------------------------------------------
// put all the above in a function called removed_redundant_ids_in_project_save



















	def sorted_debound_entries = cluster_segm_pairs_by_alt_vs_default(project.allEntries)

	debound_s2_alt_in_proj_save = sorted_debound_entries[0] // map - string to list of strings
	debound_s2_default_in_proj_save = sorted_debound_entries[1] // map - string to string
	// s2 = segment pair


	// get list of repeated segments in the project
	def repeated_in_proj_save = get_list_of_repetitions(project.allEntries)




	def first_and_uniq_in_tms = get_first_and_uniq_in_tms(all_tu_nodes)
	def default_xlats_in_tms = get_default_xlats_in_tms(all_tu_nodes)


	tmx_files_in_dir.each { tmx_file ->

		def tmx_content = read_tmx_file(tmx_file)
		//clean_tmx_content = remove_props_from_uniq_in_proj(tmx_content, repeated_in_proj_save, first_and_uniq_in_tms)
		// clean_tmx_content = remove_props_from_rep_src_with_same_tgt(tmx_content, first_and_uniq_in_tms)
		clean_tmx_content = remove_props_if_default_in_projsave(tmx_content, default_xlats_in_tms)
		write_tmx_file(clean_tmx_content, tmx_file)

	}

	// log 
	logFile.text +="[${logDate}] Script 'Remove Redundant IDs in TMs' [remove_redundant_ids_in_tm.groovy] run by ${userId}.\n"
	def countMsg = "Context properties removed from ${props_to_remove_counter} entries."
	logFile.text += "[${logDate}] " + countMsg + "\n"
	// Phew, all is good. Reopen the project.
	JOptionPane.showMessageDialog(null, countMsg, title, JOptionPane.INFORMATION_MESSAGE);
	ProjectUICommands.projectOpen(projectRoot, true);

}


System.gc()
